from __future__ import annotations

from copy import deepcopy
from typing import Dict, Iterable, List, Sequence, Tuple


TEXT_TYPES = {"text", "title", "list"}
TABLE_TYPES = {"table"}
FIGURE_TYPES = {"figure"}
PAGE_TYPES = {"page"}


def prepare_blocks_for_ocr(
    blocks: Sequence[Dict],
    *,
    page_width: int,
    page_height: int,
) -> List[Dict]:
    """Normalize, enrich, and order layout blocks for downstream OCR/image work."""

    prepared = [
        _normalize_block(block, page_width=page_width, page_height=page_height)
        for block in blocks
    ]
    prepared = [block for block in prepared if _area(block["bbox"]) > 0]

    _mark_pdf_native_redundancy(prepared)
    for block in prepared:
        _apply_ocr_metadata(block)

    return reading_order(prepared, page_width=page_width, page_height=page_height)


def reading_order(
    blocks: Sequence[Dict],
    *,
    page_width: int,
    page_height: int,
) -> List[Dict]:
    page_blocks = [block for block in blocks if _type_key(block) in PAGE_TYPES]
    content_blocks = [block for block in blocks if _type_key(block) not in PAGE_TYPES]
    ordered_content = _recursive_xy_order(
        content_blocks,
        page_width=page_width,
        page_height=page_height,
        depth=0,
    )
    return sorted(page_blocks, key=_top_left_key) + ordered_content


def clamp_bbox(bbox: Sequence[float], width: int, height: int) -> List[float]:
    x1, y1, x2, y2 = [float(value) for value in bbox]
    x1 = max(0.0, min(float(width), x1))
    y1 = max(0.0, min(float(height), y1))
    x2 = max(0.0, min(float(width), x2))
    y2 = max(0.0, min(float(height), y2))

    if x2 <= x1:
        x2 = min(float(width), x1 + 1.0)
    if y2 <= y1:
        y2 = min(float(height), y1 + 1.0)

    return [x1, y1, x2, y2]


def analysis_hint_for_block(block: Dict) -> str:
    metadata = block.get("metadata", {})
    ocr = metadata.get("ocr", {})
    block_type = _type_key(block)

    if block_type in PAGE_TYPES:
        return "full_page"
    if metadata.get("source") == "pdf_native" and metadata.get("has_pdf_text"):
        return "embedded_text"
    if ocr.get("redundant_with") == "pdf_native_text":
        return "layout"
    if block_type in TEXT_TYPES:
        return "ocr"
    if block_type in TABLE_TYPES:
        return "table"
    if block_type in FIGURE_TYPES:
        return "figure"
    return "layout"


def _normalize_block(block: Dict, *, page_width: int, page_height: int) -> Dict:
    normalized = deepcopy(block)
    normalized.setdefault("type", "Unknown")
    normalized.setdefault("score", 0.0)
    normalized["bbox"] = clamp_bbox(normalized.get("bbox", [0, 0, 1, 1]), page_width, page_height)

    metadata = normalized.setdefault("metadata", {})
    roles = set(metadata.get("roles", []))
    role = metadata.get("role")
    if role:
        roles.add(role)
    metadata["roles"] = sorted(roles)
    metadata["bbox_normalized"] = _normalized_bbox(
        normalized["bbox"],
        width=page_width,
        height=page_height,
    )
    metadata["area_ratio"] = round(_area(normalized["bbox"]) / float(page_width * page_height), 6)
    return normalized


