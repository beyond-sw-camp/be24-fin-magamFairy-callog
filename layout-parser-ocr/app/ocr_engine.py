from __future__ import annotations

import gc
import logging
import os
import shutil
import tempfile
import time
import uuid
from dataclasses import dataclass, replace
from datetime import datetime, timezone
from functools import lru_cache
from pathlib import Path
from queue import Full, Queue
from threading import BoundedSemaphore, Lock, Thread
from typing import Any, Dict, List, Optional, Sequence, Tuple, Union

import fitz
from fastapi import APIRouter, File, HTTPException, Request, UploadFile
from PIL import Image, ImageEnhance, ImageFilter, ImageOps
from pydantic import BaseModel
from starlette.concurrency import run_in_threadpool


OCR_LANG = os.getenv("OCR_LANG", "korean")
OCR_MAX_PDF_PAGES = int(os.getenv("OCR_MAX_PDF_PAGES", "10"))
OCR_MAX_UPLOAD_MB = int(os.getenv("OCR_MAX_UPLOAD_MB", "20"))
OCR_USE_GPU = os.getenv("OCR_USE_GPU", "false").lower() == "true"
OCR_USE_ANGLE_CLS = os.getenv("OCR_USE_ANGLE_CLS", "false").lower() == "true"
OCR_ENABLE_MKLDNN = os.getenv("OCR_ENABLE_MKLDNN", "false").lower() == "true"
OCR_CPU_THREADS = int(os.getenv("OCR_CPU_THREADS", "1"))
PADDLE_OCR_BASE_DIR = os.getenv("PADDLE_OCR_BASE_DIR", "").strip()
OCR_TEMP_ROOT = os.getenv("OCR_TEMP_ROOT", "").strip()
OCR_PREPROCESS = os.getenv("OCR_PREPROCESS", "true").lower() == "true"
OCR_CANDIDATE_STRATEGY = os.getenv("OCR_CANDIDATE_STRATEGY", "first").strip().lower()
OCR_AUTO_ACCEPT_MIN_SCORE = float(os.getenv("OCR_AUTO_ACCEPT_MIN_SCORE", "330"))
OCR_AUTO_ACCEPT_MIN_AVG_CONFIDENCE = float(os.getenv("OCR_AUTO_ACCEPT_MIN_AVG_CONFIDENCE", "0.88"))
OCR_AUTO_ACCEPT_MIN_CHARS = int(os.getenv("OCR_AUTO_ACCEPT_MIN_CHARS", "180"))
OCR_AUTO_ACCEPT_MIN_LINES = int(os.getenv("OCR_AUTO_ACCEPT_MIN_LINES", "2"))
OCR_AUTO_ACCEPT_MIN_MEANINGFUL_RATIO = float(os.getenv("OCR_AUTO_ACCEPT_MIN_MEANINGFUL_RATIO", "0.72"))
OCR_AUTO_ACCEPT_MAX_FRAGMENT_RATIO = float(os.getenv("OCR_AUTO_ACCEPT_MAX_FRAGMENT_RATIO", "0.35"))
OCR_TARGET_LONG_EDGE = int(os.getenv("OCR_TARGET_LONG_EDGE", "1600"))
OCR_DET_LIMIT_SIDE_LEN = int(os.getenv("OCR_DET_LIMIT_SIDE_LEN", str(OCR_TARGET_LONG_EDGE)))
OCR_CONTRAST = float(os.getenv("OCR_CONTRAST", "1.8"))
OCR_SHARPNESS = float(os.getenv("OCR_SHARPNESS", "1.6"))
OCR_SOFT_CONTRAST = float(os.getenv("OCR_SOFT_CONTRAST", "1.18"))
OCR_SOFT_SHARPNESS = float(os.getenv("OCR_SOFT_SHARPNESS", "1.08"))
OCR_MIN_SCORE = float(os.getenv("OCR_MIN_SCORE", "0.42"))
OCR_RETRY_ATTEMPTS = int(os.getenv("OCR_RETRY_ATTEMPTS", "3"))
OCR_MAX_CONCURRENT_REQUESTS = max(1, int(os.getenv("OCR_MAX_CONCURRENT_REQUESTS", "1")))
OCR_BUSY_STATUS_CODE = int(os.getenv("OCR_BUSY_STATUS_CODE", "503"))
OCR_TEMP_CLEANUP_MAX_AGE_SECONDS = int(os.getenv("OCR_TEMP_CLEANUP_MAX_AGE_SECONDS", str(6 * 60 * 60)))
OCR_JOB_QUEUE_SIZE = max(1, int(os.getenv("OCR_JOB_QUEUE_SIZE", "5")))
OCR_JOB_WORKERS = max(1, min(int(os.getenv("OCR_JOB_WORKERS", str(OCR_MAX_CONCURRENT_REQUESTS))), OCR_MAX_CONCURRENT_REQUESTS))
OCR_JOB_RETENTION_SECONDS = int(os.getenv("OCR_JOB_RETENTION_SECONDS", str(60 * 60)))
PDF_RENDER_SCALE = float(os.getenv("OCR_PDF_RENDER_SCALE", "3"))
OCR_TEMP_DIR_PREFIX = "callog-ocr-"

