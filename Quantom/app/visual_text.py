from __future__ import annotations

from pathlib import Path
from statistics import median
from typing import TYPE_CHECKING, Dict, List, Sequence, Tuple, Union

from app.settings import settings

if TYPE_CHECKING:
    from app.document import PageImage


def extract_visual_text_blocks(
    *,
    source_path: Path,
    page: PageImage,
    has_pdf_native_blocks: bool,
) -> List[Dict]:
    if not settings.include_visual_text_blocks:
        return []
    if has_pdf_native_blocks and not settings.visual_text_for_pdf:
        return []

    try:
        import cv2
        import numpy as np
    except ImportError as exc:
        raise RuntimeError("opencv-python is required for visual text detection") from exc

    image = cv2.imread(str(page.path), cv2.IMREAD_COLOR)
    if image is None:
        return []

    height, width = image.shape[:2]
    page_area = float(width * height)
    if page_area <= 0:
        return []

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)
    saturation = hsv[:, :, 1]
    value = hsv[:, :, 2]

    dark_or_colored = (
        ((gray < 205) | ((saturation > 55) & (value < 248))).astype("uint8") * 255
    )
    ink_mask = (
        ((gray < 185) | ((saturation > 80) & (value < 220))).astype("uint8") * 255
    )

    gradient = cv2.morphologyEx(
        gray,
        cv2.MORPH_GRADIENT,
        cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3)),
    )
    _, gradient_mask = cv2.threshold(
        gradient,
        0,
        255,
        cv2.THRESH_BINARY + cv2.THRESH_OTSU,
    )

    masks = [dark_or_colored, gradient_mask]
    candidates: List[Tuple[int, int, int, int]] = []

    for mask in masks:
        closed = cv2.morphologyEx(
            mask,
            cv2.MORPH_CLOSE,
            cv2.getStructuringElement(
                cv2.MORPH_RECT,
                (max(12, width // 75), max(3, height // 220)),
            ),
            iterations=1,
        )
        dilated = cv2.dilate(
            closed,
            cv2.getStructuringElement(
                cv2.MORPH_RECT,
                (max(14, width // 95), max(2, height // 260)),
            ),
            iterations=1,
        )
        contours, _ = cv2.findContours(
            dilated,
            cv2.RETR_EXTERNAL,
            cv2.CHAIN_APPROX_SIMPLE,
        )
        for contour in contours:
            x, y, box_width, box_height = cv2.boundingRect(contour)
            if _is_text_like_box(
                gray,
                x,
                y,
                box_width,
                box_height,
                width,
                height,
                page_area,
            ):
                candidates.append(_pad_box(x, y, box_width, box_height, width, height))

    large_text_mask = cv2.morphologyEx(
        dark_or_colored,
        cv2.MORPH_CLOSE,
        cv2.getStructuringElement(
            cv2.MORPH_RECT,
            (max(24, width // 28), max(6, height // 150)),
        ),
        iterations=1,
    )
    large_text_mask = cv2.dilate(
        large_text_mask,
        cv2.getStructuringElement(
            cv2.MORPH_RECT,
            (max(18, width // 45), max(4, height // 220)),
        ),
        iterations=1,
    )
    contours, _ = cv2.findContours(
        large_text_mask,
        cv2.RETR_EXTERNAL,
        cv2.CHAIN_APPROX_SIMPLE,
    )
    for contour in contours:
        x, y, box_width, box_height = cv2.boundingRect(contour)
        if _is_text_like_box(
            gray,
            x,
            y,
            box_width,
            box_height,
            width,
            height,
            page_area,
        ):
            candidates.append(_pad_box(x, y, box_width, box_height, width, height))

    candidates = _expand_boxes_to_line_ink(candidates, ink_mask, width, height)
    merged = _merge_boxes(candidates, width, height)
    blocks: List[Dict] = []

    for index, box in enumerate(merged, start=1):
        x1, y1, x2, y2 = box
        blocks.append(
            {
                "type": "Text",
                "bbox": [float(x1), float(y1), float(x2), float(y2)],
                "score": 0.72,
                "metadata": {
                    "source": "visual_text",
                    "role": "ocr_region",
                    "native_type": "visual_text",
                    "detector": "opencv_morphology",
                    "sequence": index,
                    "candidate_id": f"visual-text-{index:03d}",
                    "note": "Heuristic text region for raster images; OCR remains external.",
                },
            }
        )

    return blocks


def merge_visual_text_blocks(
    visual_text_blocks: List[Dict],
    *,
    page_width: int,
    page_height: int,
) -> List[Dict]:
    """Create OCR-first line/paragraph regions while preserving source candidates."""

    candidates = _visual_text_candidates(visual_text_blocks)
    if len(candidates) < 2:
        return []

    lines = _group_candidates_into_lines(candidates, page_width, page_height)
    paragraphs = _group_lines_into_paragraphs(lines, page_width, page_height)
    merged_blocks: List[Dict] = []

    for sequence, paragraph in enumerate(
        [group for group in paragraphs if _candidate_count(group) > 1],
        start=1,
    ):
        children = [
            candidate
            for line in sorted(paragraph, key=_line_key)
            for candidate in sorted(line["children"], key=lambda item: item["bbox"][0])
        ]
        bbox = _union_boxes([candidate["bbox"] for candidate in children])
        merge_type = "paragraph" if len(paragraph) > 1 else "line"
        child_ids = [candidate["candidate_id"] for candidate in children]
        heights = [candidate["height"] for candidate in children]
        scores = [candidate["score"] for candidate in children]

        merged_blocks.append(
            {
                "type": "Text",
                "bbox": [float(value) for value in bbox],
                "score": round(min(0.96, max(scores) + 0.08), 4),
                "metadata": {
                    "source": "visual_text_merged",
                    "role": "ocr_region",
                    "native_type": "visual_text",
                    "merge_type": merge_type,
                    "sequence": sequence,
                    "children": [],
                    "child_candidate_ids": child_ids,
                    "child_count": len(child_ids),
                    "detector": "opencv_morphology",
                    "merge_strategy": "line_then_paragraph_geometry",
                    "mean_child_height": round(sum(heights) / len(heights), 3),
                    "note": "Merged visual text region preferred for OCR; source candidates are preserved.",
                },
            }
        )

    return merged_blocks


def refine_layout_blocks_with_visual_text(
    detected_blocks: List[Dict],
    visual_text_blocks: List[Dict],
    *,
    page_width: int,
    page_height: int,
) -> None:
    if not settings.reclassify_layout_with_visual_text or not visual_text_blocks:
        return

    for block in detected_blocks:
        if block.get("type") != "Figure":
            continue

        bbox = block.get("bbox", [])
        if len(bbox) != 4:
            continue

        block_area = _area(bbox)
        if block_area <= 0:
            continue

        page_area = float(page_width * page_height)
        area_ratio = block_area / page_area if page_area > 0 else 0.0
        overlap_area = sum(
            _intersection_area(bbox, text_block["bbox"])
            for text_block in visual_text_blocks
        )
        text_coverage = min(1.0, overlap_area / block_area)
        metadata = block.setdefault("metadata", {})
        metadata["visual_text_coverage"] = round(text_coverage, 4)

        if area_ratio >= 0.45:
            if text_coverage >= 0.08:
                metadata["contains_text_regions"] = True
            continue

        if text_coverage >= 0.45:
            metadata["original_type"] = block["type"]
            metadata["classification_refined_by"] = "visual_text"
            block["type"] = "Text"
        elif text_coverage >= 0.15:
            metadata["contains_text_regions"] = True


def _is_text_like_box(
    gray,
    x: int,
    y: int,
    width: int,
    height: int,
    page_width: int,
    page_height: int,
    page_area: float,
) -> bool:
    if width < max(24, page_width * 0.015):
        return False
    if height < max(8, page_height * 0.012):
        return False

    box_area = float(width * height)
    area_ratio = box_area / page_area
    if area_ratio < 0.00025 or area_ratio > 0.35:
        return False

    aspect_ratio = width / float(height)
    if aspect_ratio < 1.05:
        return False

    if 0.72 <= aspect_ratio <= 1.35 and area_ratio >= 0.012 and height >= page_height * 0.09:
        return False

    if area_ratio >= 0.07:
        height_ratio = height / float(page_height)
        if height_ratio >= 0.16 and aspect_ratio < 1.90:
            return False
        if height_ratio > 0.22 or aspect_ratio < 1.70:
            return False
    if height > page_height * 0.42:
        return False

    return _has_text_component_profile(gray, x, y, width, height)


def _has_text_component_profile(
    gray,
    x: int,
    y: int,
    width: int,
    height: int,
) -> bool:
    try:
        import cv2
        import numpy as np
    except ImportError as exc:
        raise RuntimeError("opencv-python is required for visual text detection") from exc

    region = gray[y : y + height, x : x + width]
    if region.size == 0:
        return False

    normalized = cv2.GaussianBlur(region, (3, 3), 0)
    _, dark_mask = cv2.threshold(
        normalized,
        0,
        255,
        cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU,
    )

    horizontal_kernel = cv2.getStructuringElement(
        cv2.MORPH_RECT,
        (max(18, width // 5), 1),
    )
    horizontal_lines = cv2.morphologyEx(
        dark_mask,
        cv2.MORPH_OPEN,
        horizontal_kernel,
        iterations=1,
    )
    horizontal_ratio = float(np.count_nonzero(horizontal_lines)) / float(
        max(1, np.count_nonzero(dark_mask)),
    )
    if horizontal_ratio > 0.42 and height < 70:
        return False

    contours, _ = cv2.findContours(
        dark_mask,
        cv2.RETR_EXTERNAL,
        cv2.CHAIN_APPROX_SIMPLE,
    )

    box_area = float(width * height)
    component_count = 0
    component_area = 0.0
    slim_component_count = 0
    large_component_count = 0
    large_component_area = 0.0
    tall_component_count = 0
    component_heights: List[int] = []

    for contour in contours:
        cx, cy, cw, ch = cv2.boundingRect(contour)
        area = float(cw * ch)
        if area < max(6.0, box_area * 0.00012):
            continue
        if area > box_area * 0.45:
            large_component_count += 1
            large_component_area += area
            continue
        if ch > height * 0.92 and cw > width * 0.45:
            large_component_count += 1
            large_component_area += area
            continue

        component_count += 1
        component_area += area
        component_heights.append(ch)
        aspect = cw / float(max(1, ch))
        if 0.08 <= aspect <= 8.5:
            slim_component_count += 1
        if ch >= max(10, height * 0.28):
            tall_component_count += 1

    ink_ratio = component_area / box_area
    large_component_ratio = large_component_area / box_area
    total_ink_ratio = (component_area + large_component_area) / box_area
    significant_component_count = component_count + large_component_count
    estimated_chars = max(1.0, width / max(1.0, height * 0.65))
    max_component_height = max(component_heights) if component_heights else 0

    if large_component_ratio > 0.18:
        if (
            significant_component_count >= 2
            and width >= height * 1.20
            and 0.08 <= total_ink_ratio <= 0.82
        ):
            return True
        return False

    if height < 52 and tall_component_count < 2 and max_component_height < height * 0.55:
        return False

    if large_component_count > 0 and component_count < 4:
        return False
    if component_count >= 6 and 0.015 <= ink_ratio <= 0.78:
        return True
    if slim_component_count >= 4 and estimated_chars >= 2.0 and 0.01 <= ink_ratio <= 0.82:
        return True

    return False


def _visual_text_candidates(blocks: List[Dict]) -> List[Dict]:
    candidates: List[Dict] = []

    for index, block in enumerate(blocks, start=1):
        metadata = block.setdefault("metadata", {})
        if metadata.get("source") != "visual_text":
            continue

        bbox = block.get("bbox", [])
        if len(bbox) != 4:
            continue

        sequence = int(metadata.get("sequence") or index)
        candidate_id = metadata.setdefault("candidate_id", f"visual-text-{sequence:03d}")
        width = max(0.0, float(bbox[2]) - float(bbox[0]))
        height = max(0.0, float(bbox[3]) - float(bbox[1]))
        if width <= 0 or height <= 0:
            continue

        candidates.append(
            {
                "block": block,
                "bbox": [float(value) for value in bbox],
                "candidate_id": str(candidate_id),
                "sequence": sequence,
                "score": float(block.get("score", 0.0) or 0.0),
                "width": width,
                "height": height,
                "center_y": (float(bbox[1]) + float(bbox[3])) / 2.0,
            }
        )

    return sorted(candidates, key=lambda item: (item["bbox"][1], item["bbox"][0]))


def _group_candidates_into_lines(
    candidates: List[Dict],
    page_width: int,
    page_height: int,
) -> List[Dict]:
    lines: List[Dict] = []

    for candidate in sorted(candidates, key=lambda item: (item["center_y"], item["bbox"][0])):
        matching_lines = [
            line
            for line in lines
            if _same_line(line, candidate, page_width, page_height)
        ]

        if not matching_lines:
            lines.append(_new_line(candidate))
            continue

        target = min(
            matching_lines,
            key=lambda line: abs(_line_center_y(line) - candidate["center_y"]),
        )
        target["children"].append(candidate)
        _refresh_line(target)

    for line in lines:
        line["children"].sort(key=lambda item: item["bbox"][0])
        _refresh_line(line)

    return sorted(lines, key=_line_key)


def _group_lines_into_paragraphs(
    lines: List[Dict],
    page_width: int,
    page_height: int,
) -> List[List[Dict]]:
    paragraphs: List[List[Dict]] = []

    for line in sorted(lines, key=_line_key):
        matches = [
            paragraph
            for paragraph in paragraphs
            if _same_paragraph(paragraph[-1], line, page_width, page_height)
        ]

        if not matches:
            paragraphs.append([line])
            continue

        target = min(
            matches,
            key=lambda paragraph: (
                max(0.0, line["bbox"][1] - paragraph[-1]["bbox"][3]),
                abs(line["bbox"][0] - paragraph[-1]["bbox"][0]),
            ),
        )
        target.append(line)

    return sorted(paragraphs, key=lambda group: _line_key(group[0]))


def _same_line(
    line: Dict,
    candidate: Dict,
    page_width: int,
    page_height: int,
) -> bool:
    line_height = line["median_height"]
    candidate_height = candidate["height"]
    if max(line_height, candidate_height) > _max_mergeable_text_height(page_height):
        return False

    height_ratio = _ratio(line_height, candidate_height)
    if height_ratio > 1.6:
        return False

    center_delta = abs(_line_center_y(line) - candidate["center_y"])
    if center_delta > max(7.0, min(line_height, candidate_height) * 0.48, page_height * 0.006):
        return False

    x_gap = _axis_gap(line["bbox"][0], line["bbox"][2], candidate["bbox"][0], candidate["bbox"][2])
    max_gap = max(18.0, page_width * 0.055, line_height * 2.1)
    return x_gap <= max_gap


def _same_paragraph(
    previous: Dict,
    current: Dict,
    page_width: int,
    page_height: int,
) -> bool:
    if _side_by_side_text_chunks(previous, current, page_width, page_height):
        return True
    if _display_heading_chunk(previous, current, page_width, page_height):
        return True

    previous_height = previous["median_height"]
    current_height = current["median_height"]
    if max(previous_height, current_height) > _max_mergeable_text_height(page_height):
        return False

    if _ratio(previous_height, current_height) > 1.55:
        return False

    vertical_gap = float(current["bbox"][1]) - float(previous["bbox"][3])
    if vertical_gap < -min(previous_height, current_height) * 0.08:
        return False

    median_height = (previous_height + current_height) / 2.0
    max_vertical_gap = min(
        max(16.0, median_height * 1.35),
        max(40.0, page_height * 0.05),
    )
    if vertical_gap > max_vertical_gap:
        return False

    x_start_delta = abs(float(current["bbox"][0]) - float(previous["bbox"][0]))
    max_x_start_delta = min(
        max(18.0, median_height * 1.2, page_width * 0.04),
        max(36.0, page_width * 0.08),
    )
    if x_start_delta > max_x_start_delta:
        return False

    if _looks_like_independent_control(previous, current, page_width):
        return False

    return True


def _side_by_side_text_chunks(
    previous: Dict,
    current: Dict,
    page_width: int,
    page_height: int,
) -> bool:
    previous_box = previous["bbox"]
    current_box = current["bbox"]
    overlap_y = max(0.0, min(previous_box[3], current_box[3]) - max(previous_box[1], current_box[1]))
    smaller_height = max(1.0, min(previous_box[3] - previous_box[1], current_box[3] - current_box[1]))
    if overlap_y / smaller_height < 0.45:
        return False

    x_gap = _axis_gap(previous_box[0], previous_box[2], current_box[0], current_box[2])
    if x_gap > max(26.0, page_width * 0.075):
        return False

    combined_width = max(previous_box[2], current_box[2]) - min(previous_box[0], current_box[0])
    if combined_width < page_width * 0.24:
        return False

    return max(previous["median_height"], current["median_height"]) <= page_height * 0.12


def _display_heading_chunk(
    previous: Dict,
    current: Dict,
    page_width: int,
    page_height: int,
) -> bool:
    previous_box = previous["bbox"]
    current_box = current["bbox"]
    if min(previous_box[1], current_box[1]) > page_height * 0.48:
        return False

    previous_height = previous_box[3] - previous_box[1]
    current_height = current_box[3] - current_box[1]
    if max(previous_height, current_height) > page_height * 0.26:
        return False
    if min(previous_height, current_height) < page_height * 0.055:
        return False

    vertical_gap = max(0.0, max(previous_box[1], current_box[1]) - min(previous_box[3], current_box[3]))
    x_gap = _axis_gap(previous_box[0], previous_box[2], current_box[0], current_box[2])
    x_start_delta = abs(previous_box[0] - current_box[0])

    if vertical_gap <= max(12.0, page_height * 0.012) and x_start_delta <= page_width * 0.16:
        return True
    if vertical_gap <= page_height * 0.03 and x_gap <= page_width * 0.075:
        return True

    return False


def _looks_like_independent_control(
    previous: Dict,
    current: Dict,
    page_width: int,
) -> bool:
    previous_width = float(previous["bbox"][2]) - float(previous["bbox"][0])
    current_width = float(current["bbox"][2]) - float(current["bbox"][0])
    if current_width <= 0 or previous_width <= 0:
        return False

    vertical_gap = max(0.0, float(current["bbox"][1]) - float(previous["bbox"][3]))
    median_height = (previous["median_height"] + current["median_height"]) / 2.0
    narrow_relative_to_previous = previous_width >= current_width * 1.8
    narrow_on_page = current_width <= page_width * 0.32
    separated_enough = vertical_gap >= max(8.0, median_height * 0.55)

    return narrow_relative_to_previous and narrow_on_page and separated_enough


def _max_mergeable_text_height(page_height: int) -> float:
    return max(90.0, page_height * 0.10)


def _new_line(candidate: Dict) -> Dict:
    return {
        "children": [candidate],
        "bbox": list(candidate["bbox"]),
        "median_height": candidate["height"],
    }


def _refresh_line(line: Dict) -> None:
    children = line["children"]
    line["bbox"] = _union_boxes([candidate["bbox"] for candidate in children])
    line["median_height"] = float(median(candidate["height"] for candidate in children))


def _line_center_y(line: Dict) -> float:
    return (float(line["bbox"][1]) + float(line["bbox"][3])) / 2.0


def _line_key(line: Dict) -> Tuple[float, float, float, float]:
    x1, y1, x2, y2 = line["bbox"]
    return (float(y1), float(x1), float(y2), float(x2))


def _candidate_count(lines: Sequence[Dict]) -> int:
    return sum(len(line["children"]) for line in lines)


def _union_boxes(boxes: Sequence[Sequence[float]]) -> List[float]:
    return [
        min(float(box[0]) for box in boxes),
        min(float(box[1]) for box in boxes),
        max(float(box[2]) for box in boxes),
        max(float(box[3]) for box in boxes),
    ]


def _axis_gap(a1: float, a2: float, b1: float, b2: float) -> float:
    return max(0.0, max(float(a1), float(b1)) - min(float(a2), float(b2)))


def _ratio(a: float, b: float) -> float:
    smaller = max(1.0, min(float(a), float(b)))
    larger = max(float(a), float(b))
    return larger / smaller


def _pad_box(
    x: int,
    y: int,
    width: int,
    height: int,
    page_width: int,
    page_height: int,
) -> Tuple[int, int, int, int]:
    pad_x = max(3, int(width * 0.025))
    pad_y = max(3, int(height * 0.12))
    return (
        max(0, x - pad_x),
        max(0, y - pad_y),
        min(page_width, x + width + pad_x),
        min(page_height, y + height + pad_y),
    )


def _merge_boxes(
    boxes: List[Tuple[int, int, int, int]],
    page_width: int,
    page_height: int,
) -> List[Tuple[int, int, int, int]]:
    boxes = sorted(boxes, key=lambda box: (box[1], box[0], box[3], box[2]))
    merged: List[Tuple[int, int, int, int]] = []

    for box in boxes:
        if not merged:
            merged.append(box)
            continue

        previous = merged[-1]
        if _should_merge(previous, box, page_width, page_height):
            merged[-1] = (
                min(previous[0], box[0]),
                min(previous[1], box[1]),
                max(previous[2], box[2]),
                max(previous[3], box[3]),
            )
        else:
            merged.append(box)

    return _dedupe_boxes(merged)


def _should_merge(
    a: Tuple[int, int, int, int],
    b: Tuple[int, int, int, int],
    page_width: int,
    page_height: int,
) -> bool:
    overlap = _intersection_area(a, b)
    if overlap > 0:
        smaller = min(_area(a), _area(b))
        if smaller > 0 and overlap / smaller > 0.35:
            return True

    y_gap = max(0, max(a[1], b[1]) - min(a[3], b[3]))
    x_overlap = max(0, min(a[2], b[2]) - max(a[0], b[0]))
    min_width = max(1, min(a[2] - a[0], b[2] - b[0]))
    if y_gap <= max(6, page_height * 0.012) and x_overlap / min_width > 0.55:
        return True

    x_gap = max(0, max(a[0], b[0]) - min(a[2], b[2]))
    y_overlap = max(0, min(a[3], b[3]) - max(a[1], b[1]))
    min_height = max(1, min(a[3] - a[1], b[3] - b[1]))
    if x_gap <= max(10, page_width * 0.012) and y_overlap / min_height > 0.45:
        return True

    return False


def _dedupe_boxes(
    boxes: List[Tuple[int, int, int, int]],
) -> List[Tuple[int, int, int, int]]:
    deduped: List[Tuple[int, int, int, int]] = []
    for box in boxes:
        duplicate = False
        for existing in deduped:
            overlap = _intersection_area(box, existing)
            smaller = min(_area(box), _area(existing))
            if smaller > 0 and overlap / smaller > 0.82:
                duplicate = True
                break
        if not duplicate:
            deduped.append(box)
    return deduped


def _expand_boxes_to_line_ink(
    boxes: List[Tuple[int, int, int, int]],
    ink_mask,
    page_width: int,
    page_height: int,
) -> List[Tuple[int, int, int, int]]:
    expanded: List[Tuple[int, int, int, int]] = []
    for box in boxes:
        x1, y1, x2, y2 = box
        width = max(1, x2 - x1)
        height = max(1, y2 - y1)
        area_ratio = (width * height) / float(page_width * page_height)
        aspect_ratio = width / float(height)
        if area_ratio > 0.12 or height > page_height * 0.14 or 0.72 <= aspect_ratio <= 1.35:
            expanded.append(box)
            continue

        band_pad = max(3, int(height * 0.18))
        band_y1 = max(0, y1 - band_pad)
        band_y2 = min(page_height, y2 + band_pad)
        band = ink_mask[band_y1:band_y2, :]
        if band.size == 0:
            expanded.append(box)
            continue

        threshold = max(2, int((band_y2 - band_y1) * 0.035))
        active_columns = [
            index
            for index, count in enumerate((band > 0).sum(axis=0).tolist())
            if count >= threshold
        ]
        if not active_columns:
            expanded.append(box)
            continue

        clusters = _column_clusters(active_columns, max_gap=max(4, int(height * 0.22)))
        max_join_gap = max(24, int(height * 1.15), int(page_width * 0.035))
        selected = [
            cluster
            for cluster in clusters
            if _axis_gap(cluster[0], cluster[1], x1, x2) <= max_join_gap
        ]
        if not selected:
            expanded.append(box)
            continue

        new_x1 = min(cluster[0] for cluster in selected)
        new_x2 = max(cluster[1] for cluster in selected)
        new_width = new_x2 - new_x1
        if new_width / float(height) > 13.0:
            expanded.append(box)
            continue
        if new_width > max(width * 2.15, width + page_width * 0.28):
            expanded.append(box)
            continue
        if width >= page_width * 0.25 and new_width > width + page_width * 0.18:
            expanded.append(box)
            continue
        if width >= page_width * 0.45 and new_width > width * 1.15:
            expanded.append(box)
            continue

        expanded.append((max(0, new_x1 - 4), y1, min(page_width, new_x2 + 4), y2))
    return expanded


def _column_clusters(columns: List[int], max_gap: int) -> List[Tuple[int, int]]:
    clusters: List[Tuple[int, int]] = []
    start = columns[0]
    previous = columns[0]
    for column in columns[1:]:
        if column - previous > max_gap:
            clusters.append((start, previous + 1))
            start = column
        previous = column
    clusters.append((start, previous + 1))
    return clusters


BoxLike = Union[Tuple[int, int, int, int], List[float]]


def _area(box: BoxLike) -> float:
    return max(0.0, float(box[2]) - float(box[0])) * max(
        0.0,
        float(box[3]) - float(box[1]),
    )


def _intersection_area(
    a: BoxLike,
    b: BoxLike,
) -> float:
    x1 = max(float(a[0]), float(b[0]))
    y1 = max(float(a[1]), float(b[1]))
    x2 = min(float(a[2]), float(b[2]))
    y2 = min(float(a[3]), float(b[3]))
    return max(0.0, x2 - x1) * max(0.0, y2 - y1)