def _apply_ocr_metadata(block: Dict) -> None:
    metadata = block.setdefault("metadata", {})
    roles = set(metadata.get("roles", []))
    ocr = dict(metadata.get("ocr", {}))
    block_type = _type_key(block)
    source = metadata.get("source")

    if block_type in PAGE_TYPES:
        roles.add("coverage_guard")
        ocr.update(
            {
                "recommended": False,
                "priority": 5,
                "mode": "fallback_full_page",
                "fallback_only": True,
                "reason": "Use only when region-level extraction is insufficient.",
            }
        )
    elif source == "pdf_native" and metadata.get("has_pdf_text"):
        roles.add("embedded_text")
        ocr.update(
            {
                "recommended": False,
                "priority": 0,
                "mode": "embedded_text",
                "reason": "Text already exists in the source PDF; OCR is not required.",
            }
        )
    elif ocr.get("redundant_with") == "pdf_native_text":
        roles.add("semantic_region")
        ocr.update(
            {
                "recommended": False,
                "priority": 15,
                "mode": "skip_duplicate",
                "reason": "Covered by overlapping PDF-native text.",
            }
        )
    elif block_type in TEXT_TYPES:
        roles.add("ocr_region")
        roles.add("semantic_region")
        if source == "visual_text_merged":
            priority = 100
            mode = "merged_text_ocr"
            reason = "Merged visual text region preferred for OCR before source candidates."
        else:
            priority = _text_priority(block_type, source)
            mode = "text_ocr"
            reason = "Text-like region suitable for OCR."
        ocr.update(
            {
                "recommended": True,
                "priority": priority,
                "mode": mode,
                "reason": reason,
            }
        )
    elif block_type in TABLE_TYPES:
        roles.add("table_region")
        ocr.update(
            {
                "recommended": True,
                "priority": 70,
                "mode": "table_ocr",
                "reason": "Table region should use OCR or table extraction downstream.",
            }
        )
    elif block_type in FIGURE_TYPES:
        roles.add("image_region")
        ocr.update(
            {
                "recommended": False,
                "priority": 25,
                "mode": "image_analysis",
                "reason": "Figure region is better routed to image analysis before OCR.",
            }
        )
    else:
        roles.add("semantic_region")
        ocr.update(
            {
                "recommended": False,
                "priority": 20,
                "mode": "layout",
                "reason": "Semantic layout region with no direct OCR recommendation.",
            }
        )

    metadata["roles"] = sorted(roles)
    metadata["ocr"] = ocr
    block["analysis_hint"] = analysis_hint_for_block(block)


def _mark_pdf_native_redundancy(blocks: List[Dict]) -> None:
    native_text_blocks = [
        block
        for block in blocks
        if block.get("metadata", {}).get("source") == "pdf_native"
        and block.get("metadata", {}).get("has_pdf_text")
        and _area(block.get("bbox", [])) > 0
    ]
    if not native_text_blocks:
        return

    for block in blocks:
        metadata = block.setdefault("metadata", {})
        source = metadata.get("source")
        if source == "pdf_native" or _type_key(block) not in TEXT_TYPES:
            continue
        if source in {"visual_text", "visual_text_merged"}:
            continue

        block_area = _area(block.get("bbox", []))
        if block_area <= 0:
            continue

        overlap_area = sum(
            _intersection_area(block["bbox"], native_block["bbox"])
            for native_block in native_text_blocks
        )
        coverage = min(1.0, overlap_area / block_area)
        if coverage >= 0.72:
            ocr = metadata.setdefault("ocr", {})
            ocr["redundant_with"] = "pdf_native_text"
            ocr["native_text_coverage"] = round(coverage, 4)


def _recursive_xy_order(
    blocks: Sequence[Dict],
    *,
    page_width: int,
    page_height: int,
    depth: int,
) -> List[Dict]:
    if len(blocks) <= 1 or depth > 8:
        return sorted(blocks, key=_top_left_key)

    spanning_order = _order_around_spanning_blocks(
        blocks,
        page_width=page_width,
        page_height=page_height,
        depth=depth,
    )
    if spanning_order is not None:
        return spanning_order

    vertical_groups = _split_by_axis(
        blocks,
        axis="x",
        min_gap=max(18.0, page_width * 0.025),
    )
    if len(vertical_groups) > 1:
        return [
            block
            for group in vertical_groups
            for block in _recursive_xy_order(
                group,
                page_width=page_width,
                page_height=page_height,
                depth=depth + 1,
            )
        ]

    horizontal_groups = _split_by_axis(
        blocks,
        axis="y",
        min_gap=max(10.0, page_height * 0.012),
    )
    if len(horizontal_groups) > 1:
        return [
            block
            for group in horizontal_groups
            for block in _recursive_xy_order(
                group,
                page_width=page_width,
                page_height=page_height,
                depth=depth + 1,
            )
        ]

    return sorted(blocks, key=_top_left_key)