os.environ["FLAGS_use_mkldnn"] = "1" if OCR_ENABLE_MKLDNN else "0"
os.environ["OMP_NUM_THREADS"] = str(OCR_CPU_THREADS)
os.environ["MKL_NUM_THREADS"] = str(OCR_CPU_THREADS)
os.environ["OPENBLAS_NUM_THREADS"] = str(OCR_CPU_THREADS)
os.environ["NUMEXPR_NUM_THREADS"] = str(OCR_CPU_THREADS)
if PADDLE_OCR_BASE_DIR:
    os.environ["PADDLEOCR_HOME"] = PADDLE_OCR_BASE_DIR

logger = logging.getLogger(__name__)
ocr_router = APIRouter()
ocr_lock = Lock()
ocr_request_slots = BoundedSemaphore(OCR_MAX_CONCURRENT_REQUESTS)
ocr_job_queue: Queue = Queue(maxsize=OCR_JOB_QUEUE_SIZE)
ocr_job_workers: List[Thread] = []
ocr_job_workers_lock = Lock()
ocr_job_stop = object()
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
    score: Optional[float] = None
    x: Optional[float] = None
    y: Optional[float] = None
    width: Optional[float] = None
    height: Optional[float] = None


class OcrResponse(BaseModel):
    text: str
    lines: List[OcrLine]
    pageCount: int


class OcrJobAccepted(BaseModel):
    job_id: str
    status: str
    status_url: str
    result_url: str


class OcrJobStatus(BaseModel):
    job_id: str
    status: str
    created_at: datetime
    updated_at: datetime
    error: Optional[str] = None


@dataclass
class RawOcrItem:
    text: str
    score: Optional[float] = None
    x: Optional[float] = None
    y: Optional[float] = None
    width: Optional[float] = None
    height: Optional[float] = None

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
    lines: List[OcrLine]
    score: float


@dataclass
class CandidateStats:
    meaningful_char_count: int
    total_non_space_count: int
    avg_confidence: float
    avg_line_length: float
    short_line_count: int
    fragment_ratio: float
    meaningful_ratio: float


@dataclass
class OcrJobRecord:
    job_id: str
    status: str
    created_at: datetime
    updated_at: datetime
    upload_path: Path
    temp_dir: Path
    content_type: str
    filename: Optional[str] = None
    result: Optional[OcrResponse] = None
    error: Optional[str] = None


