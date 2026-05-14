import os
import logging
import shutil
import tempfile
import time
import uuid
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from threading import Lock
from typing import Any

OCR_LANG = os.getenv("OCR_LANG", "korean")
OCR_MAX_PDF_PAGES = int(os.getenv("OCR_MAX_PDF_PAGES", "10"))
OCR_MAX_UPLOAD_MB = int(os.getenv("OCR_MAX_UPLOAD_MB", "20"))
OCR_USE_GPU = os.getenv("OCR_USE_GPU", "false").lower() == "true"
OCR_USE_ANGLE_CLS = os.getenv("OCR_USE_ANGLE_CLS", "false").lower() == "true"
OCR_ENABLE_MKLDNN = os.getenv("OCR_ENABLE_MKLDNN", "false").lower() == "true"
OCR_CPU_THREADS = int(os.getenv("OCR_CPU_THREADS", "1"))
OCR_TEMP_ROOT = os.getenv("OCR_TEMP_ROOT", "").strip()
OCR_PREPROCESS = os.getenv("OCR_PREPROCESS", "true").lower() == "true"
OCR_TARGET_LONG_EDGE = int(os.getenv("OCR_TARGET_LONG_EDGE", "1600"))
OCR_DET_LIMIT_SIDE_LEN = int(os.getenv("OCR_DET_LIMIT_SIDE_LEN", str(OCR_TARGET_LONG_EDGE)))
OCR_CONTRAST = float(os.getenv("OCR_CONTRAST", "1.8"))
OCR_SHARPNESS = float(os.getenv("OCR_SHARPNESS", "1.6"))
OCR_SOFT_CONTRAST = float(os.getenv("OCR_SOFT_CONTRAST", "1.18"))
OCR_SOFT_SHARPNESS = float(os.getenv("OCR_SOFT_SHARPNESS", "1.08"))
OCR_MIN_SCORE = float(os.getenv("OCR_MIN_SCORE", "0.42"))
OCR_RETRY_ATTEMPTS = int(os.getenv("OCR_RETRY_ATTEMPTS", "3"))
PDF_RENDER_SCALE = float(os.getenv("OCR_PDF_RENDER_SCALE", "3"))

os.environ["FLAGS_use_mkldnn"] = "1" if OCR_ENABLE_MKLDNN else "0"
os.environ["OMP_NUM_THREADS"] = str(OCR_CPU_THREADS)
os.environ["MKL_NUM_THREADS"] = str(OCR_CPU_THREADS)
os.environ["OPENBLAS_NUM_THREADS"] = str(OCR_CPU_THREADS)
os.environ["NUMEXPR_NUM_THREADS"] = str(OCR_CPU_THREADS)

import fitz
from fastapi import FastAPI, File, HTTPException, UploadFile
from paddleocr import PaddleOCR
from PIL import Image, ImageEnhance, ImageFilter, ImageOps
from pydantic import BaseModel
from starlette.concurrency import run_in_threadpool

logger = logging.getLogger("uvicorn.error")
ocr_lock = Lock()
TRANSIENT_OCR_ERROR_MARKERS = (
    "could not execute a primitive",
    "onednn",
    "mkldnn",
    "dnnl",
    "resource exhausted",
    "bad allocation",
    "std::bad_alloc",
)


class OcrLine(BaseModel):
    text: str
    score: float | None = None
    x: float | None = None
    y: float | None = None
    width: float | None = None
    height: float | None = None


class OcrResponse(BaseModel):
    text: str
    lines: list[OcrLine]
    pageCount: int


@dataclass
class RawOcrItem:
    text: str
    score: float | None = None
    x: float | None = None
    y: float | None = None
    width: float | None = None
    height: float | None = None

    @property
    def has_box(self) -> bool:
        return self.x is not None and self.y is not None and self.width is not None and self.height is not None

    @property
    def center_y(self) -> float:
        return float(self.y or 0) + float(self.height or 0) / 2

    @property
    def right(self) -> float:
        return float(self.x or 0) + float(self.width or 0)


@dataclass
class CandidateResult:
    path: Path
    lines: list[OcrLine]
    score: float


app = FastAPI(title="Callog OCR Service")


