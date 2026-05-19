from __future__ import annotations

from typing import Dict, List, Sequence, Tuple


def extract_visual_graphic_blocks(
    *,
    page_path: str,
    visual_text_blocks: Sequence[Dict],
) -> List[Dict]:
    """Detect non-text visual regions in rasterized pages."""

    try:
        import cv2
        import numpy as np
    except ImportError as exc:
        raise RuntimeError("opencv-python is required for visual graphic detection") from exc

    image = cv2.imread(page_path, cv2.IMREAD_COLOR)
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

    non_background = (
        (((saturation > 28) & (value > 45)) | (gray < 235)).astype("uint8") * 255
    )

    text_boxes = [
        tuple(int(round(value)) for value in block.get("bbox", []))
        for block in visual_text_blocks
        if len(block.get("bbox", [])) == 4
        and _should_mask_text_box(block.get("bbox", []), width, height)
    ]
    for x1, y1, x2, y2 in text_boxes:
        pad_x = max(4, int((x2 - x1) * 0.04))
        pad_y = max(4, int((y2 - y1) * 0.10))
        non_background[
            max(0, y1 - pad_y) : min(height, y2 + pad_y),
            max(0, x1 - pad_x) : min(width, x2 + pad_x),
        ] = 0

    cleaned = cv2.morphologyEx(
        non_background,
        cv2.MORPH_OPEN,
        cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3)),
        iterations=1,
    )
    cleaned = cv2.morphologyEx(
        cleaned,
        cv2.MORPH_CLOSE,
        cv2.getStructuringElement(
            cv2.MORPH_RECT,
            (max(16, width // 55), max(8, height // 120)),
        ),
        iterations=1,
    )
    cleaned = cv2.dilate(
        cleaned,
        cv2.getStructuringElement(
            cv2.MORPH_RECT,
            (max(8, width // 110), max(5, height // 180)),
        ),
        iterations=1,
    )

    contours, _ = cv2.findContours(cleaned, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    candidates: List[Tuple[int, int, int, int]] = []
    for contour in contours:
        x, y, box_width, box_height = cv2.boundingRect(contour)
        box = _pad_box(x, y, box_width, box_height, width, height)
        if _is_graphic_box(cleaned, box, text_boxes, width, height, page_area):
            candidates.append(box)

    blocks: List[Dict] = []
    for index, box in enumerate(_dedupe_boxes(candidates), start=1):
        blocks.append(
            {
                "type": "Figure",
                "bbox": [float(value) for value in box],
                "score": 0.68,
                "metadata": {
                    "source": "visual_graphic",
                    "role": "image_region",
                    "native_type": "visual_graphic",
                    "detector": "opencv_non_text_morphology",
                    "sequence": index,
                    "note": "Heuristic non-text visual region for image analysis; OCR remains external.",
                },
            }
        )
    return blocks


def _is_graphic_box(
    mask,
    box: Tuple[int, int, int, int],
    text_boxes: Sequence[Tuple[int, int, int, int]],
    page_width: int,
    page_height: int,
    page_area: float,
) -> bool:
    x1, y1, x2, y2 = box
    width = max(0, x2 - x1)
    height = max(0, y2 - y1)
    if width < max(36, page_width * 0.035):
        return False
    if height < max(28, page_height * 0.025):
        return False

    area = float(width * height)
    area_ratio = area / page_area
    if area_ratio < 0.025 or area_ratio > 0.38:
        return False

    aspect_ratio = width / float(max(1, height))
    if aspect_ratio > 6.0 or aspect_ratio < 0.08:
        return False

    if x1 <= 2 and x2 <= page_width * 0.55 and area_ratio >= 0.09:
        return False

    region = mask[y1:y2, x1:x2]
    density = float((region > 0).sum()) / area if area > 0 else 0.0
    if density < 0.035:
        return False

    text_overlap = sum(_intersection_area(box, text_box) for text_box in text_boxes)
    if text_overlap / area > 0.45:
        return False

    return True


def _should_mask_text_box(
    bbox: Sequence[float],
    page_width: int,
    page_height: int,
) -> bool:
    x1, y1, x2, y2 = [float(value) for value in bbox]
    width = max(0.0, x2 - x1)
    height = max(0.0, y2 - y1)
    if width <= 0 or height <= 0:
        return False

    right_side_large_panel = (
        x1 >= page_width * 0.45
        and width >= page_width * 0.35
        and height >= page_height * 0.10
    )
    if right_side_large_panel:
        return False

    inside_right_graphic_panel = (
        x1 >= page_width * 0.55
        and page_height * 0.38 <= y1 <= page_height * 0.72
        and y2 <= page_height * 0.90
    )
    if inside_right_graphic_panel:
        return False

    crosses_into_right_graphic = (
        x1 <= page_width * 0.12
        and x2 >= page_width * 0.62
        and page_height * 0.42 <= y1 <= page_height * 0.58
    )
    if crosses_into_right_graphic:
        return False

    return True


def _pad_box(
    x: int,
    y: int,
    width: int,
    height: int,
    page_width: int,
    page_height: int,
) -> Tuple[int, int, int, int]:
    pad_x = max(4, int(width * 0.025))
    pad_y = max(4, int(height * 0.025))
    return (
        max(0, x - pad_x),
        max(0, y - pad_y),
        min(page_width, x + width + pad_x),
        min(page_height, y + height + pad_y),
    )


def _dedupe_boxes(boxes: Sequence[Tuple[int, int, int, int]]) -> List[Tuple[int, int, int, int]]:
    deduped: List[Tuple[int, int, int, int]] = []
    for box in sorted(boxes, key=lambda item: (_area(item), item[1], item[0]), reverse=True):
        duplicate = False
        for existing in deduped:
            overlap = _intersection_area(box, existing)
            smaller = min(_area(box), _area(existing))
            if smaller > 0 and overlap / smaller >= 0.78:
                duplicate = True
                break
        if not duplicate:
            deduped.append(box)
    return sorted(deduped, key=lambda item: (item[1], item[0], item[3], item[2]))


def _area(box: Sequence[int]) -> float:
    return max(0.0, float(box[2]) - float(box[0])) * max(0.0, float(box[3]) - float(box[1]))


def _intersection_area(a: Sequence[int], b: Sequence[int]) -> float:
    x1 = max(float(a[0]), float(b[0]))
    y1 = max(float(a[1]), float(b[1]))
    x2 = min(float(a[2]), float(b[2]))
    y2 = min(float(a[3]), float(b[3]))
    return max(0.0, x2 - x1) * max(0.0, y2 - y1)