class OcrJobStore:
    def __init__(self) -> None:
        self._records: Dict[str, OcrJobRecord] = {}
        self._lock = Lock()

    def create(
        self,
        job_id: str,
        upload_path: Path,
        temp_dir: Path,
        content_type: str,
        filename: Optional[str],
    ) -> OcrJobRecord:
        now = _utc_now()
        record = OcrJobRecord(
            job_id=job_id,
            status="queued",
            created_at=now,
            updated_at=now,
            upload_path=upload_path,
            temp_dir=temp_dir,
            content_type=content_type,
            filename=filename,
        )
        with self._lock:
            self._records[job_id] = record
        return replace(record)

    def mark_running(self, job_id: str) -> None:
        self._update(job_id, status="running", error=None)

    def mark_completed(self, job_id: str, result: OcrResponse) -> None:
        self._update(job_id, status="completed", result=result, error=None)

    def mark_failed(self, job_id: str, error: str) -> None:
        self._update(job_id, status="failed", error=error)

    def get(self, job_id: str) -> Optional[OcrJobRecord]:
        with self._lock:
            record = self._records.get(job_id)
            return replace(record) if record is not None else None

    def remove(self, job_id: str) -> None:
        with self._lock:
            self._records.pop(job_id, None)

    def cleanup_terminal(self, max_age_seconds: Optional[int] = None) -> int:
        retention = OCR_JOB_RETENTION_SECONDS if max_age_seconds is None else max_age_seconds
        if retention < 0:
            return 0

        cutoff = time.time() - retention
        removed = 0
        with self._lock:
            for job_id, record in list(self._records.items()):
                if record.status not in {"completed", "failed"}:
                    continue
                if retention == 0 or record.updated_at.timestamp() <= cutoff:
                    self._records.pop(job_id, None)
                    removed += 1
        return removed

    def _update(self, job_id: str, **changes: object) -> None:
        with self._lock:
            record = self._records[job_id]
            for key, value in changes.items():
                setattr(record, key, value)
            record.updated_at = _utc_now()


ocr_jobs = OcrJobStore()


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


@lru_cache(maxsize=1)
def get_ocr() -> Any:
    from paddleocr import PaddleOCR

    if PADDLE_OCR_BASE_DIR:
        import paddleocr.paddleocr as paddleocr_module

        base_dir = str(Path(PADDLE_OCR_BASE_DIR).expanduser())
        Path(base_dir).mkdir(parents=True, exist_ok=True)
        paddleocr_module.BASE_DIR = base_dir

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


def warm_up_ocr_model() -> None:
    get_ocr()
    logger.info(
        (
            "OCR model warmed up. lang=%s gpu=%s angle_cls=%s mkldnn=%s cpu_threads=%s "
            "target_long_edge=%s det_limit_side_len=%s candidate_strategy=%s"
        ),
        OCR_LANG,
        OCR_USE_GPU,
        OCR_USE_ANGLE_CLS,
        OCR_ENABLE_MKLDNN,
        OCR_CPU_THREADS,
        OCR_TARGET_LONG_EDGE,
        OCR_DET_LIMIT_SIDE_LEN,
        _candidate_strategy(),
    )


def release_ocr_model() -> None:
    with ocr_lock:
        get_ocr.cache_clear()
    gc.collect()
    logger.info("OCR model cache cleared.")


def start_ocr_job_workers() -> None:
    with ocr_job_workers_lock:
        if ocr_job_workers:
            return
        for index in range(OCR_JOB_WORKERS):
            worker = Thread(target=_ocr_job_worker, name=f"ocr-job-worker-{index + 1}", daemon=True)
            worker.start()
            ocr_job_workers.append(worker)
    logger.info(
        "OCR job workers started. workers=%s queueSize=%s maxConcurrent=%s",
        OCR_JOB_WORKERS,
        OCR_JOB_QUEUE_SIZE,
        OCR_MAX_CONCURRENT_REQUESTS,
    )