@lru_cache(maxsize=1)
def get_ocr() -> PaddleOCR:
    return PaddleOCR(
        use_angle_cls=OCR_USE_ANGLE_CLS,
        lang=OCR_LANG,
        use_gpu=OCR_USE_GPU,
        enable_mkldnn=OCR_ENABLE_MKLDNN,
        cpu_threads=OCR_CPU_THREADS,
        det_limit_side_len=OCR_DET_LIMIT_SIDE_LEN,
        use_space_char=True,
        show_log=False,
    )


@app.on_event("startup")
def warm_up_model() -> None:
    get_ocr()
    logger.info(
        "OCR model warmed up. lang=%s gpu=%s angle_cls=%s mkldnn=%s cpu_threads=%s target_long_edge=%s det_limit_side_len=%s",
        OCR_LANG,
        OCR_USE_GPU,
        OCR_USE_ANGLE_CLS,
        OCR_ENABLE_MKLDNN,
        OCR_CPU_THREADS,
        OCR_TARGET_LONG_EDGE,
        OCR_DET_LIMIT_SIDE_LEN,
    )


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/ocr", response_model=OcrResponse)
async def extract_text(file: UploadFile = File(...)) -> OcrResponse:
    request_id = uuid.uuid4().hex[:8]
    start_time = time.perf_counter()
    content = await file.read()
    max_bytes = OCR_MAX_UPLOAD_MB * 1024 * 1024
    if len(content) > max_bytes:
        raise HTTPException(status_code=413, detail="File is too large")

    suffix = Path(file.filename or "").suffix.lower()
    if not suffix:
        suffix = _suffix_from_content_type(file.content_type or "")

    temp_dir = _make_temp_dir()
    try:
        logger.info(
            "OCR request started. id=%s fileName=%s contentType=%s size=%s",
            request_id,
            file.filename,
            file.content_type,
            len(content),
        )
        upload_path = temp_dir / f"upload{suffix}"
        upload_path.write_bytes(content)
        response = await run_in_threadpool(_extract_from_path, upload_path, file.content_type or "", temp_dir)
        logger.info(
            "OCR request completed. id=%s textLength=%s pageCount=%s elapsedMs=%s",
            request_id,
            len(response.text or ""),
            response.pageCount,
            int((time.perf_counter() - start_time) * 1000),
        )
        return response
    except HTTPException:
        logger.exception("OCR request failed. id=%s fileName=%s", request_id, file.filename)
        raise
    except Exception as exc:
        logger.exception("OCR request crashed. id=%s fileName=%s", request_id, file.filename)
        raise HTTPException(status_code=500, detail=f"OCR service internal error: {type(exc).__name__}: {exc}") from exc
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)


def _extract_from_path(path: Path, content_type: str, temp_dir: Path) -> OcrResponse:
    if _is_pdf(path, content_type):
        return _extract_from_pdf(path, temp_dir)

    if _is_image(path, content_type):
        lines = _extract_from_image(path, temp_dir)
        return _response_from_pages([lines])

    raise HTTPException(status_code=415, detail="Unsupported file type")


def _make_temp_dir() -> Path:
    roots: list[Path] = []
    if OCR_TEMP_ROOT:
        roots.append(Path(OCR_TEMP_ROOT))
    roots.extend([Path(tempfile.gettempdir()), Path.cwd() / ".callog-ocr-tmp"])

    for root in roots:
        temp_dir: Path | None = None
        try:
            root.mkdir(parents=True, exist_ok=True)
            temp_dir = root / f"callog-ocr-{uuid.uuid4().hex}"
            temp_dir.mkdir()
            probe_path = temp_dir / ".write-check"
            probe_path.write_bytes(b"")
            probe_path.unlink(missing_ok=True)
            return temp_dir
        except Exception:
            if temp_dir is not None:
                shutil.rmtree(temp_dir, ignore_errors=True)

    raise HTTPException(status_code=500, detail="OCR temporary directory is not writable")


def _extract_from_pdf(path: Path, temp_dir: Path) -> OcrResponse:
    pages: list[list[OcrLine]] = []
    page_errors: list[Exception] = []

    with fitz.open(path) as document:
        page_count = min(len(document), OCR_MAX_PDF_PAGES)
        for page_index in range(page_count):
            page = document[page_index]
            pixmap = page.get_pixmap(matrix=fitz.Matrix(PDF_RENDER_SCALE, PDF_RENDER_SCALE), alpha=False)
            image_path = temp_dir / f"page-{page_index + 1}.png"
            pixmap.save(image_path)
            try:
                pages.append(_extract_from_image(image_path, temp_dir))
            except Exception as exc:
                page_errors.append(exc)
                logger.exception("OCR PDF page failed. path=%s page=%s", path, page_index + 1)
                pages.append([])

    if page_errors and not any(page for page in pages):
        latest_error = page_errors[-1]
        if isinstance(latest_error, HTTPException):
            raise latest_error
        raise HTTPException(
            status_code=500,
            detail=f"OCR engine failed: {type(latest_error).__name__}: {latest_error}",
        )

    return _response_from_pages(pages)


