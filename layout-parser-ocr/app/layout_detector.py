from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Dict, List
from urllib.parse import urlparse
from urllib.request import urlretrieve

import numpy as np
from PIL import Image

from app.settings import settings


LABEL_MAP = {
    0: "Text",
    1: "Title",
    2: "List",
    3: "Table",
    4: "Figure",
}


class LayoutDetector:
    """Layout-parser backed detector.

    This service intentionally keeps layout detection behind this small adapter
    so the API layer stays stable while layout-parser owns the model inference.
    """

    def __init__(self) -> None:
        self._model = None

    def load(self) -> None:
        if self._model is not None:
            return

        try:
            import layoutparser as lp
        except ImportError as exc:
            raise RuntimeError(
                "layoutparser is not installed. Install runtime dependencies first."
            ) from exc

        kwargs = {
            "extra_config": [
                "MODEL.ROI_HEADS.SCORE_THRESH_TEST",
                settings.layout_score_threshold,
            ],
            "label_map": LABEL_MAP,
        }

        config_path = _materialize_model_asset(settings.layout_model_config, "config")

        if settings.layout_model_weights:
            kwargs["model_path"] = _materialize_model_asset(
                settings.layout_model_weights,
                "weights",
            )

        device = settings.layout_model_device.strip().lower()
        if device == "cpu":
            kwargs["enforce_cpu"] = True
        elif device:
            kwargs["device"] = device

        self._model = lp.Detectron2LayoutModel(config_path, **kwargs)

    def detect(self, image_path: Path) -> List[Dict]:
        self.load()

        with Image.open(image_path) as image:
            rgb = image.convert("RGB")
            layout_input = np.asarray(rgb)

        layout = self._model.detect(layout_input)
        blocks: List[Dict] = []

        for block in layout:
            x1, y1, x2, y2 = _coordinates(block)
            block_type = getattr(block, "type", None) or "Unknown"
            score = float(getattr(block, "score", 0.0) or 0.0)
            blocks.append(
                {
                    "type": block_type,
                    "bbox": [float(x1), float(y1), float(x2), float(y2)],
                    "score": score,
                    "metadata": {
                        "source": "layout_parser",
                        "role": "semantic_region",
                    },
                }
            )

        return blocks


def _coordinates(block: object) -> tuple[float, float, float, float]:
    coordinates = getattr(block, "coordinates", None)
    if coordinates is not None:
        x1, y1, x2, y2 = coordinates
        return float(x1), float(y1), float(x2), float(y2)

    inner_block = getattr(block, "block", None)
    if inner_block is not None:
        return (
            float(getattr(inner_block, "x_1")),
            float(getattr(inner_block, "y_1")),
            float(getattr(inner_block, "x_2")),
            float(getattr(inner_block, "y_2")),
        )

    raise RuntimeError("Unsupported layout block shape")


def _materialize_model_asset(value: str, kind: str) -> str:
    if not value.startswith(("http://", "https://")):
        return value

    cache_dir = settings.storage_dir / "model_cache"
    cache_dir.mkdir(parents=True, exist_ok=True)

    parsed = urlparse(value)
    suffix = Path(parsed.path).suffix or ".bin"
    digest = hashlib.sha256(value.encode("utf-8")).hexdigest()[:16]
    target_path = cache_dir / f"{kind}-{digest}{suffix}"

    if target_path.exists() and target_path.stat().st_size > 0:
        return str(target_path)

    tmp_path = target_path.with_suffix(target_path.suffix + ".tmp")
    urlretrieve(value, tmp_path)
    tmp_path.replace(target_path)
    return str(target_path)


detector = LayoutDetector()