def stop_ocr_job_workers() -> None:
    with ocr_job_workers_lock:
        workers = list(ocr_job_workers)
        ocr_job_workers.clear()

    for _worker in workers:
        try:
            ocr_job_queue.put(ocr_job_stop, timeout=1)
        except Full:
            logger.warning("OCR job queue is full while stopping workers.")

    for worker in workers:
        worker.join(timeout=2)
    if workers:
        logger.info("OCR job workers stopped. count=%s", len(workers))


def _ocr_job_worker() -> None:
    while True:
        item = ocr_job_queue.get()
        try:
            if item is ocr_job_stop:
                return
            _run_queued_ocr_job(str(item))
        finally:
            ocr_job_queue.task_done()


def _run_queued_ocr_job(job_id: str) -> None:
    record = ocr_jobs.get(job_id)
    if record is None:
        logger.warning("OCR job record was missing. jobId=%s", job_id)
        return

    start_time = time.perf_counter()
    ocr_request_slots.acquire()
    try:
        ocr_jobs.mark_running(job_id)
        logger.info(
            "OCR job started. jobId=%s fileName=%s contentType=%s",
            job_id,
            record.filename,
            record.content_type,
        )
        response = _extract_from_path(record.upload_path, record.content_type, record.temp_dir)
        ocr_jobs.mark_completed(job_id, response)
        logger.info(
            "OCR job completed. jobId=%s textLength=%s pageCount=%s elapsedMs=%s",
            job_id,
            len(response.text or ""),
            response.pageCount,
            int((time.perf_counter() - start_time) * 1000),
        )
    except HTTPException as exc:
        detail = str(exc.detail)
        ocr_jobs.mark_failed(job_id, f"HTTP {exc.status_code}: {detail}")
        logger.warning("OCR job failed. jobId=%s status=%s detail=%s", job_id, exc.status_code, detail)
    except Exception as exc:
        ocr_jobs.mark_failed(job_id, f"{type(exc).__name__}: {exc}")
        logger.exception("OCR job crashed. jobId=%s fileName=%s", job_id, record.filename)
    finally:
        shutil.rmtree(record.temp_dir, ignore_errors=True)
        ocr_request_slots.release()


@ocr_router.post("/ocr", response_model=OcrResponse)
async def extract_text(file: UploadFile = File(...)) -> OcrResponse:
    request_id = uuid.uuid4().hex[:8]
    start_time = time.perf_counter()
    temp_dir: Optional[Path] = None

    if not ocr_request_slots.acquire(blocking=False):
        await _close_upload(file)
        logger.warning(
            "OCR request rejected because service is busy. id=%s fileName=%s maxConcurrent=%s",
            request_id,
            file.filename,
            OCR_MAX_CONCURRENT_REQUESTS,
        )
        raise HTTPException(
            status_code=OCR_BUSY_STATUS_CODE,
            detail=f"OCR service is busy. max_concurrent_requests={OCR_MAX_CONCURRENT_REQUESTS}",
        )

    try:
        suffix = Path(file.filename or "").suffix.lower()
        if not suffix:
            suffix = _suffix_from_content_type(file.content_type or "")

        temp_dir = _make_temp_dir()
        upload_path = temp_dir / f"upload{suffix}"
        uploaded_size = await _write_upload_to_path(file, upload_path)
        logger.info(
            "OCR request started. id=%s fileName=%s contentType=%s size=%s",
            request_id,
            file.filename,
            file.content_type,
            uploaded_size,
        )
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
        if temp_dir is not None:
            shutil.rmtree(temp_dir, ignore_errors=True)
        await _close_upload(file)
        ocr_request_slots.release()