def _extract_from_image(path: Path, temp_dir: Path) -> list[OcrLine]:
    candidates = _prepare_image_candidates(path, temp_dir)
    best: CandidateResult | None = None
    errors: list[Exception] = []

    for candidate_path in candidates:
        try:
            result = _run_ocr(candidate_path)
            raw_items = _collect_raw_items(result)
            lines = _layout_lines(raw_items)
            score = _score_candidate(lines)
            if best is None or score > best.score:
                best = CandidateResult(path=candidate_path, lines=lines, score=score)
        except Exception as exc:
            errors.append(exc)
            logger.exception("OCR candidate failed. path=%s", candidate_path)
            continue

    if best is None and errors:
        latest_error = errors[-1]
        raise HTTPException(
            status_code=500,
            detail=f"OCR engine failed: {type(latest_error).__name__}: {latest_error}",
        )

    if best is not None and not best.lines:
        logger.warning("OCR completed but extracted no text. path=%s candidates=%s", path, len(candidates))

    return best.lines if best is not None else []


def _run_ocr(path: Path) -> Any:
    attempts = max(1, OCR_RETRY_ATTEMPTS)
    latest_error: Exception | None = None

    for attempt in range(1, attempts + 1):
        try:
            with ocr_lock:
                return get_ocr().ocr(str(path), cls=OCR_USE_ANGLE_CLS)
        except Exception as exc:
            latest_error = exc
            if not _is_transient_ocr_error(exc) or attempt >= attempts:
                raise
            logger.warning(
                "OCR engine transient failure. resetting model and retrying attempt=%s/%s path=%s error=%s",
                attempt,
                attempts,
                path,
                exc,
            )
            with ocr_lock:
                get_ocr.cache_clear()
            time.sleep(0.35 * attempt)

    if latest_error is not None:
        raise latest_error
    raise RuntimeError("OCR execution failed")


def _is_transient_ocr_error(exc: Exception) -> bool:
    message = f"{type(exc).__name__}: {exc}".lower()
    return any(marker in message for marker in TRANSIENT_OCR_ERROR_MARKERS)


def _prepare_image_candidates(path: Path, temp_dir: Path) -> list[Path]:
    if not OCR_PREPROCESS:
        return [path]

    try:
        with Image.open(path) as source:
            base = ImageOps.exif_transpose(source).convert("RGB")
            base = _resize_for_ocr(base)

            rgb_path = _save_candidate(base, temp_dir, path.stem, "rgb")

            soft_gray = ImageOps.grayscale(base)
            soft_gray = ImageOps.autocontrast(soft_gray, cutoff=1)
            soft_gray = ImageEnhance.Contrast(soft_gray).enhance(OCR_SOFT_CONTRAST)
            soft_gray = ImageEnhance.Sharpness(soft_gray).enhance(OCR_SOFT_SHARPNESS)
            soft_path = _save_candidate(soft_gray, temp_dir, path.stem, "soft")

            high_gray = ImageOps.grayscale(base)
            high_gray = ImageOps.autocontrast(high_gray)
            high_gray = ImageEnhance.Contrast(high_gray).enhance(OCR_CONTRAST)
            high_gray = ImageEnhance.Sharpness(high_gray).enhance(OCR_SHARPNESS)
            high_gray = high_gray.filter(ImageFilter.SHARPEN)
            high_path = _save_candidate(high_gray, temp_dir, path.stem, "high")

            return [rgb_path, soft_path, high_path]
    except Exception:
        logger.exception("OCR image preprocessing failed. path=%s", path)
        return [path]


def _resize_for_ocr(image: Image.Image) -> Image.Image:
    if OCR_TARGET_LONG_EDGE <= 0:
        return image

    width, height = image.size
    longest_edge = max(width, height)
    if longest_edge == OCR_TARGET_LONG_EDGE:
        return image

    scale = OCR_TARGET_LONG_EDGE / longest_edge
    return image.resize(
        (max(1, int(width * scale)), max(1, int(height * scale))),
        Image.Resampling.LANCZOS,
    )


