from __future__ import annotations

import re
from math import sqrt
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple

from fastapi.encoders import jsonable_encoder
from PIL import Image

from app.block_postprocess import (
    analysis_hint_for_block,
    clamp_bbox,
    prepare_blocks_for_ocr,
)
from app.document import PageImage, convert_to_page_images
from app.layout_detector import detector
from app.pdf_native import extract_pdf_native_blocks
from app.schemas import BlockResult, DocumentResult, DownstreamTarget, PageResult
from app.settings import settings
from app.storage import asset_url, result_job_dir, write_job_json, write_result_json
from app.target_builder import attach_clean_blocks, build_downstream_targets
from app.visual_graphic import extract_visual_graphic_blocks
from app.visual_text import (
    extract_visual_text_blocks,
    merge_visual_text_blocks,
    refine_layout_blocks_with_visual_text,
)


TEXT_CANVAS_SOURCE = "page_text_canvas"
OCR_CANVAS_ROUTE = "ocr_text_canvas"


def process_document(
    *,
    job_id: str,
    document_id: str,
    source_path: Path,
    request_base_url: str,
    include_image_targets: Optional[bool] = None,
) -> DocumentResult:
    created_at = datetime.now(timezone.utc)
    job_dir = result_job_dir(job_id)
    pages_dir = job_dir / "pages"
    crops_dir = job_dir / "debug_crops"

    page_images = convert_to_page_images(source_path, pages_dir)
    native_blocks_by_page = extract_pdf_native_blocks(source_path, page_images)
    page_results = [
        _process_page(
            job_id=job_id,
            source_path=source_path,
            page=page,
            native_blocks=native_blocks_by_page.get(page.page_number, []),
            crops_dir=crops_dir,
            request_base_url=request_base_url,
        )
        for page in page_images
    ]
    downstream_targets = build_downstream_targets(
        page_results,
        include_image_targets=settings.include_image_targets
        if include_image_targets is None
        else include_image_targets,
    )
    attach_clean_blocks(page_results, downstream_targets)
    downstream_targets["ocr_targets"] = _build_ocr_text_canvas_targets(
        page_results,
        downstream_targets["ocr_targets"],
    )
    _mark_ocr_regions_for_text_canvas(page_results, downstream_targets["ocr_targets"])
    _refresh_layout_summaries(page_results)
    _attach_clean_crops(
        page_results=page_results,
        downstream_targets=[
            *downstream_targets["ocr_targets"],
            *downstream_targets["image_targets"],
            *downstream_targets["table_targets"],
        ],
        clean_crops_dir=job_dir / "clean_crops",
        request_base_url=request_base_url,
    )

    completed_at = datetime.now(timezone.utc)
    layout_result_relative_path = f"{job_id}/layout_result.json"
    result = DocumentResult(
        job_id=job_id,
        document_id=document_id,
        status="completed",
        pages=page_results,
        ocr_targets=downstream_targets["ocr_targets"],
        image_targets=downstream_targets["image_targets"],
        table_targets=downstream_targets["table_targets"],
        layout_result_path=layout_result_relative_path,
        layout_result_url=asset_url(request_base_url, layout_result_relative_path),
        created_at=created_at,
        completed_at=completed_at,
    )
    result_payload = jsonable_encoder(result)
    write_result_json(job_id, result_payload)
    write_job_json(job_id, "layout_result.json", _compact_layout_result(result_payload))
    return result