@ocr_router.post("/ocr/jobs", response_model=OcrJobAccepted, status_code=202)
async def create_ocr_job(request: Request, file: UploadFile = File(...)) -> OcrJobAccepted:
    ocr_jobs.cleanup_terminal()
    if ocr_job_queue.full():
        await _close_upload(file)
        raise HTTPException(status_code=429, detail=f"OCR job queue is full. queue_size={OCR_JOB_QUEUE_SIZE}")

    job_id = uuid.uuid4().hex
    temp_dir: Optional[Path] = None
    enqueued = False

    try:
        suffix = Path(file.filename or "").suffix.lower()
        if not suffix:
            suffix = _suffix_from_content_type(file.content_type or "")

        temp_dir = _make_temp_dir()
        upload_path = temp_dir / f"upload{suffix}"
        uploaded_size = await _write_upload_to_path(file, upload_path)
        ocr_jobs.create(
            job_id=job_id,
            upload_path=upload_path,
            temp_dir=temp_dir,
            content_type=file.content_type or "",
            filename=file.filename,
        )

        try:
            ocr_job_queue.put_nowait(job_id)
            enqueued = True
        except Full as exc:
            ocr_jobs.remove(job_id)
            raise HTTPException(
                status_code=429,
                detail=f"OCR job queue is full. queue_size={OCR_JOB_QUEUE_SIZE}",
            ) from exc

        logger.info(
            "OCR job queued. jobId=%s fileName=%s contentType=%s size=%s queueSize=%s",
            job_id,
            file.filename,
            file.content_type,
            uploaded_size,
            ocr_job_queue.qsize(),
        )
        base_url = _request_base_url(request)
        return OcrJobAccepted(
            job_id=job_id,
            status="queued",
            status_url=f"{base_url}/ocr/jobs/{job_id}",
            result_url=f"{base_url}/ocr/jobs/{job_id}/result",
        )
    except HTTPException:
        if temp_dir is not None and not enqueued:
            shutil.rmtree(temp_dir, ignore_errors=True)
        raise
    except Exception as exc:
        if temp_dir is not None and not enqueued:
            shutil.rmtree(temp_dir, ignore_errors=True)
        logger.exception("OCR job enqueue crashed. jobId=%s fileName=%s", job_id, file.filename)
        raise HTTPException(status_code=500, detail=f"OCR job enqueue failed: {type(exc).__name__}: {exc}") from exc
    finally:
        await _close_upload(file)


@ocr_router.get("/ocr/jobs/{job_id}", response_model=OcrJobStatus)
def get_ocr_job(job_id: str) -> OcrJobStatus:
    record = ocr_jobs.get(job_id)
    if record is None:
        raise HTTPException(status_code=404, detail="OCR job not found")

    return OcrJobStatus(
        job_id=record.job_id,
        status=record.status,
        created_at=record.created_at,
        updated_at=record.updated_at,
        error=record.error,
    )


@ocr_router.get("/ocr/jobs/{job_id}/result", response_model=OcrResponse)
def get_ocr_job_result(job_id: str) -> OcrResponse:
    record = ocr_jobs.get(job_id)
    if record is None:
        raise HTTPException(status_code=404, detail="OCR job not found")
    if record.status != "completed" or record.result is None:
        raise HTTPException(status_code=409, detail=f"OCR job is {record.status}")
    return record.result


async def _write_upload_to_path(file: UploadFile, upload_path: Path) -> int:
    max_bytes = OCR_MAX_UPLOAD_MB * 1024 * 1024
    uploaded_size = 0

    with upload_path.open("wb") as out:
        while True:
            chunk = await file.read(1024 * 1024)
            if not chunk:
                break
            uploaded_size += len(chunk)
            if uploaded_size > max_bytes:
                raise HTTPException(status_code=413, detail="File is too large")
            out.write(chunk)

    if uploaded_size == 0:
        raise HTTPException(status_code=400, detail="Uploaded file is empty")

    return uploaded_size


async def _close_upload(file: UploadFile) -> None:
    try:
        await file.close()
    except Exception:
        logger.debug("Failed to close OCR upload file.", exc_info=True)


