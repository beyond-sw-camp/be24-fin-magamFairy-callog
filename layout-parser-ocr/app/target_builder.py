from __future__ import annotations

from typing import Dict, Iterable, List, Sequence, Set

from app.schemas import BlockResult, DownstreamTarget, PageResult


TEXT_TARGET_TYPES = {"text", "title", "list"}


def build_downstream_targets(
    page_results: Sequence[PageResult],
    *,
    include_image_targets: bool = True,
) -> Dict[str, List[DownstreamTarget]]:
    """Build minimal server-facing work queues from full parser blocks."""

    merged_child_ids = _merged_child_ids(page_results)
    ocr_targets = [
        target
        for page in page_results
        for target in _ocr_targets_for_page(page, merged_child_ids)
    ]
    table_targets = [
        target
        for page in page_results
        for target in _table_targets_for_page(page)
    ]
    ocr_targets = _filter_ocr_targets_inside_tables(page_results, ocr_targets, table_targets)
    raw_image_targets = [
        target
        for page in page_results
        for target in _image_targets_for_page(page)
    ]
    image_filter_targets = _filter_context_image_targets(
        page_results,
        raw_image_targets,
        ocr_targets + table_targets,
    )
    ocr_targets = _filter_ocr_targets_inside_images(page_results, ocr_targets, image_filter_targets)
    image_targets = _filter_context_image_targets(
        page_results,
        raw_image_targets,
        ocr_targets + table_targets,
    ) if include_image_targets else []

    return {
        "ocr_targets": _with_ids(
            sorted(ocr_targets, key=lambda item: (item.page, item.reading_order)),
            "ocr",
        ),
        "image_targets": _with_ids(
            sorted(image_targets, key=lambda item: (item.page, item.reading_order)),
            "image",
        ),
        "table_targets": _with_ids(
            sorted(table_targets, key=lambda item: (item.page, item.reading_order)),
            "table",
        ),
    }


def attach_clean_blocks(
    page_results: Sequence[PageResult],
    downstream_targets: Dict[str, List[DownstreamTarget]],
) -> None:
    """Attach non-overlapping presentation/contract blocks to each page."""

    targets_by_page: Dict[int, Dict[str, List[DownstreamTarget]]] = {}
    for kind in ("ocr_targets", "table_targets", "image_targets"):
        for target in downstream_targets.get(kind, []):
            targets_by_page.setdefault(target.page, {}).setdefault(kind, []).append(target)

    for page in page_results:
        block_by_id = {block.id: block for block in page.blocks}
        page_targets = targets_by_page.get(page.page, {})
        target_by_block_id = {
            target.block_id: target
            for target_group in page_targets.values()
            for target in target_group
        }
        selected: List[BlockResult] = []
        selected_ids: Set[str] = set()

        for target in page_targets.get("ocr_targets", []) + page_targets.get("table_targets", []):
            block = block_by_id.get(target.block_id)
            if block is None or block.id in selected_ids:
                continue
            selected.append(block)
            selected_ids.add(block.id)

        for target in page_targets.get("image_targets", []):
            block = block_by_id.get(target.block_id)
            if block is None or block.id in selected_ids:
                continue
            if _conflicts_with_selected(block, selected):
                continue
            selected.append(block)
            selected_ids.add(block.id)

        page.clean_blocks = sorted(selected, key=lambda block: block.reading_order)
        page.layout_regions = _layout_regions_for_page(page, page.clean_blocks, target_by_block_id)
        page.metadata["clean_block_counts"] = _source_counts(page.clean_blocks)
        page.metadata["clean_block_strategy"] = (
            "ocr_targets + table_targets + non-overlapping image_targets"
            if page_targets.get("image_targets")
            else "ocr_targets + table_targets"
        )
        page.metadata["layout_summary"] = _layout_summary(page, page.layout_regions)

    clean_block_ids = {
        block.id
        for page in page_results
        for block in page.clean_blocks
    }
    for kind in ("ocr_targets", "table_targets", "image_targets"):
        downstream_targets[kind][:] = [
            target
            for target in downstream_targets.get(kind, [])
            if target.block_id in clean_block_ids
        ]