def _process_page(
    *,
    job_id: str,
    source_path: Path,
    page: PageImage,
    native_blocks: List[Dict],
    crops_dir: Path,
    request_base_url: str,
) -> PageResult:
    detected_blocks = detector.detect(page.path)
    for block in detected_blocks:
        metadata = block.setdefault("metadata", {})
        metadata.setdefault("source", "layout_parser")
        metadata.setdefault("role", "semantic_region")

    visual_text_blocks = extract_visual_text_blocks(
        source_path=source_path,
        page=page,
        has_pdf_native_blocks=bool(native_blocks),
    )
    visual_graphic_blocks: List[Dict] = []
    if source_path.suffix.lower() != ".pdf":
        visual_graphic_blocks = extract_visual_graphic_blocks(
            page_path=str(page.path),
            visual_text_blocks=visual_text_blocks,
        )
    refine_layout_blocks_with_visual_text(
        detected_blocks,
        visual_text_blocks,
        page_width=page.width,
        page_height=page.height,
    )
    visual_text_merged_blocks = merge_visual_text_blocks(
        visual_text_blocks,
        page_width=page.width,
        page_height=page.height,
    )

    combined_blocks = (
        _page_fallback_blocks(page)
        + detected_blocks
        + visual_graphic_blocks
        + visual_text_merged_blocks
        + visual_text_blocks
        + native_blocks
    )
    source_counts = _source_counts(combined_blocks)
    ordered_blocks = prepare_blocks_for_ocr(
        combined_blocks,
        page_width=page.width,
        page_height=page.height,
    )

    block_results: List[BlockResult] = []
    page_crop_dir = crops_dir / f"page-{page.page_number:03d}"
    if settings.save_debug_crops:
        page_crop_dir.mkdir(parents=True, exist_ok=True)

    with Image.open(page.path) as image:
        rgb = image.convert("RGB")
        for index, block in enumerate(ordered_blocks, start=1):
            bbox = block["bbox"]
            if block["type"] == "Page" or not settings.save_debug_crops:
                relative_crop = page.path.relative_to(settings.results_dir).as_posix()
            else:
                crop_name = _crop_name(index, block)
                crop_path = page_crop_dir / crop_name
                rgb.crop(tuple(int(round(value)) for value in bbox)).save(crop_path)
                relative_crop = crop_path.relative_to(settings.results_dir).as_posix()

            block_results.append(
                BlockResult(
                    id=f"p{page.page_number}-b{index}",
                    page=page.page_number,
                    type=block["type"],
                    bbox=bbox,
                    score=block["score"],
                    reading_order=index,
                    analysis_hint=block.get("analysis_hint") or analysis_hint_for_block(block),
                    crop_path=relative_crop,
                    crop_url=asset_url(request_base_url, relative_crop),
                    text=block.get("text"),
                    metadata=block.get("metadata", {}),
                )
            )

    _resolve_merged_children(block_results)
    relative_page = page.path.relative_to(settings.results_dir).as_posix()
    return PageResult(
        page=page.page_number,
        width=page.width,
        height=page.height,
        image_path=relative_page,
        image_url=asset_url(request_base_url, relative_page),
        blocks=block_results,
        metadata={
            "source_counts": source_counts,
            "coverage_strategy": [
                "layout_parser_detection",
                "visual_text_region_detection",
                "visual_graphic_region_detection",
                "visual_text_candidate_merging",
                "pdf_native_text_image_blocks",
                "full_page_fallback",
            ],
            "debug_crops_saved": settings.save_debug_crops,
        },
    )


def _clamp_bbox(bbox: List[float], width: int, height: int) -> List[float]:
    return clamp_bbox(bbox, width, height)


def _safe_type(block_type: str) -> str:
    return re.sub(r"[^A-Za-z0-9_-]+", "_", block_type).strip("_") or "Block"


def _crop_name(index: int, block: Dict) -> str:
    metadata = block.get("metadata", {})
    if metadata.get("source") == "visual_text_merged":
        sequence = int(metadata.get("sequence") or index)
        return f"merged-text-{sequence:03d}.png"
    return f"block-{index:03d}-{_safe_type(block['type'])}.png"