def _extract_from_path(path: Path, content_type: str, temp_dir: Path) -> OcrResponse:
    if _is_pdf(path, content_type):
        return _extract_from_pdf(path, temp_dir)

    if _is_image(path, content_type):
        lines = _extract_from_image(path, temp_dir)
        return _response_from_pages([lines])

    raise HTTPException(status_code=415, detail="Unsupported file type")


def _make_temp_dir() -> Path:
    for root in _ocr_temp_roots():
        temp_dir: Optional[Path] = None
        try:
            root.mkdir(parents=True, exist_ok=True)
            temp_dir = root / f"{OCR_TEMP_DIR_PREFIX}{uuid.uuid4().hex}"
            temp_dir.mkdir()
            probe_path = temp_dir / ".write-check"
            probe_path.write_bytes(b"")
            if probe_path.exists():
                probe_path.unlink()
            return temp_dir
        except Exception:
            if temp_dir is not None:
                shutil.rmtree(temp_dir, ignore_errors=True)

    raise HTTPException(status_code=500, detail="OCR temporary directory is not writable")


def _request_base_url(request: Request) -> str:
    return str(request.base_url).rstrip("/")


def _ocr_temp_roots() -> List[Path]:
    roots: List[Path] = []
    if OCR_TEMP_ROOT:
        roots.append(Path(OCR_TEMP_ROOT))
    roots.extend([Path(tempfile.gettempdir()), Path.cwd() / ".callog-ocr-tmp"])

    unique_roots: List[Path] = []
    seen: set = set()
    for root in roots:
        normalized = str(root)
        if normalized in seen:
            continue
        seen.add(normalized)
        unique_roots.append(root)
    return unique_roots


def cleanup_stale_ocr_temp_dirs(
    max_age_seconds: Optional[int] = None,
    roots: Optional[Sequence[Path]] = None,
) -> int:
    cleanup_age = OCR_TEMP_CLEANUP_MAX_AGE_SECONDS if max_age_seconds is None else max_age_seconds
    if cleanup_age < 0:
        return 0

    cutoff = time.time() - cleanup_age
    removed = 0

    for root in roots or _ocr_temp_roots():
        try:
            if not root.exists() or not root.is_dir():
                continue
            for child in root.iterdir():
                if not child.is_dir() or not child.name.startswith(OCR_TEMP_DIR_PREFIX):
                    continue
                if cleanup_age == 0 or child.stat().st_mtime <= cutoff:
                    shutil.rmtree(child, ignore_errors=True)
                    removed += 1
        except Exception:
            logger.warning("Failed to cleanup stale OCR temp directory. root=%s", root, exc_info=True)

    if removed:
        logger.info("Removed stale OCR temp directories. count=%s", removed)
    return removed


def _extract_from_pdf(path: Path, temp_dir: Path) -> OcrResponse:
    pages: List[List[OcrLine]] = []
    page_errors: List[Exception] = []

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


def _extract_from_image(path: Path, temp_dir: Path) -> List[OcrLine]:
    strategy = _candidate_strategy()
    candidates = _prepare_image_candidates(path, temp_dir, strategy=strategy)

    best: Optional[CandidateResult] = None
    errors: List[Exception] = []

    for index, candidate_path in enumerate(candidates):
        try:
            result = _run_ocr(candidate_path)
            raw_items = _collect_raw_items(result)
            lines = _layout_lines(raw_items)
            score = _score_candidate(lines)
            if best is None or score > best.score:
                best = CandidateResult(path=candidate_path, lines=lines, score=score)
            if strategy == "auto" and index == 0 and _is_auto_acceptable(lines, score):
                stats = _candidate_stats(lines)
                logger.info(
                    (
                        "OCR accepted first candidate. path=%s candidate=%s score=%.2f "
                        "avgConfidence=%.3f meaningfulChars=%s lineCount=%s"
                    ),
                    path,
                    candidate_path.name,
                    score,
                    stats.avg_confidence,
                    stats.meaningful_char_count,
                    len(lines),
                )
                return lines
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
    elif best is not None and len(candidates) > 1:
        logger.info(
            "OCR selected best candidate after full pass. path=%s candidate=%s candidates=%s score=%.2f",
            path,
            best.path.name,
            len(candidates),
            best.score,
        )

    return best.lines if best is not None else []


