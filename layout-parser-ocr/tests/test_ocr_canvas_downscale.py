from __future__ import annotations

import unittest
from importlib.util import find_spec


if find_spec("fastapi") is not None:
    from app.processor import _ocr_text_canvas_downscale_plan
else:
    _ocr_text_canvas_downscale_plan = None


def placement(height: int) -> dict:
    return {"canvas_bbox": [0, 0, 100, height]}


class OcrCanvasDownscaleTests(unittest.TestCase):
    @unittest.skipIf(_ocr_text_canvas_downscale_plan is None, "fastapi is not installed in the local test runner")
    def test_downscales_large_canvas_when_text_stays_readable(self) -> None:
        plan = _ocr_text_canvas_downscale_plan(
            (2000, 2000),
            [placement(40), placement(44), placement(48)],
            page_width=2000,
            page_height=2000,
        )

        self.assertTrue(plan["applied"])
        self.assertEqual(plan["size"], (1414, 1414))
        self.assertAlmostEqual(plan["scale"], 0.707107, places=5)
        self.assertGreaterEqual(plan["estimated_guard_text_height_after"], 20)

    @unittest.skipIf(_ocr_text_canvas_downscale_plan is None, "fastapi is not installed in the local test runner")
    def test_text_height_guard_prevents_over_shrinking(self) -> None:
        plan = _ocr_text_canvas_downscale_plan(
            (2000, 2000),
            [placement(22), placement(24), placement(26)],
            page_width=2000,
            page_height=2000,
        )

        self.assertTrue(plan["applied"])
        self.assertEqual(plan["size"], (1818, 1818))
        self.assertAlmostEqual(plan["scale"], 20 / 22, places=5)
        self.assertIn("min_text_height_guard", plan["reason"])

    @unittest.skipIf(_ocr_text_canvas_downscale_plan is None, "fastapi is not installed in the local test runner")
    def test_small_canvas_is_not_resized(self) -> None:
        plan = _ocr_text_canvas_downscale_plan(
            (1200, 1000),
            [placement(28)],
            page_width=1600,
            page_height=2000,
        )

        self.assertFalse(plan["applied"])
        self.assertEqual(plan["size"], (1200, 1000))
        self.assertEqual(plan["scale"], 1.0)


if __name__ == "__main__":
    unittest.main()