def _attach_clean_crops(
    *,
    page_results: Sequence[PageResult],
    downstream_targets: Sequence[DownstreamTarget],
    clean_crops_dir: Path,
    request_base_url: str,
) -> None:
    targets_by_block_id = {target.block_id: target for target in downstream_targets}

    for page in page_results:
        clean_page_dir = clean_crops_dir / f"page-{page.page:03d}"
        clean_page_dir.mkdir(parents=True, exist_ok=True)
        blocks_by_id = {block.id: block for block in page.clean_blocks}

        with Image.open(settings.results_dir / page.image_path) as image:
            rgb = image.convert("RGB")
            for target in downstream_targets:
                if target.page != page.page or target.source != TEXT_CANVAS_SOURCE:
                    continue

                crop_name = f"page-{page.page:03d}-text-canvas.png"
                crop_path = clean_page_dir / crop_name
                canvas = _render_text_canvas(page, rgb, target)
                ocr_canvas = _prepare_ocr_text_canvas_for_delivery(
                    canvas=canvas,
                    target=target,
                    page=page,
                    clean_page_dir=clean_page_dir,
                    crop_name=crop_name,
                    request_base_url=request_base_url,
                )
                ocr_canvas.save(crop_path)

                relative_crop = crop_path.relative_to(settings.results_dir).as_posix()
                target.crop_path = relative_crop
                target.crop_url = asset_url(request_base_url, relative_crop)
                page.metadata["ocr_text_canvas"] = {
                    "target_id": target.id,
                    "crop_path": relative_crop,
                    "crop_url": target.crop_url,
                    "source": TEXT_CANVAS_SOURCE,
                    "text_region_count": int(target.metadata.get("text_region_count", 0) or 0),
                    "canvas_size": target.metadata.get("canvas_size"),
                    "compression_ratio": target.metadata.get("compression_ratio"),
                    "ocr_input_size": target.metadata.get("ocr_input_size"),
                    "ocr_input_scale": target.metadata.get("ocr_input_scale"),
                    "ocr_input_compression_ratio": target.metadata.get(
                        "ocr_input_compression_ratio"
                    ),
                    "original_canvas_path": target.metadata.get("original_canvas_path"),
                    "original_canvas_url": target.metadata.get("original_canvas_url"),
                    "original_text_content_bbox": target.metadata.get("original_text_content_bbox"),
                    "canvas_content_bbox": target.metadata.get("canvas_content_bbox"),
                }

            for index, region in enumerate(page.layout_regions, start=1):
                if region.get("route") == OCR_CANVAS_ROUTE:
                    region["crop_path"] = None
                    region["crop_url"] = None
                    continue

                block_id = str(region.get("block_id", ""))
                target = targets_by_block_id.get(block_id)
                if target is None:
                    continue

                block = blocks_by_id.get(block_id)
                if block is None:
                    continue

                crop_name = _clean_crop_name(index, region)
                crop_path = clean_page_dir / crop_name
                bbox = tuple(int(round(value)) for value in region["bbox"])
                rgb.crop(bbox).save(crop_path)

                relative_crop = crop_path.relative_to(settings.results_dir).as_posix()
                crop_url = asset_url(request_base_url, relative_crop)

                block.crop_path = relative_crop
                block.crop_url = crop_url
                region["crop_path"] = relative_crop
                region["crop_url"] = crop_url

                target.crop_path = relative_crop
                target.crop_url = crop_url

        page.metadata["clean_crops_dir"] = (
            clean_page_dir.relative_to(settings.results_dir).as_posix()
        )
        page.metadata["clean_crop_strategy"] = (
            "Text layout regions are rendered onto one page-level OCR canvas; figure/table regions are cropped separately."
        )


def _clean_crop_name(index: int, region: Dict) -> str:
    region_type = _safe_type(str(region.get("region_type", "region")))
    route = _safe_type(str(region.get("route", "none")))
    return f"region-{index:03d}-{region_type}-{route}.png"


def _render_text_canvas(
    page: PageResult,
    image: Image.Image,
    target: DownstreamTarget,
) -> Image.Image:
    plan = _build_text_canvas_plan(page, target)
    canvas = Image.new("RGB", plan["canvas_size"], "white")
    for placement in plan["placements"]:
        x1, y1, x2, y2 = placement["original_bbox_int"]
        canvas_x1, canvas_y1, _, _ = placement["canvas_bbox_int"]
        canvas.paste(image.crop((x1, y1, x2, y2)), (canvas_x1, canvas_y1))

    target.metadata["canvas_strategy"] = "compact_text_only_original_x_collapsed_y_gaps"
    target.metadata["canvas_size"] = list(plan["canvas_size"])
    target.metadata["original_page_size"] = [page.width, page.height]
    target.metadata["original_text_content_bbox"] = plan["original_text_content_bbox"]
    target.metadata["canvas_content_bbox"] = plan["canvas_content_bbox"]
    target.metadata["canvas_region_map"] = plan["placements"]
    target.metadata["vertical_gap_policy"] = {
        "max_gap_pixels": plan["max_vertical_gap"],
        "reason": "Large gaps left by removed figures/tables are collapsed for OCR efficiency.",
    }
    target.metadata["compression_ratio"] = round(
        (plan["canvas_size"][0] * plan["canvas_size"][1]) / float(page.width * page.height),
        6,
    )
    target.metadata["canvas_region_map_coordinate_space"] = "canonical_canvas"
    return canvas