def _candidate_strategy() -> str:
    if OCR_CANDIDATE_STRATEGY in {"auto", "all"}:
        return OCR_CANDIDATE_STRATEGY
    if OCR_CANDIDATE_STRATEGY in {"first", "rgb"}:
        return "first"
    logger.warning("Unknown OCR_CANDIDATE_STRATEGY=%s. Falling back to auto.", OCR_CANDIDATE_STRATEGY)
    return "auto"


def _run_ocr(path: Path) -> Any:
    attempts = max(1, OCR_RETRY_ATTEMPTS)
    latest_error: Optional[Exception] = None

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


def _prepare_image_candidates(path: Path, temp_dir: Path, strategy: Optional[str] = None) -> List[Path]:
    if not OCR_PREPROCESS:
        return [path]

    try:
        with Image.open(path) as source:
            base = ImageOps.exif_transpose(source).convert("RGB")
            base = _resize_for_ocr(base)

            rgb_path = _save_candidate(base, temp_dir, path.stem, "rgb")
            if strategy == "first":
                return [rgb_path]

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


def _collect_raw_items(result: Any) -> List[RawOcrItem]:
    items: List[RawOcrItem] = []

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


def _layout_lines(items: List[RawOcrItem]) -> List[OcrLine]:
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

    line_groups: List[List[RawOcrItem]] = []
    for item in sorted(boxed_items, key=lambda value: (value.center_y, value.x or 0)):
        target_group = _find_line_group(line_groups, item)
        if target_group is None:
            line_groups.append([item])
        else:
            target_group.append(item)

    lines: List[OcrLine] = []
    for group in sorted(line_groups, key=lambda value: _group_center_y(value)):
        text = _join_line_group(group)
        if not text:
            continue
        score = _mean([item.score for item in group if item.score is not None])
        x, y, width, height = _group_box(group)
        lines.append(OcrLine(text=text, score=score, x=x, y=y, width=width, height=height))

    return lines


def _filter_low_confidence(items: List[RawOcrItem]) -> List[RawOcrItem]:
    filtered = [
        item for item in items
        if item.score is None or item.score >= OCR_MIN_SCORE or len(item.text.strip()) >= 4
    ]
    if len(filtered) < max(3, int(len(items) * 0.35)):
        return items
    return filtered


def _find_line_group(
    line_groups: List[List[RawOcrItem]],
    item: RawOcrItem,
) -> Optional[List[RawOcrItem]]:
    for group in reversed(line_groups):
        group_height = max(
            _mean([entry.height for entry in group if entry.height is not None]) or 0,
            item.height or 0,
            1,
        )
        tolerance = max(8.0, group_height * 0.58)
        if abs(item.center_y - _group_center_y(group)) <= tolerance:
            return group
    return None


def _join_line_group(group: List[RawOcrItem]) -> str:
    ordered = sorted(group, key=lambda item: item.x or 0)
    parts: List[str] = []
    previous: Optional[RawOcrItem] = None

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


def _response_from_pages(pages: List[List[OcrLine]]) -> OcrResponse:
    all_lines = [line for page in pages for line in page if line.text.strip()]
    page_texts = [_page_text(page) for page in pages]
    text = "\n\n".join(page_text for page_text in page_texts if page_text.strip())
    return OcrResponse(text=text, lines=all_lines, pageCount=len(pages))


def _page_text(lines: List[OcrLine]) -> str:
    visible_lines = [line for line in lines if line.text.strip()]
    if not visible_lines:
        return ""

    page_parts: List[str] = []
    previous: Optional[OcrLine] = None
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


