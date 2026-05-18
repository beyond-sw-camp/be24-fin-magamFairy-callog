from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import List

from fastapi import HTTPException
from PIL import Image

from app.settings import settings


SUPPORTED_IMAGE_SUFFIXES = {".png", ".jpg", ".jpeg", ".tif", ".tiff", ".bmp", ".webp"}


@dataclass(frozen=True)
class PageImage:
    page_number: int
    path: Path
    width: int
    height: int


def convert_to_page_images(source_path: Path, output_dir: Path) -> List[PageImage]:
    output_dir.mkdir(parents=True, exist_ok=True)
    suffix = source_path.suffix.lower()

    if suffix == ".pdf":
        return _render_pdf(source_path, output_dir)

    if suffix in SUPPORTED_IMAGE_SUFFIXES:
        return [_normalize_image(source_path, output_dir / "page-001.png", 1)]

    raise HTTPException(
        status_code=415,
        detail="Unsupported file type. Upload a PDF or image file.",
    )


def _render_pdf(source_path: Path, output_dir: Path) -> List[PageImage]:
    try:
        import fitz
    except ImportError as exc:
        raise RuntimeError("PyMuPDF is required for PDF rendering") from exc

    pages: List[PageImage] = []
    zoom = settings.render_dpi / 72
    matrix = fitz.Matrix(zoom, zoom)

    with fitz.open(source_path) as document:
        if document.page_count == 0:
            raise HTTPException(status_code=400, detail="PDF has no pages")

        for index in range(document.page_count):
            page = document.load_page(index)
            pixmap = page.get_pixmap(matrix=matrix, alpha=False)
            page_path = output_dir / f"page-{index + 1:03d}.png"
            pixmap.save(page_path)
            pages.append(
                PageImage(
                    page_number=index + 1,
                    path=page_path,
                    width=pixmap.width,
                    height=pixmap.height,
                )
            )

    return pages


def _normalize_image(source_path: Path, target_path: Path, page_number: int) -> PageImage:
    try:
        with Image.open(source_path) as image:
            normalized = image.convert("RGB")
            normalized.save(target_path, format="PNG")
            return PageImage(
                page_number=page_number,
                path=target_path,
                width=normalized.width,
                height=normalized.height,
            )
    except OSError as exc:
        raise HTTPException(status_code=400, detail="Invalid image file") from exc
