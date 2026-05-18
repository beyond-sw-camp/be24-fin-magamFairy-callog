from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


def _bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


@dataclass(frozen=True)
class Settings:
    app_name: str = os.getenv("APP_NAME", "document-analysis-service")
    storage_dir: Path = Path(os.getenv("STORAGE_DIR", "storage")).resolve()
    max_upload_mb: int = int(os.getenv("MAX_UPLOAD_MB", "100"))
    render_dpi: int = int(os.getenv("RENDER_DPI", "200"))
    public_base_url: str = os.getenv("PUBLIC_BASE_URL", "").rstrip("/")

    layout_model_config: str = os.getenv(
        "LAYOUT_MODEL_CONFIG",
        "https://huggingface.co/layoutparser/detectron2/resolve/main/"
        "PubLayNet/faster_rcnn_R_50_FPN_3x/config.yml",
    )
    layout_model_weights: str = os.getenv(
        "LAYOUT_MODEL_WEIGHTS",
        "https://huggingface.co/layoutparser/detectron2/resolve/main/"
        "PubLayNet/faster_rcnn_R_50_FPN_3x/model_final.pth",
    )
    layout_model_device: str = os.getenv("LAYOUT_MODEL_DEVICE", "cpu")
    layout_score_threshold: float = float(os.getenv("LAYOUT_SCORE_THRESHOLD", "0.50"))
    load_model_on_startup: bool = _bool_env("LOAD_MODEL_ON_STARTUP", False)
    include_pdf_native_blocks: bool = _bool_env("INCLUDE_PDF_NATIVE_BLOCKS", True)
    include_visual_text_blocks: bool = _bool_env("INCLUDE_VISUAL_TEXT_BLOCKS", True)
    visual_text_for_pdf: bool = _bool_env("VISUAL_TEXT_FOR_PDF", True)
    reclassify_layout_with_visual_text: bool = _bool_env(
        "RECLASSIFY_LAYOUT_WITH_VISUAL_TEXT",
        True,
    )
    include_full_page_block: bool = _bool_env("INCLUDE_FULL_PAGE_BLOCK", True)
    include_image_targets: bool = _bool_env("INCLUDE_IMAGE_TARGETS", False)
    save_debug_crops: bool = _bool_env("SAVE_DEBUG_CROPS", False)
    ocr_text_canvas_max_long_side: int = int(
        os.getenv("OCR_TEXT_CANVAS_MAX_LONG_SIDE", "1600")
    )
    ocr_text_canvas_max_pixels: int = int(
        os.getenv("OCR_TEXT_CANVAS_MAX_PIXELS", "2000000")
    )
    ocr_text_canvas_min_text_height: int = int(
        os.getenv("OCR_TEXT_CANVAS_MIN_TEXT_HEIGHT", "20")
    )
    ocr_text_canvas_keep_original: bool = _bool_env(
        "OCR_TEXT_CANVAS_KEEP_ORIGINAL",
        True,
    )

    callback_timeout_seconds: float = float(os.getenv("CALLBACK_TIMEOUT_SECONDS", "10"))
    ocr_load_model_on_startup: bool = _bool_env("OCR_LOAD_MODEL_ON_STARTUP", False)

    @property
    def uploads_dir(self) -> Path:
        return self.storage_dir / "uploads"

    @property
    def results_dir(self) -> Path:
        return self.storage_dir / "results"


settings = Settings()
