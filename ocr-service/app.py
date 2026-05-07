import os
import shutil
import tempfile
from functools import lru_cache
from pathlib import Path
from typing import Any

import fitz
from fastapi import FastAPI, File, HTTPException, UploadFile
from paddleocr import PaddleOCR
from PIL import Image, ImageEnhance, ImageFilter, ImageOps
from pydantic import BaseModel
from starlette.concurrency import run_in_threadpool


OCR_LANG = os.getenv("OCR_LANG", "korean")
OCR_MAX_PDF_PAGES = int(os.getenv("OCR_MAX_PDF_PAGES", "10"))
OCR_MAX_UPLOAD_MB = int(os.getenv("OCR_MAX_UPLOAD_MB", "20"))
OCR_USE_GPU = os.getenv("OCR_USE_GPU", "false").lower() == "true"
OCR_PREPROCESS = os.getenv("OCR_PREPROCESS", "true").lower() == "true"
OCR_TARGET_LONG_EDGE = int(os.getenv("OCR_TARGET_LONG_EDGE", "2400"))
OCR_CONTRAST = float(os.getenv("OCR_CONTRAST", "1.8"))
OCR_SHARPNESS = float(os.getenv("OCR_SHARPNESS", "1.6"))
PDF_RENDER_SCALE = float(os.getenv("OCR_PDF_RENDER_SCALE", "3"))


class OcrLine(BaseModel):
    text: str
    score: float | None = None


class OcrResponse(BaseModel):
    text: str
    lines: list[OcrLine]
    pageCount: int


app = FastAPI(title="Callog OCR Service")


@lru_cache(maxsize=1)
def get_ocr() -> PaddleOCR:
    return PaddleOCR(
        use_angle_cls=True,
        lang=OCR_LANG,
        use_gpu=OCR_USE_GPU,
        show_log=False,
    )


@app.on_event("startup")
def warm_up_model() -> None:
    get_ocr()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/ocr", response_model=OcrResponse)
async def extract_text(file: UploadFile = File(...)) -> OcrResponse:
    content = await file.read()
    max_bytes = OCR_MAX_UPLOAD_MB * 1024 * 1024
    if len(content) > max_bytes:
        raise HTTPException(status_code=413, detail="File is too large")

    suffix = Path(file.filename or "").suffix.lower()
    if not suffix:
        suffix = _suffix_from_content_type(file.content_type or "")

    temp_dir = Path(tempfile.mkdtemp(prefix="callog-ocr-"))
    try:
        upload_path = temp_dir / f"upload{suffix}"
        upload_path.write_bytes(content)
        return await run_in_threadpool(_extract_from_path, upload_path, file.content_type or "", temp_dir)
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)


def _extract_from_path(path: Path, content_type: str, temp_dir: Path) -> OcrResponse:
    if _is_pdf(path, content_type):
        return _extract_from_pdf(path, temp_dir)

    if _is_image(path, content_type):
        lines = _extract_from_image(path, temp_dir)
        return _response_from_pages([lines])

    raise HTTPException(status_code=415, detail="Unsupported file type")


def _extract_from_pdf(path: Path, temp_dir: Path) -> OcrResponse:
    pages: list[list[OcrLine]] = []

    with fitz.open(path) as document:
        page_count = min(len(document), OCR_MAX_PDF_PAGES)
        for page_index in range(page_count):
            page = document[page_index]
            pixmap = page.get_pixmap(matrix=fitz.Matrix(PDF_RENDER_SCALE, PDF_RENDER_SCALE), alpha=False)
            image_path = temp_dir / f"page-{page_index + 1}.png"
            pixmap.save(image_path)
            pages.append(_extract_from_image(image_path, temp_dir))

    return _response_from_pages(pages)


def _extract_from_image(path: Path, temp_dir: Path) -> list[OcrLine]:
    ocr_path = _prepare_image_for_ocr(path, temp_dir)
    result = get_ocr().ocr(str(ocr_path), cls=True)
    return _collect_lines(result)


def _prepare_image_for_ocr(path: Path, temp_dir: Path) -> Path:
    if not OCR_PREPROCESS:
        return path

    try:
        with Image.open(path) as source:
            image = ImageOps.exif_transpose(source).convert("RGB")

            width, height = image.size
            longest_edge = max(width, height)
            if longest_edge < OCR_TARGET_LONG_EDGE:
                scale = OCR_TARGET_LONG_EDGE / longest_edge
                image = image.resize(
                    (int(width * scale), int(height * scale)),
                    Image.Resampling.LANCZOS,
                )

            image = ImageOps.grayscale(image)
            image = ImageOps.autocontrast(image)
            image = ImageEnhance.Contrast(image).enhance(OCR_CONTRAST)
            image = ImageEnhance.Sharpness(image).enhance(OCR_SHARPNESS)
            image = image.filter(ImageFilter.SHARPEN)

            output_path = temp_dir / f"{path.stem}-preprocessed.png"
            image.save(output_path)
            return output_path
    except Exception:
        return path


def _collect_lines(result: Any) -> list[OcrLine]:
    lines: list[OcrLine] = []

    def walk(node: Any) -> None:
        if node is None:
            return

        if isinstance(node, dict):
            rec_texts = node.get("rec_texts") or node.get("res", {}).get("rec_texts")
            rec_scores = node.get("rec_scores") or node.get("res", {}).get("rec_scores") or []
            if rec_texts:
                for index, text in enumerate(rec_texts):
                    lines.append(OcrLine(text=str(text), score=_score_at(rec_scores, index)))
                return

            for value in node.values():
                walk(value)
            return

        if isinstance(node, (list, tuple)):
            if len(node) >= 2 and isinstance(node[1], (list, tuple)):
                candidate = node[1]
                if candidate and isinstance(candidate[0], str):
                    lines.append(OcrLine(text=candidate[0], score=_to_float(candidate[1] if len(candidate) > 1 else None)))
                    return

            for value in node:
                walk(value)

    walk(result)
    return lines


def _response_from_pages(pages: list[list[OcrLine]]) -> OcrResponse:
    all_lines = [line for page in pages for line in page if line.text.strip()]
    page_texts = ["\n".join(line.text for line in page if line.text.strip()) for page in pages]
    text = "\n\n".join(page_text for page_text in page_texts if page_text.strip())
    return OcrResponse(text=text, lines=all_lines, pageCount=len(pages))


def _score_at(scores: Any, index: int) -> float | None:
    try:
        return _to_float(scores[index])
    except Exception:
        return None


def _to_float(value: Any) -> float | None:
    try:
        return float(value)
    except Exception:
        return None


def _is_pdf(path: Path, content_type: str) -> bool:
    return content_type == "application/pdf" or path.suffix.lower() == ".pdf"


def _is_image(path: Path, content_type: str) -> bool:
    return content_type.startswith("image/") or path.suffix.lower() in {
        ".bmp",
        ".jpeg",
        ".jpg",
        ".png",
        ".tif",
        ".tiff",
        ".webp",
    }


def _suffix_from_content_type(content_type: str) -> str:
    return {
        "application/pdf": ".pdf",
        "image/bmp": ".bmp",
        "image/jpeg": ".jpg",
        "image/png": ".png",
        "image/tiff": ".tiff",
        "image/webp": ".webp",
    }.get(content_type, "")