def _save_candidate(image: Image.Image, temp_dir: Path, stem: str, suffix: str) -> Path:
    output_path = temp_dir / f"{stem}-{suffix}.png"
    image.save(output_path)
    return output_path


def _collect_raw_items(result: Any) -> list[RawOcrItem]:
    items: list[RawOcrItem] = []

    def add(text: Any, score: Any = None, box: Any = None) -> None:
        normalized_text = str(text or "").strip()
        if not normalized_text:
            return
        x, y, width, height = _normalize_box(box)
        items.append(RawOcrItem(
            text=normalized_text,
            score=_to_float(score),
            x=x,
            y=y,
            width=width,
            height=height,
        ))

    def walk(node: Any) -> None:
        if node is None:
            return

        if isinstance(node, dict):
            rec_texts = node.get("rec_texts") or node.get("res", {}).get("rec_texts")
            rec_scores = node.get("rec_scores") or node.get("res", {}).get("rec_scores") or []
            rec_boxes = (
                node.get("rec_boxes")
                or node.get("rec_polys")
                or node.get("dt_polys")
                or node.get("res", {}).get("rec_boxes")
                or node.get("res", {}).get("rec_polys")
                or node.get("res", {}).get("dt_polys")
                or []
            )
            if rec_texts:
                for index, text in enumerate(rec_texts):
                    add(text, _at(rec_scores, index), _at(rec_boxes, index))
                return

            for value in node.values():
                walk(value)
            return

        if isinstance(node, (list, tuple)):
            if len(node) >= 2:
                box = node[0]
                payload = node[1]
                if isinstance(payload, (list, tuple)) and payload and isinstance(payload[0], str):
                    add(payload[0], payload[1] if len(payload) > 1 else None, box)
                    return

            for value in node:
                walk(value)

    walk(result)
    return items