def _prepare_ocr_text_canvas_for_delivery(
    *,
    canvas: Image.Image,
    target: DownstreamTarget,
    page: PageResult,
    clean_page_dir: Path,
    crop_name: str,
    request_base_url: str,
) -> Image.Image:
    plan = _ocr_text_canvas_downscale_plan(
        canvas.size,
        target.metadata.get("canvas_region_map", []),
        page_width=page.width,
        page_height=page.height,
    )
    target.metadata["ocr_input_scale"] = plan["scale"]
    target.metadata["ocr_input_size"] = list(plan["size"])
    target.metadata["ocr_input_compression_ratio"] = round(
        (plan["size"][0] * plan["size"][1]) / float(page.width * page.height),
        6,
    )
    target.metadata["ocr_input_downscale"] = {
        "applied": plan["applied"],
        "reason": plan["reason"],
        "max_long_side": settings.ocr_text_canvas_max_long_side,
        "max_pixels": settings.ocr_text_canvas_max_pixels,
        "min_text_height": settings.ocr_text_canvas_min_text_height,
        "guard_text_height": plan["guard_text_height"],
        "estimated_guard_text_height_after": plan["estimated_guard_text_height_after"],
    }

    if not plan["applied"]:
        return canvas

    if settings.ocr_text_canvas_keep_original:
        original_name = crop_name.replace(".png", "-original.png")
        original_path = clean_page_dir / original_name
        canvas.save(original_path)
        relative_original = original_path.relative_to(settings.results_dir).as_posix()
        target.metadata["original_canvas_path"] = relative_original
        target.metadata["original_canvas_url"] = asset_url(request_base_url, relative_original)

    resampling = getattr(Image, "Resampling", Image).LANCZOS
    return canvas.resize(plan["size"], resampling)


def _ocr_text_canvas_downscale_plan(
    canvas_size: Tuple[int, int],
    placements: Sequence[Dict],
    *,
    page_width: int,
    page_height: int,
) -> Dict:
    width, height = canvas_size
    if width <= 0 or height <= 0:
        return _ocr_text_canvas_no_downscale(canvas_size, reason="empty_canvas")

    max_long_side = max(0, int(settings.ocr_text_canvas_max_long_side))
    max_pixels = max(0, int(settings.ocr_text_canvas_max_pixels))
    min_text_height = max(0, int(settings.ocr_text_canvas_min_text_height))

    scale = 1.0
    reasons: List[str] = []
    longest = max(width, height)
    area = width * height

    if max_long_side > 0 and longest > max_long_side:
        scale = min(scale, max_long_side / float(longest))
        reasons.append("max_long_side")

    if max_pixels > 0 and area > max_pixels:
        scale = min(scale, sqrt(max_pixels / float(area)))
        reasons.append("max_pixels")

    guard_text_height = _canvas_guard_text_height(placements)
    if guard_text_height and min_text_height > 0:
        estimated_after = guard_text_height * scale
        if estimated_after < min_text_height:
            scale = min(1.0, min_text_height / float(guard_text_height))
            reasons.append("min_text_height_guard")

    if scale >= 0.95:
        plan = _ocr_text_canvas_no_downscale(
            canvas_size,
            reason="within_limits" if not reasons else "minor_reduction_skipped",
        )
        plan["guard_text_height"] = guard_text_height
        plan["estimated_guard_text_height_after"] = (
            round(guard_text_height, 3) if guard_text_height else None
        )
        return plan

    target_width = max(1, int(round(width * scale)))
    target_height = max(1, int(round(height * scale)))
    estimated_after = guard_text_height * scale if guard_text_height else None
    return {
        "applied": True,
        "scale": round(scale, 6),
        "size": (target_width, target_height),
        "reason": ",".join(_ordered_unique(reasons)) or "downscaled",
        "guard_text_height": round(guard_text_height, 3) if guard_text_height else None,
        "estimated_guard_text_height_after": (
            round(estimated_after, 3) if estimated_after else None
        ),
        "page_size": (page_width, page_height),
    }


def _ocr_text_canvas_no_downscale(canvas_size: Tuple[int, int], *, reason: str) -> Dict:
    return {
        "applied": False,
        "scale": 1.0,
        "size": canvas_size,
        "reason": reason,
        "guard_text_height": None,
        "estimated_guard_text_height_after": None,
    }


def _canvas_guard_text_height(placements: Sequence[Dict]) -> Optional[float]:
    heights: List[float] = []
    for placement in placements:
        bbox = placement.get("canvas_bbox") or placement.get("canvas_bbox_int") or []
        if len(bbox) != 4:
            continue
        height = float(bbox[3]) - float(bbox[1])
        if height > 0:
            heights.append(height)

    if not heights:
        return None

    heights.sort()
    index = max(0, int(len(heights) * 0.10) - 1)
    return heights[index]