def _ocr_targets_for_page(page: PageResult, merged_child_ids: Set[str]) -> List[DownstreamTarget]:
    candidates: List[DownstreamTarget] = []
    for block in page.blocks:
        if block.id in merged_child_ids:
            continue

        metadata = block.metadata
        source = str(metadata.get("source", ""))
        ocr = metadata.get("ocr", {})
        block_type = block.type.strip().lower()

        if source == "visual_text_merged":
            candidates.append(
                _target(
                    page=page,
                    block=block,
                    target_type="ocr",
                    route="ocr_server",
                    priority=int(ocr.get("priority", 100)),
                    child_block_ids=[str(child_id) for child_id in metadata.get("children", [])],
                    metadata={
                        "mode": ocr.get("mode", "merged_text_ocr"),
                        "reason": ocr.get("reason"),
                        "fallback_sources": metadata.get("children", []),
                    },
                )
            )
            continue

        if (
            block.analysis_hint == "ocr"
            and ocr.get("recommended") is True
            and block_type in TEXT_TARGET_TYPES
            and source != "pdf_native"
        ):
            candidates.append(
                _target(
                    page=page,
                    block=block,
                    target_type="ocr",
                    route="ocr_server",
                    priority=int(ocr.get("priority", 80)),
                    metadata={
                        "mode": ocr.get("mode", "text_ocr"),
                        "reason": ocr.get("reason"),
                    },
                )
            )
    return _dedupe_ocr_targets(candidates)


def _table_targets_for_page(page: PageResult) -> List[DownstreamTarget]:
    targets: List[DownstreamTarget] = []
    for block in page.blocks:
        if block.analysis_hint != "table" and block.type.strip().lower() != "table":
            continue

        ocr = block.metadata.get("ocr", {})
        targets.append(
            _target(
                page=page,
                block=block,
                target_type="table",
                route="table_extraction_server",
                priority=int(ocr.get("priority", 70)),
                metadata={
                    "mode": ocr.get("mode", "table_ocr"),
                    "reason": ocr.get("reason"),
                },
            )
        )
    return targets


def _image_targets_for_page(page: PageResult) -> List[DownstreamTarget]:
    targets: List[DownstreamTarget] = []
    for block in page.blocks:
        if block.analysis_hint != "figure" and block.type.strip().lower() != "figure":
            continue

        area_ratio = float(block.metadata.get("area_ratio", 0.0) or 0.0)
        source = str(block.metadata.get("source", ""))
        if source == "pdf_native" and area_ratio < 0.003:
            continue

        targets.append(
            _target(
                page=page,
                block=block,
                target_type="image",
                route="image_analysis_server",
                priority=_image_priority(source, area_ratio),
                metadata={
                    "mode": "image_analysis",
                    "reason": "Figure region routed to image understanding service.",
                    "area_ratio": area_ratio,
                },
            )
        )
    return targets


def _layout_regions_for_page(
    page: PageResult,
    blocks: Sequence[BlockResult],
    target_by_block_id: Dict[str, DownstreamTarget],
) -> List[Dict]:
    regions: List[Dict] = []
    for index, block in enumerate(blocks, start=1):
        target = target_by_block_id.get(block.id)
        route = target.route if target is not None else _default_route(block)
        target_type = target.target_type if target is not None else _region_type(block)
        regions.append(
            {
                "id": f"p{page.page}-r{index}",
                "page": page.page,
                "block_id": block.id,
                "region_type": _region_type(block),
                "target_type": target_type,
                "route": route,
                "label": block.type,
                "analysis_hint": block.analysis_hint,
                "source": str(block.metadata.get("source", "unknown")),
                "reading_order": block.reading_order,
                "bbox": block.bbox,
                "bbox_normalized": list(block.metadata.get("bbox_normalized", [])),
                "area_ratio": float(block.metadata.get("area_ratio", 0.0) or 0.0),
                "score": block.score,
                "crop_path": block.crop_path,
                "crop_url": block.crop_url,
                "text": block.text,
                "child_block_ids": list(target.child_block_ids if target is not None else []),
                "layout_role": _layout_role(block),
            }
        )
    return regions


def _layout_summary(page: PageResult, regions: Sequence[Dict]) -> Dict:
    counts: Dict[str, int] = {}
    route_counts: Dict[str, int] = {}
    covered_area = 0.0
    for region in regions:
        region_type = str(region.get("region_type", "unknown"))
        route = str(region.get("route", "none"))
        counts[region_type] = counts.get(region_type, 0) + 1
        route_counts[route] = route_counts.get(route, 0) + 1
        covered_area += _area(region.get("bbox", []))

    page_area = float(page.width * page.height)
    return {
        "region_counts": counts,
        "route_counts": route_counts,
        "region_count": len(regions),
        "clean_block_count": len(page.clean_blocks),
        "coverage_ratio_sum": round(covered_area / page_area, 6) if page_area > 0 else 0.0,
        "strategy": "deduplicated reading-order layout regions for OCR/image/table routing",
    }