def _layout_lines(items: list[RawOcrItem]) -> list[OcrLine]:
    clean_items = [item for item in items if item.text.strip()]
    if not clean_items:
        return []

    filtered_items = _filter_low_confidence(clean_items)
    boxed_items = [item for item in filtered_items if item.has_box]
    if len(boxed_items) < max(2, len(filtered_items) // 3):
        return [
            OcrLine(text=item.text.strip(), score=item.score, x=item.x, y=item.y, width=item.width, height=item.height)
            for item in filtered_items
        ]

    line_groups: list[list[RawOcrItem]] = []
    for item in sorted(boxed_items, key=lambda value: (value.center_y, value.x or 0)):
        target_group = _find_line_group(line_groups, item)
        if target_group is None:
            line_groups.append([item])
        else:
            target_group.append(item)

    lines: list[OcrLine] = []
    for group in sorted(line_groups, key=lambda value: _group_center_y(value)):
        text = _join_line_group(group)
        if not text:
            continue
        score = _mean([item.score for item in group if item.score is not None])
        x, y, width, height = _group_box(group)
        lines.append(OcrLine(text=text, score=score, x=x, y=y, width=width, height=height))

    return lines


def _filter_low_confidence(items: list[RawOcrItem]) -> list[RawOcrItem]:
    filtered = [
        item for item in items
        if item.score is None or item.score >= OCR_MIN_SCORE or len(item.text.strip()) >= 4
    ]
    if len(filtered) < max(3, int(len(items) * 0.35)):
        return items
    return filtered


def _find_line_group(line_groups: list[list[RawOcrItem]], item: RawOcrItem) -> list[RawOcrItem] | None:
    for group in reversed(line_groups):
        group_height = max(_mean([entry.height for entry in group if entry.height is not None]) or 0, item.height or 0, 1)
        tolerance = max(8.0, group_height * 0.58)
        if abs(item.center_y - _group_center_y(group)) <= tolerance:
            return group
    return None


def _join_line_group(group: list[RawOcrItem]) -> str:
    ordered = sorted(group, key=lambda item: item.x or 0)
    parts: list[str] = []
    previous: RawOcrItem | None = None

    for item in ordered:
        text = item.text.strip()
        if not text:
            continue

        if previous is not None:
            gap = (item.x or 0) - previous.right
            reference_height = max(previous.height or 0, item.height or 0, 1)
            if gap > reference_height * 0.12:
                parts.append(" ")

        parts.append(text)
        previous = item

    return _normalize_spacing("".join(parts))


def _response_from_pages(pages: list[list[OcrLine]]) -> OcrResponse:
    all_lines = [line for page in pages for line in page if line.text.strip()]
    page_texts = [_page_text(page) for page in pages]
    text = "\n\n".join(page_text for page_text in page_texts if page_text.strip())
    return OcrResponse(text=text, lines=all_lines, pageCount=len(pages))


def _page_text(lines: list[OcrLine]) -> str:
    visible_lines = [line for line in lines if line.text.strip()]
    if not visible_lines:
        return ""

    page_parts: list[str] = []
    previous: OcrLine | None = None
    for line in visible_lines:
        if previous is not None and _should_start_paragraph(previous, line):
            page_parts.append("")
        page_parts.append(line.text)
        previous = line

    return "\n".join(page_parts)


def _should_start_paragraph(previous: OcrLine, current: OcrLine) -> bool:
    if previous.y is None or previous.height is None or current.y is None or current.height is None:
        return False

    vertical_gap = current.y - (previous.y + previous.height)
    reference_height = max(previous.height, current.height, 1)
    return vertical_gap > reference_height * 0.85


def _score_candidate(lines: list[OcrLine]) -> float:
    if not lines:
        return -1_000_000.0

    texts = [line.text.strip() for line in lines if line.text.strip()]
    text = "\n".join(texts)
    meaningful_chars = [char for char in text if char.isalnum() or _is_hangul(char)]
    total_non_space = [char for char in text if not char.isspace()]
    avg_score = _mean([line.score for line in lines if line.score is not None]) or 0.5
    avg_line_length = _mean([len(line.text.strip()) for line in lines]) or 0
    short_line_count = sum(1 for line in lines if len(line.text.strip()) <= 2)
    fragment_ratio = short_line_count / max(len(lines), 1)
    meaningful_ratio = len(meaningful_chars) / max(len(total_non_space), 1)

    return (
        len(meaningful_chars) * 0.8
        + avg_score * 120
        + min(avg_line_length, 48) * 2.4
        + meaningful_ratio * 35
        - fragment_ratio * 90
        - short_line_count * 3.5
    )


def _group_center_y(group: list[RawOcrItem]) -> float:
    return _mean([item.center_y for item in group]) or 0.0


def _group_box(group: list[RawOcrItem]) -> tuple[float | None, float | None, float | None, float | None]:
    boxed = [item for item in group if item.has_box]
    if not boxed:
        return None, None, None, None

    left = min(float(item.x or 0) for item in boxed)
    top = min(float(item.y or 0) for item in boxed)
    right = max(item.right for item in boxed)
    bottom = max(float(item.y or 0) + float(item.height or 0) for item in boxed)
    return left, top, right - left, bottom - top


def _normalize_box(box: Any) -> tuple[float | None, float | None, float | None, float | None]:
    if box is None:
        return None, None, None, None

    if isinstance(box, (list, tuple)) and len(box) == 4 and all(_is_number(value) for value in box):
        x1, y1, x2, y2 = [float(value) for value in box]
        return min(x1, x2), min(y1, y2), abs(x2 - x1), abs(y2 - y1)

    points: list[tuple[float, float]] = []
    if isinstance(box, (list, tuple)):
        for point in box:
            if isinstance(point, (list, tuple)) and len(point) >= 2 and _is_number(point[0]) and _is_number(point[1]):
                points.append((float(point[0]), float(point[1])))

    if not points:
        return None, None, None, None

    xs = [point[0] for point in points]
    ys = [point[1] for point in points]
    left = min(xs)
    top = min(ys)
    return left, top, max(xs) - left, max(ys) - top


def _normalize_spacing(text: str) -> str:
    return " ".join(text.split())


def _mean(values: list[float | int | None]) -> float | None:
    numeric_values = [float(value) for value in values if value is not None]
    if not numeric_values:
        return None
    return sum(numeric_values) / len(numeric_values)


def _at(values: Any, index: int) -> Any:
    try:
        return values[index]
    except Exception:
        return None


def _to_float(value: Any) -> float | None:
    try:
        return float(value)
    except Exception:
        return None


def _is_number(value: Any) -> bool:
    try:
        float(value)
        return True
    except Exception:
        return False


def _is_hangul(char: str) -> bool:
    return "\uac00" <= char <= "\ud7a3"


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
