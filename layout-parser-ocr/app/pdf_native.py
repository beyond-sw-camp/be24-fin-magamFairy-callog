from __future__ import annotations

from pathlib import Path
from typing import TYPE_CHECKING, Dict, List, Tuple

from app.settings import settings

if TYPE_CHECKING:
    from app.document import PageImage


def extract_pdf_native_blocks(
    source_path: Path,
    page_images: List[PageImage],
) -> Dict[int, List[Dict]]:
    if not settings.include_pdf_native_blocks or source_path.suffix.lower() != ".pdf":
        return {}

    try:
        import fitz
    except ImportError as exc:
        raise RuntimeError("PyMuPDF is required for PDF-native block extraction") from exc

    native_blocks: Dict[int, List[Dict]] = {}

    with fitz.open(source_path) as document:
        for index, page_image in enumerate(page_images):
            if index >= document.page_count:
                break

            page = document.load_page(index)
            scale_x = page_image.width / float(page.rect.width)
            scale_y = page_image.height / float(page.rect.height)
            blocks: List[Dict] = []

            for block in page.get_text("dict").get("blocks", []):
                block_type = block.get("type")
                bbox = block.get("bbox")
                if not bbox:
                    continue

                if block_type == 0:
                    text = _text_from_block(block)
                    if not text.strip():
                        continue
                    blocks.append(
                        {
                            "type": "Text",
                            "bbox": _scale_bbox(bbox, scale_x, scale_y),
                            "score": 1.0,
                            "text": text,
                            "metadata": {
                                "source": "pdf_native",
                                "native_type": "text",
                                "role": "embedded_text",
                                "roles": ["embedded_text"],
                                "has_pdf_text": True,
                            },
                        }
                    )
                elif block_type == 1:
                    blocks.append(
                        {
                            "type": "Figure",
                            "bbox": _scale_bbox(bbox, scale_x, scale_y),
                            "score": 1.0,
                            "metadata": {
                                "source": "pdf_native",
                                "native_type": "image",
                                "role": "image_region",
                                "width": block.get("width"),
                                "height": block.get("height"),
                                "ext": block.get("ext"),
                            },
                        }
                    )

            native_blocks[page_image.page_number] = blocks

    return native_blocks


def _text_from_block(block: Dict) -> str:
    lines: List[str] = []
    for line in block.get("lines", []):
        spans = line.get("spans", [])
        line_text = "".join(span.get("text", "") for span in spans).strip()
        if line_text:
            lines.append(line_text)
    return "\n".join(lines)


def _scale_bbox(
    bbox: Tuple[float, float, float, float],
    scale_x: float,
    scale_y: float,
) -> List[float]:
    x1, y1, x2, y2 = bbox
    return [
        float(x1) * scale_x,
        float(y1) * scale_y,
        float(x2) * scale_x,
        float(y2) * scale_y,
    ]