def _build_text_canvas_plan(page: PageResult, target: DownstreamTarget) -> Dict:
    items = _text_canvas_items(page, target)

    if not items:
        return {
            "canvas_size": (max(1, page.width), 1),
            "placements": [],
            "original_text_content_bbox": [],
            "canvas_content_bbox": [],
            "max_vertical_gap": 0,
        }

    items.sort(key=lambda item: (item["bbox"][1], item["bbox"][0], item["reading_order"]))
    boxes = [item["bbox"] for item in items]
    content_bbox = _integer_union_bboxes(boxes)
    content_x1, content_y1, content_x2, _ = content_bbox
    pad_x = max(12, int(round(page.width * 0.018)))
    pad_y = max(12, int(round(page.height * 0.012)))
    x_offset = max(0, content_x1 - pad_x)
    right_edge = min(page.width, content_x2 + pad_x)
    canvas_width = max(1, right_edge - x_offset)
    max_vertical_gap = _compact_canvas_max_vertical_gap(boxes, page.height)

    previous_original_bottom = content_y1
    previous_canvas_bottom = pad_y
    placements: List[Dict] = []
    canvas_content_x1: Optional[int] = None
    canvas_content_y1: Optional[int] = None
    canvas_content_x2: Optional[int] = None
    canvas_content_y2: Optional[int] = None

    for item in items:
        x1, y1, x2, y2 = item["bbox"]
        gap = y1 - previous_original_bottom
        mapped_gap = min(gap, max_vertical_gap) if gap > 0 else gap
        canvas_y1 = max(0, previous_canvas_bottom + mapped_gap)
        canvas_y2 = canvas_y1 + (y2 - y1)
        canvas_x1 = x1 - x_offset
        canvas_x2 = canvas_x1 + (x2 - x1)

        original_bbox = [x1, y1, x2, y2]
        canvas_bbox = [canvas_x1, canvas_y1, canvas_x2, canvas_y2]
        placements.append(
            {
                "region_id": item.get("region_id"),
                "block_id": item.get("block_id"),
                "source_block_id": item.get("source_block_id"),
                "source": item.get("source"),
                "reading_order": item.get("reading_order"),
                "original_bbox": original_bbox,
                "canvas_bbox": canvas_bbox,
                "original_bbox_int": original_bbox,
                "canvas_bbox_int": canvas_bbox,
            }
        )

        canvas_content_x1 = canvas_x1 if canvas_content_x1 is None else min(canvas_content_x1, canvas_x1)
        canvas_content_y1 = canvas_y1 if canvas_content_y1 is None else min(canvas_content_y1, canvas_y1)
        canvas_content_x2 = canvas_x2 if canvas_content_x2 is None else max(canvas_content_x2, canvas_x2)
        canvas_content_y2 = canvas_y2 if canvas_content_y2 is None else max(canvas_content_y2, canvas_y2)
        previous_original_bottom = max(previous_original_bottom, y2)
        previous_canvas_bottom = max(previous_canvas_bottom, canvas_y2)

    canvas_height = max(1, int(previous_canvas_bottom + pad_y))
    return {
        "canvas_size": (canvas_width, canvas_height),
        "placements": placements,
        "original_text_content_bbox": list(content_bbox),
        "canvas_content_bbox": [
            int(canvas_content_x1 or 0),
            int(canvas_content_y1 or 0),
            int(canvas_content_x2 or 0),
            int(canvas_content_y2 or 0),
        ],
        "max_vertical_gap": max_vertical_gap,
    }


def _text_canvas_items(page: PageResult, target: DownstreamTarget) -> List[Dict]:
    source_block_ids = set(target.child_block_ids)
    precise_used: set = set()
    items: List[Dict] = []

    for region in page.layout_regions:
        if region.get("region_type") != "text_region":
            continue
        if str(region.get("block_id", "")) not in source_block_ids:
            continue

        region_bbox = _integer_bbox(region.get("bbox", []), width=page.width, height=page.height)
        if region_bbox is None:
            continue

        precise_blocks = _precise_text_blocks_for_region(page, region_bbox, precise_used)
        if precise_blocks:
            for block in precise_blocks:
                bbox = _integer_bbox(block.bbox, width=page.width, height=page.height)
                if bbox is None:
                    continue
                precise_used.add(block.id)
                items.append(
                    {
                        "region_id": region.get("id"),
                        "block_id": region.get("block_id"),
                        "source_block_id": block.id,
                        "source": str(block.metadata.get("source", "unknown")),
                        "bbox": bbox,
                        "reading_order": block.reading_order,
                    }
                )
            continue

        items.append(
            {
                "region_id": region.get("id"),
                "block_id": region.get("block_id"),
                "source_block_id": region.get("block_id"),
                "source": region.get("source"),
                "bbox": region_bbox,
                "reading_order": int(region.get("reading_order", 0) or 0),
            }
        )

    return items