def _region_type(block: BlockResult) -> str:
    block_type = block.type.strip().lower()
    if block.analysis_hint == "table" or block_type == "table":
        return "table_region"
    if block.analysis_hint == "figure" or block_type == "figure":
        return "image_region"
    if block.analysis_hint in {"ocr", "embedded_text"} or block_type in TEXT_TARGET_TYPES:
        return "text_region"
    return "layout_region"


def _layout_role(block: BlockResult) -> str:
    source = str(block.metadata.get("source", "unknown"))
    if source == "layout_parser":
        return "semantic_layout_detector"
    if source == "visual_text_merged":
        return "merged_ocr_region"
    if source == "visual_text":
        return "visual_ocr_region"
    if source == "pdf_native":
        return "pdf_native_region"
    return "layout_region"


def _default_route(block: BlockResult) -> str:
    region_type = _region_type(block)
    if region_type == "table_region":
        return "table_extraction_server"
    if region_type == "image_region":
        return "image_analysis_server"
    if region_type == "text_region":
        return "ocr_server"
    return "none"


def _filter_context_image_targets(
    page_results: Sequence[PageResult],
    image_targets: Sequence[DownstreamTarget],
    protected_targets: Sequence[DownstreamTarget],
) -> List[DownstreamTarget]:
    block_by_page_id = {
        (page.page, block.id): block
        for page in page_results
        for block in page.blocks
    }
    protected_blocks_by_page: Dict[int, List[BlockResult]] = {}
    for target in protected_targets:
        block = block_by_page_id.get((target.page, target.block_id))
        if block is not None:
            protected_blocks_by_page.setdefault(target.page, []).append(block)

    filtered: List[DownstreamTarget] = []
    for target in image_targets:
        block = block_by_page_id.get((target.page, target.block_id))
        if block is None:
            continue
        if _conflicts_with_selected(block, protected_blocks_by_page.get(target.page, [])):
            continue
        filtered.append(target)
    return filtered


def _filter_ocr_targets_inside_tables(
    page_results: Sequence[PageResult],
    ocr_targets: Sequence[DownstreamTarget],
    table_targets: Sequence[DownstreamTarget],
) -> List[DownstreamTarget]:
    block_by_page_id = {
        (page.page, block.id): block
        for page in page_results
        for block in page.blocks
    }
    table_blocks_by_page: Dict[int, List[BlockResult]] = {}
    for target in table_targets:
        block = block_by_page_id.get((target.page, target.block_id))
        if block is not None:
            table_blocks_by_page.setdefault(target.page, []).append(block)

    filtered: List[DownstreamTarget] = []
    for target in ocr_targets:
        target_area = _area(target.bbox)
        if target_area <= 0:
            continue

        inside_table = False
        for table_block in table_blocks_by_page.get(target.page, []):
            overlap = _intersection_area(target.bbox, table_block.bbox)
            if overlap / target_area >= 0.80:
                inside_table = True
                break
        if inside_table:
            continue
        filtered.append(target)
    return filtered


def _filter_ocr_targets_inside_images(
    page_results: Sequence[PageResult],
    ocr_targets: Sequence[DownstreamTarget],
    image_targets: Sequence[DownstreamTarget],
) -> List[DownstreamTarget]:
    block_by_page_id = {
        (page.page, block.id): block
        for page in page_results
        for block in page.blocks
    }
    image_blocks_by_page: Dict[int, List[BlockResult]] = {}
    for target in image_targets:
        block = block_by_page_id.get((target.page, target.block_id))
        if block is not None:
            image_blocks_by_page.setdefault(target.page, []).append(block)

    filtered: List[DownstreamTarget] = []
    for target in ocr_targets:
        target_area = _area(target.bbox)
        if target_area <= 0:
            continue

        covered_by_image = False
        for image_block in image_blocks_by_page.get(target.page, []):
            image_area = _area(image_block.bbox)
            if image_area <= 0:
                continue
            overlap = _intersection_area(target.bbox, image_block.bbox)
            if overlap / target_area >= 0.82 and target_area / image_area <= 0.12:
                covered_by_image = True
                break

        if not covered_by_image:
            filtered.append(target)

    return filtered


def _dedupe_ocr_targets(targets: Sequence[DownstreamTarget]) -> List[DownstreamTarget]:
    selected: List[DownstreamTarget] = []
    for target in sorted(
        targets,
        key=lambda item: (-_target_selection_priority(item), item.reading_order, _area(item.bbox)),
    ):
        if any(_is_duplicate_ocr_region(target, existing) for existing in selected):
            continue
        selected.append(target)
    return sorted(selected, key=lambda item: item.reading_order)


def _target_selection_priority(target: DownstreamTarget) -> int:
    source = str(target.source)
    block_type = target.type.strip().lower()
    if source == "layout_parser" and block_type == "title":
        return target.priority + 18
    if source == "layout_parser" and block_type in TEXT_TARGET_TYPES:
        return target.priority + 15
    return target.priority


