from __future__ import annotations

import unittest
from importlib.util import find_spec


if find_spec("numpy") is not None:
    import numpy as np

    from app.visual_graphic import _is_graphic_box
else:
    np = None
    _is_graphic_box = None


class VisualGraphicTests(unittest.TestCase):
    @unittest.skipIf(np is None, "numpy is not installed in the local test runner")
    def test_accepts_large_lower_poster_illustration(self) -> None:
        page_width = 1131
        page_height = 1600
        mask = np.ones((page_height, page_width), dtype=np.uint8) * 255
        text_boxes = [
            (982, 12, 1098, 103),
            (77, 130, 1131, 725),
            (338, 1482, 798, 1561),
        ]

        self.assertTrue(
            _is_graphic_box(
                mask,
                (0, 761, 1131, 1600),
                text_boxes,
                page_width,
                page_height,
                float(page_width * page_height),
            )
        )

    @unittest.skipIf(np is None, "numpy is not installed in the local test runner")
    def test_rejects_large_text_heavy_upper_region(self) -> None:
        page_width = 1131
        page_height = 1600
        mask = np.ones((page_height, page_width), dtype=np.uint8) * 255
        text_boxes = [
            (982, 12, 1098, 103),
            (77, 130, 1131, 725),
        ]

        self.assertFalse(
            _is_graphic_box(
                mask,
                (0, 0, 1131, 718),
                text_boxes,
                page_width,
                page_height,
                float(page_width * page_height),
            )
        )


if __name__ == "__main__":
    unittest.main()
