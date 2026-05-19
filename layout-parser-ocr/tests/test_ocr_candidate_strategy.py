from __future__ import annotations

import tempfile
import unittest
from importlib.util import find_spec
from pathlib import Path


if find_spec("fastapi") is not None and find_spec("PIL") is not None:
    from PIL import Image

    from app.ocr_engine import _candidate_strategy, _prepare_image_candidates
else:
    Image = None
    _candidate_strategy = None
    _prepare_image_candidates = None


class OcrCandidateStrategyTests(unittest.TestCase):
    @unittest.skipIf(_candidate_strategy is None, "OCR service dependencies are not installed")
    def test_default_strategy_is_single_candidate(self) -> None:
        self.assertEqual(_candidate_strategy(), "first")

    @unittest.skipIf(_prepare_image_candidates is None or Image is None, "OCR service dependencies are not installed")
    def test_first_strategy_creates_only_rgb_candidate(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir_name:
            temp_dir = Path(temp_dir_name)
            source_path = temp_dir / "sample.png"
            Image.new("RGB", (120, 80), "white").save(source_path)

            candidates = _prepare_image_candidates(source_path, temp_dir, strategy="first")

            self.assertEqual(len(candidates), 1)
            self.assertEqual(candidates[0].name, "sample-rgb.png")
            self.assertFalse((temp_dir / "sample-soft.png").exists())
            self.assertFalse((temp_dir / "sample-high.png").exists())

    @unittest.skipIf(_prepare_image_candidates is None or Image is None, "OCR service dependencies are not installed")
    def test_auto_strategy_keeps_comparison_candidates(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir_name:
            temp_dir = Path(temp_dir_name)
            source_path = temp_dir / "sample.png"
            Image.new("RGB", (120, 80), "white").save(source_path)

            candidates = _prepare_image_candidates(source_path, temp_dir, strategy="auto")

            self.assertEqual([path.name for path in candidates], [
                "sample-rgb.png",
                "sample-soft.png",
                "sample-high.png",
            ])


if __name__ == "__main__":
    unittest.main()