def _order_around_spanning_blocks(
    blocks: Sequence[Dict],
    *,
    page_width: int,
    page_height: int,
    depth: int,
) -> List[Dict] | None:
    spanning_blocks = [
        block
        for block in blocks
        if (float(block["bbox"][2]) - float(block["bbox"][0])) / float(page_width) >= 0.65
    ]
    if not spanning_blocks or len(spanning_blocks) == len(blocks):
        return None

    tolerance = max(8.0, page_height * 0.008)
    remaining = list(blocks)
    ordered: List[Dict] = []

    for spanning in sorted(spanning_blocks, key=_top_left_key):
        if spanning not in remaining:
            continue

        above = [
            block
            for block in remaining
            if block is not spanning and float(block["bbox"][3]) <= float(spanning["bbox"][1]) + tolerance
        ]
        if above:
            ordered.extend(
                _recursive_xy_order(
                    above,
                    page_width=page_width,
                    page_height=page_height,
                    depth=depth + 1,
                )
            )
            remaining = [block for block in remaining if block not in above]

        ordered.append(spanning)
        remaining.remove(spanning)

    if remaining:
        ordered.extend(
            _recursive_xy_order(
                remaining,
                page_width=page_width,
                page_height=page_height,
                depth=depth + 1,
            )
        )

    return ordered


def _split_by_axis(
    blocks: Sequence[Dict],
    *,
    axis: str,
    min_gap: float,
) -> List[List[Dict]]:
    axis_index = 0 if axis == "x" else 1
    sorted_blocks = sorted(blocks, key=lambda item: (item["bbox"][axis_index], item["bbox"][1 - axis_index]))

    groups: List[List[Dict]] = []
    current_group: List[Dict] = []
    current_end = None

    for block in sorted_blocks:
        start = float(block["bbox"][axis_index])
        end = float(block["bbox"][axis_index + 2])

        if current_end is not None and start - current_end > min_gap:
            groups.append(current_group)
            current_group = []

        current_group.append(block)
        current_end = end if current_end is None else max(current_end, end)

    if current_group:
        groups.append(current_group)

    if len(groups) <= 1:
        return groups

    return [sorted(group, key=_top_left_key) for group in groups]


def _normalized_bbox(bbox: Sequence[float], *, width: int, height: int) -> List[float]:
    return [
        round(float(bbox[0]) / float(width), 6),
        round(float(bbox[1]) / float(height), 6),
        round(float(bbox[2]) / float(width), 6),
        round(float(bbox[3]) / float(height), 6),
    ]


def _text_priority(block_type: str, source: object) -> int:
    if source == "visual_text_merged":
        return 100
    if source == "visual_text":
        return 95
    if block_type == "title":
        return 88
    if block_type == "list":
        return 82
    return 85


def _top_left_key(block: Dict) -> Tuple[float, float, float, float]:
    x1, y1, x2, y2 = block["bbox"]
    return (float(y1), float(x1), float(y2), float(x2))


def _type_key(block: Dict) -> str:
    return str(block.get("type", "")).strip().lower()


def _area(box: Iterable[float]) -> float:
    values = list(box)
    if len(values) != 4:
        return 0.0
    return max(0.0, float(values[2]) - float(values[0])) * max(
        0.0,
        float(values[3]) - float(values[1]),
    )


def _intersection_area(a: Sequence[float], b: Sequence[float]) -> float:
    x1 = max(float(a[0]), float(b[0]))
    y1 = max(float(a[1]), float(b[1]))
    x2 = min(float(a[2]), float(b[2]))
    y2 = min(float(a[3]), float(b[3]))
    return max(0.0, x2 - x1) * max(0.0, y2 - y1)