def _is_duplicate_ocr_region(candidate: DownstreamTarget, existing: DownstreamTarget) -> bool:
    candidate_area = _area(candidate.bbox)
    existing_area = _area(existing.bbox)
    if candidate_area <= 0 or existing_area <= 0:
        return True

    overlap = _intersection_area(candidate.bbox, existing.bbox)
    if overlap <= 0:
        return False

    if overlap / min(candidate_area, existing_area) >= 0.55:
        return True

    candidate_covered = overlap / candidate_area
    existing_covered = overlap / existing_area
    visual_existing = str(existing.source).startswith("visual_text")
    visual_candidate = str(candidate.source).startswith("visual_text")
    layout_candidate = candidate.source == "layout_parser"
    layout_existing = existing.source == "layout_parser"
    if layout_candidate and visual_existing and candidate_covered >= 0.30:
        return True
    if visual_candidate and layout_existing and candidate_covered >= 0.25:
        return True

    return candidate_covered >= 0.70 or existing_covered >= 0.85


def _target(
    *,
    page: PageResult,
    block: BlockResult,
    target_type: str,
    route: str,
    priority: int,
    child_block_ids: List[str] = None,
    related_block_ids: List[str] = None,
    metadata: Dict = None,
) -> DownstreamTarget:
    source = str(block.metadata.get("source", "unknown"))
    return DownstreamTarget(
        id="pending",
        target_type=target_type,
        route=route,
        block_id=block.id,
        page=block.page,
        type=block.type,
        source=source,
        priority=priority,
        reading_order=block.reading_order,
        bbox=block.bbox,
        bbox_normalized=list(block.metadata.get("bbox_normalized", [])),
        crop_path=block.crop_path,
        crop_url=block.crop_url,
        page_image_path=page.image_path,
        page_image_url=page.image_url,
        text=block.text,
        child_block_ids=child_block_ids or [],
        related_block_ids=related_block_ids or [],
        metadata=metadata or {},
    )


def _with_ids(targets: List[DownstreamTarget], prefix: str) -> List[DownstreamTarget]:
    for index, target in enumerate(targets, start=1):
        target.id = f"{prefix}-{index:04d}"
    return targets


def _merged_child_ids(page_results: Sequence[PageResult]) -> Set[str]:
    child_ids: Set[str] = set()
    for page in page_results:
        for block in page.blocks:
            if block.metadata.get("source") != "visual_text_merged":
                continue
            child_ids.update(str(child_id) for child_id in block.metadata.get("children", []))
    return child_ids


def _image_priority(source: str, area_ratio: float) -> int:
    if source == "visual_graphic" and area_ratio >= 0.05:
        return 78
    if source == "visual_graphic":
        return 68
    if source == "layout_parser" and area_ratio >= 0.20:
        return 90
    if source == "layout_parser":
        return 80
    if area_ratio >= 0.05:
        return 70
    return 55


def _conflicts_with_selected(block: BlockResult, selected: Sequence[BlockResult]) -> bool:
    block_area = _area(block.bbox)
    if block_area <= 0:
        return True

    overlapping_count = 0
    selected_overlap_area = 0.0
    for existing in selected:
        overlap = _intersection_area(block.bbox, existing.bbox)
        if overlap <= 0:
            continue
        selected_area = _area(existing.bbox)
        if selected_area <= 0:
            continue
        if overlap / min(block_area, selected_area) >= 0.20:
            overlapping_count += 1
            selected_overlap_area += overlap

    if overlapping_count >= 2:
        if block.metadata.get("source") == "visual_graphic" and selected_overlap_area / block_area < 0.35:
            return False
        return True
    if block.metadata.get("source") == "visual_graphic":
        return selected_overlap_area / block_area >= 0.35
    return selected_overlap_area / block_area >= 0.15


def _source_counts(blocks: Iterable[BlockResult]) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    for block in blocks:
        source = str(block.metadata.get("source", "unknown"))
        counts[source] = counts.get(source, 0) + 1
    return counts


def _area(box: Sequence[float]) -> float:
    return max(0.0, float(box[2]) - float(box[0])) * max(
        0.0,
        float(box[3]) - float(box[1]),
    )


def _intersection_area(a: Sequence[float], b: Sequence[float]) -> float:
    x1 = max(float(a[0]), float(b[0]))
    y1 = max(float(a[1]), float(b[1]))
    x2 = min(float(a[2]), float(b[2]))
    y2 = min(float(a[3]), float(b[3]))
    return max(0.0, x2 - x1) * max(0.0, y2 - y1)