def _precise_text_blocks_for_region(
    page: PageResult,
    region_bbox: Sequence[int],
    used_block_ids: set,
) -> List[BlockResult]:
    precise_blocks: List[BlockResult] = []
    region_area = _area(region_bbox)
    for block in page.blocks:
        if block.id in used_block_ids:
            continue

        source = str(block.metadata.get("source", ""))
        if source not in {"visual_text", "visual_text_merged", "pdf_native"}:
            continue
        if block.analysis_hint not in {"ocr", "embedded_text"}:
            continue

        block_bbox = _integer_bbox(block.bbox, width=page.width, height=page.height)
        if block_bbox is None:
            continue
        block_area = _area(block_bbox)
        if block_area <= 0:
            continue

        overlap = _intersection_area(region_bbox, block_bbox)
        center_inside = _center_inside(block_bbox, region_bbox)
        mostly_inside = overlap / block_area >= 0.72
        if center_inside and mostly_inside:
            precise_blocks.append(block)

    if not precise_blocks:
        return []

    precise_area = sum(_area(block.bbox) for block in precise_blocks)
    if region_area > 0 and precise_area / region_area < 0.015:
        return []

    return sorted(precise_blocks, key=lambda block: (block.bbox[1], block.bbox[0], block.reading_order))


def _compact_canvas_max_vertical_gap(
    boxes: Sequence[Sequence[int]],
    page_height: int,
) -> int:
    heights = sorted(max(1, int(box[3]) - int(box[1])) for box in boxes)
    median_height = heights[len(heights) // 2]
    return int(max(28, min(page_height * 0.035, median_height * 1.8, 82)))


def _integer_union_bboxes(boxes: Sequence[Sequence[int]]) -> Tuple[int, int, int, int]:
    return (
        min(int(box[0]) for box in boxes),
        min(int(box[1]) for box in boxes),
        max(int(box[2]) for box in boxes),
        max(int(box[3]) for box in boxes),
    )


def _integer_bbox(
    bbox: Sequence[float],
    *,
    width: int,
    height: int,
) -> Optional[Tuple[int, int, int, int]]:
    if len(bbox) != 4:
        return None

    clamped = _clamp_bbox([float(value) for value in bbox], width, height)
    x1, y1, x2, y2 = (int(round(value)) for value in clamped)
    if x2 <= x1 or y2 <= y1:
        return None
    return x1, y1, x2, y2


def _build_ocr_text_canvas_targets(
    page_results: Sequence[PageResult],
    ocr_targets: Sequence[DownstreamTarget],
) -> List[DownstreamTarget]:
    page_by_number = {page.page: page for page in page_results}
    targets_by_page: Dict[int, List[DownstreamTarget]] = {}
    for target in ocr_targets:
        targets_by_page.setdefault(target.page, []).append(target)

    canvas_targets: List[DownstreamTarget] = []
    for page_number in sorted(targets_by_page):
        page = page_by_number[page_number]
        targets = sorted(
            targets_by_page[page_number],
            key=lambda item: item.reading_order,
        )
        bbox = [0.0, 0.0, float(page.width), float(page.height)]
        content_bbox = _union_bboxes([target.bbox for target in targets])
        child_block_ids = _ordered_unique(
            [
                block_id
                for target in targets
                for block_id in [target.block_id, *target.child_block_ids]
            ]
        )
        source_target_ids = [target.id for target in targets]

        canvas_targets.append(
            DownstreamTarget(
                id=f"ocr-{len(canvas_targets) + 1:04d}",
                target_type="ocr",
                route="ocr_server",
                block_id=f"p{page_number}-ocr-text-canvas",
                page=page_number,
                type="PageTextCanvas",
                source=TEXT_CANVAS_SOURCE,
                priority=max(target.priority for target in targets),
                reading_order=min(target.reading_order for target in targets),
                bbox=bbox,
                bbox_normalized=_normalized_bbox(bbox, width=page.width, height=page.height),
                crop_path=page.image_path,
                crop_url=page.image_url,
                page_image_path=page.image_path,
                page_image_url=page.image_url,
                child_block_ids=child_block_ids,
                related_block_ids=source_target_ids,
                metadata={
                    "mode": "page_text_canvas_ocr",
                    "reason": (
                        "All OCR text regions on the page are rendered onto a compact white "
                        "canvas; original layout coordinates are retained in canvas_region_map."
                    ),
                    "canvas_strategy": "compact_text_only_original_x_collapsed_y_gaps",
                    "text_region_count": len(targets),
                    "source_target_ids": source_target_ids,
                    "original_text_content_bbox": content_bbox,
                    "original_page_size": [page.width, page.height],
                },
            )
        )

    return canvas_targets


def _mark_ocr_regions_for_text_canvas(
    page_results: Sequence[PageResult],
    ocr_targets: Sequence[DownstreamTarget],
) -> None:
    target_by_block_id = {
        block_id: target
        for target in ocr_targets
        for block_id in target.child_block_ids
    }
    for page in page_results:
        for region in page.layout_regions:
            if region.get("region_type") != "text_region":
                continue

            target = target_by_block_id.get(str(region.get("block_id", "")))
            if target is None:
                continue

            region["target_type"] = "ocr_member"
            region["route"] = OCR_CANVAS_ROUTE
            region["bundled_target_id"] = target.id
            region["crop_path"] = None
            region["crop_url"] = None


def _refresh_layout_summaries(page_results: Sequence[PageResult]) -> None:
    for page in page_results:
        counts: Dict[str, int] = {}
        route_counts: Dict[str, int] = {}
        covered_area = 0.0
        for region in page.layout_regions:
            region_type = str(region.get("region_type", "unknown"))
            route = str(region.get("route", "none"))
            counts[region_type] = counts.get(region_type, 0) + 1
            route_counts[route] = route_counts.get(route, 0) + 1
            covered_area += _area(region.get("bbox", []))

        page_area = float(page.width * page.height)
        page.metadata["layout_summary"] = {
            "region_counts": counts,
            "route_counts": route_counts,
            "region_count": len(page.layout_regions),
            "clean_block_count": len(page.clean_blocks),
            "coverage_ratio_sum": round(covered_area / page_area, 6) if page_area > 0 else 0.0,
            "strategy": (
                "text regions are rendered into one page-level OCR canvas; image/table "
                "regions remain direct downstream targets"
            ),
        }


def _ordered_unique(values: Sequence[str]) -> List[str]:
    selected: List[str] = []
    seen = set()
    for value in values:
        if value in seen:
            continue
        selected.append(value)
        seen.add(value)
    return selected


def _box_width(box: Sequence[float]) -> float:
    return max(0.0, float(box[2]) - float(box[0]))


def _box_height(box: Sequence[float]) -> float:
    return max(0.0, float(box[3]) - float(box[1]))


def _area(box: Sequence[float]) -> float:
    if len(box) < 4:
        return 0.0
    return _box_width(box) * _box_height(box)


def _intersection_area(a: Sequence[float], b: Sequence[float]) -> float:
    x1 = max(float(a[0]), float(b[0]))
    y1 = max(float(a[1]), float(b[1]))
    x2 = min(float(a[2]), float(b[2]))
    y2 = min(float(a[3]), float(b[3]))
    return max(0.0, x2 - x1) * max(0.0, y2 - y1)


def _center_inside(inner: Sequence[float], outer: Sequence[float]) -> bool:
    center_x = (float(inner[0]) + float(inner[2])) / 2.0
    center_y = (float(inner[1]) + float(inner[3])) / 2.0
    return (
        float(outer[0]) <= center_x <= float(outer[2])
        and float(outer[1]) <= center_y <= float(outer[3])
    )


def _union_bboxes(boxes: Sequence[Sequence[float]]) -> List[float]:
    return [
        min(float(box[0]) for box in boxes),
        min(float(box[1]) for box in boxes),
        max(float(box[2]) for box in boxes),
        max(float(box[3]) for box in boxes),
    ]


def _normalized_bbox(bbox: Sequence[float], *, width: int, height: int) -> List[float]:
    return [
        round(float(bbox[0]) / float(width), 6),
        round(float(bbox[1]) / float(height), 6),
        round(float(bbox[2]) / float(width), 6),
        round(float(bbox[3]) / float(height), 6),
    ]


def _compact_layout_result(result: Dict) -> Dict:
    return {
        "job_id": result["job_id"],
        "document_id": result["document_id"],
        "status": result["status"],
        "created_at": result["created_at"],
        "completed_at": result["completed_at"],
        "pages": [
            {
                "page": page["page"],
                "width": page["width"],
                "height": page["height"],
                "image_path": page["image_path"],
                "image_url": page["image_url"],
                "layout_summary": page.get("metadata", {}).get("layout_summary", {}),
                "clean_crops_dir": page.get("metadata", {}).get("clean_crops_dir"),
                "ocr_text_canvas": page.get("metadata", {}).get("ocr_text_canvas"),
                "layout_regions": [
                    _compact_region(region)
                    for region in page.get("layout_regions", [])
                ],
            }
            for page in result.get("pages", [])
        ],
        "targets": {
            "ocr": [_compact_target(target) for target in result.get("ocr_targets", [])],
            "image": [_compact_target(target) for target in result.get("image_targets", [])],
            "table": [_compact_target(target) for target in result.get("table_targets", [])],
        },
    }


def _compact_region(region: Dict) -> Dict:
    direct_crop = region.get("route") != OCR_CANVAS_ROUTE
    return {
        "id": region.get("id"),
        "page": region.get("page"),
        "region_type": region.get("region_type"),
        "target_type": region.get("target_type"),
        "route": region.get("route"),
        "label": region.get("label"),
        "source": region.get("source"),
        "reading_order": region.get("reading_order"),
        "bbox": region.get("bbox"),
        "bbox_normalized": region.get("bbox_normalized"),
        "score": region.get("score"),
        "crop_path": region.get("crop_path") if direct_crop else None,
        "crop_url": region.get("crop_url") if direct_crop else None,
        "layout_role": region.get("layout_role"),
        "child_block_ids": region.get("child_block_ids", []),
        "bundled_target_id": region.get("bundled_target_id"),
    }


def _compact_target(target: Dict) -> Dict:
    return {
        "id": target.get("id"),
        "block_id": target.get("block_id"),
        "page": target.get("page"),
        "target_type": target.get("target_type"),
        "route": target.get("route"),
        "type": target.get("type"),
        "source": target.get("source"),
        "priority": target.get("priority"),
        "reading_order": target.get("reading_order"),
        "bbox": target.get("bbox"),
        "bbox_normalized": target.get("bbox_normalized"),
        "crop_path": target.get("crop_path"),
        "crop_url": target.get("crop_url"),
        "page_image_path": target.get("page_image_path"),
        "page_image_url": target.get("page_image_url"),
        "child_block_ids": target.get("child_block_ids", []),
        "related_block_ids": target.get("related_block_ids", []),
        "metadata": target.get("metadata", {}),
    }


def _resolve_merged_children(block_results: List[BlockResult]) -> None:
    candidate_to_block_id = {
        str(block.metadata.get("candidate_id")): block.id
        for block in block_results
        if block.metadata.get("source") == "visual_text"
        and block.metadata.get("candidate_id")
    }

    for block in block_results:
        if block.metadata.get("source") != "visual_text_merged":
            continue

        child_candidate_ids = [
            str(candidate_id)
            for candidate_id in block.metadata.get("child_candidate_ids", [])
        ]
        block.metadata["children"] = [
            candidate_to_block_id[candidate_id]
            for candidate_id in child_candidate_ids
            if candidate_id in candidate_to_block_id
        ]


def _analysis_hint(block_type: str) -> str:
    return analysis_hint_for_block({"type": block_type, "metadata": {}})


def _page_fallback_blocks(page: PageImage) -> List[Dict]:
    if not settings.include_full_page_block:
        return []
    return [
        {
            "type": "Page",
            "bbox": [0.0, 0.0, float(page.width), float(page.height)],
            "score": 1.0,
            "metadata": {
                "source": "page_fallback",
                "role": "coverage_guard",
                "reason": "Allows downstream recovery if region detectors miss content.",
            },
        }
    ]


def _source_counts(blocks: List[Dict]) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    for block in blocks:
        source = block.get("metadata", {}).get("source", "unknown")
        counts[source] = counts.get(source, 0) + 1
    return counts