def _score_candidate(lines: List[OcrLine]) -> float:
    if not lines:
        return -1_000_000.0

    stats = _candidate_stats(lines)

    return (
        stats.meaningful_char_count * 0.8
        + stats.avg_confidence * 120
        + min(stats.avg_line_length, 48) * 2.4
        + stats.meaningful_ratio * 35
        - stats.fragment_ratio * 90
        - stats.short_line_count * 3.5
    )


def _candidate_stats(lines: List[OcrLine]) -> CandidateStats:
    texts = [line.text.strip() for line in lines if line.text.strip()]
    text = "\n".join(texts)
    meaningful_chars = [char for char in text if char.isalnum() or _is_hangul(char)]
    total_non_space = [char for char in text if not char.isspace()]
    avg_confidence = _mean([line.score for line in lines if line.score is not None]) or 0.5
    avg_line_length = _mean([len(line.text.strip()) for line in lines]) or 0
    short_line_count = sum(1 for line in lines if len(line.text.strip()) <= 2)
    fragment_ratio = short_line_count / max(len(lines), 1)
    meaningful_ratio = len(meaningful_chars) / max(len(total_non_space), 1)

    return CandidateStats(
        meaningful_char_count=len(meaningful_chars),
        total_non_space_count=len(total_non_space),
        avg_confidence=avg_confidence,
        avg_line_length=avg_line_length,
        short_line_count=short_line_count,
        fragment_ratio=fragment_ratio,
        meaningful_ratio=meaningful_ratio,
    )


def _is_auto_acceptable(lines: List[OcrLine], score: float) -> bool:
    if not lines:
        return False

    stats = _candidate_stats(lines)
    return (
        score >= OCR_AUTO_ACCEPT_MIN_SCORE
        and stats.avg_confidence >= OCR_AUTO_ACCEPT_MIN_AVG_CONFIDENCE
        and stats.meaningful_char_count >= OCR_AUTO_ACCEPT_MIN_CHARS
        and len(lines) >= OCR_AUTO_ACCEPT_MIN_LINES
        and stats.meaningful_ratio >= OCR_AUTO_ACCEPT_MIN_MEANINGFUL_RATIO
        and stats.fragment_ratio <= OCR_AUTO_ACCEPT_MAX_FRAGMENT_RATIO
    )


def _group_center_y(group: List[RawOcrItem]) -> float:
    return _mean([item.center_y for item in group]) or 0.0


def _group_box(group: List[RawOcrItem]) -> Tuple[Optional[float], Optional[float], Optional[float], Optional[float]]:
    boxed = [item for item in group if item.has_box]
    if not boxed:
        return None, None, None, None

    left = min(float(item.x or 0) for item in boxed)
    top = min(float(item.y or 0) for item in boxed)
    right = max(item.right for item in boxed)
    bottom = max(float(item.y or 0) + float(item.height or 0) for item in boxed)
    return left, top, right - left, bottom - top


def _normalize_box(box: Any) -> Tuple[Optional[float], Optional[float], Optional[float], Optional[float]]:
    if box is None:
        return None, None, None, None

    if isinstance(box, (list, tuple)) and len(box) == 4 and all(_is_number(value) for value in box):
        x1, y1, x2, y2 = [float(value) for value in box]
        return min(x1, x2), min(y1, y2), abs(x2 - x1), abs(y2 - y1)

    points: List[Tuple[float, float]] = []
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


def _mean(values: List[Optional[Union[float, int]]]) -> Optional[float]:
    numeric_values = [float(value) for value in values if value is not None]
    if not numeric_values:
        return None
    return sum(numeric_values) / len(numeric_values)


def _at(values: Any, index: int) -> Any:
    try:
        return values[index]
    except Exception:
        return None


def _to_float(value: Any) -> Optional[float]:
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
