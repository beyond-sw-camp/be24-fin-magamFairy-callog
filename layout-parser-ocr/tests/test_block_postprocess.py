from __future__ import annotations

import unittest

from app.block_postprocess import clamp_bbox, prepare_blocks_for_ocr


class BlockPostprocessTests(unittest.TestCase):
    def test_pdf_native_text_bypasses_ocr_and_marks_overlapping_layout(self) -> None:
        blocks = [
            {
                "type": "Page",
                "bbox": [0, 0, 1000, 1000],
                "score": 1.0,
                "metadata": {"source": "page_fallback", "role": "coverage_guard"},
            },
            {
                "type": "Text",
                "bbox": [100, 100, 500, 220],
                "score": 1.0,
                "text": "Native PDF text",
                "metadata": {
                    "source": "pdf_native",
                    "role": "embedded_text",
                    "has_pdf_text": True,
                },
            },
            {
                "type": "Text",
                "bbox": [110, 110, 490, 210],
                "score": 0.91,
                "metadata": {"source": "layout_parser", "role": "semantic_region"},
            },
        ]

        result = prepare_blocks_for_ocr(blocks, page_width=1000, page_height=1000)
        native = next(block for block in result if block.get("text") == "Native PDF text")
        layout = next(
            block
            for block in result
            if block["metadata"]["source"] == "layout_parser"
        )

        self.assertEqual(native["analysis_hint"], "embedded_text")
        self.assertFalse(native["metadata"]["ocr"]["recommended"])
        self.assertEqual(native["metadata"]["ocr"]["mode"], "embedded_text")
        self.assertEqual(native["metadata"]["bbox_normalized"], [0.1, 0.1, 0.5, 0.22])
        self.assertEqual(layout["analysis_hint"], "layout")
        self.assertFalse(layout["metadata"]["ocr"]["recommended"])
        self.assertEqual(layout["metadata"]["ocr"]["redundant_with"], "pdf_native_text")

    def test_visual_text_is_high_priority_ocr_region(self) -> None:
        blocks = [
            {
                "type": "Text",
                "bbox": [50, 60, 300, 120],
                "score": 0.72,
                "metadata": {"source": "visual_text", "role": "ocr_region"},
            }
        ]

        result = prepare_blocks_for_ocr(blocks, page_width=500, page_height=500)
        block = result[0]

        self.assertEqual(block["analysis_hint"], "ocr")
        self.assertTrue(block["metadata"]["ocr"]["recommended"])
        self.assertEqual(block["metadata"]["ocr"]["priority"], 95)
        self.assertIn("ocr_region", block["metadata"]["roles"])

    def test_pdf_native_does_not_disable_visual_text_merged_ocr(self) -> None:
        blocks = [
            {
                "type": "Text",
                "bbox": [100, 100, 500, 220],
                "score": 1.0,
                "text": "Native PDF text",
                "metadata": {
                    "source": "pdf_native",
                    "role": "embedded_text",
                    "has_pdf_text": True,
                },
            },
            {
                "type": "Text",
                "bbox": [100, 100, 500, 220],
                "score": 0.8,
                "metadata": {
                    "source": "visual_text_merged",
                    "role": "ocr_region",
                    "child_candidate_ids": ["visual-text-001", "visual-text-002"],
                },
            },
        ]

        result = prepare_blocks_for_ocr(blocks, page_width=1000, page_height=1000)
        merged = next(
            block
            for block in result
            if block["metadata"]["source"] == "visual_text_merged"
        )

        self.assertEqual(merged["analysis_hint"], "ocr")
        self.assertTrue(merged["metadata"]["ocr"]["recommended"])
        self.assertEqual(merged["metadata"]["ocr"]["priority"], 100)
        self.assertNotIn("redundant_with", merged["metadata"]["ocr"])

    def test_xy_reading_order_handles_full_width_header_then_columns(self) -> None:
        blocks = [
            {"type": "Title", "bbox": [40, 20, 760, 70], "score": 0.99, "metadata": {}},
            {"type": "Text", "bbox": [50, 120, 350, 170], "score": 0.9, "metadata": {"label": "left-1"}},
            {"type": "Text", "bbox": [50, 210, 350, 260], "score": 0.9, "metadata": {"label": "left-2"}},
            {"type": "Text", "bbox": [450, 120, 750, 170], "score": 0.9, "metadata": {"label": "right-1"}},
            {"type": "Text", "bbox": [450, 210, 750, 260], "score": 0.9, "metadata": {"label": "right-2"}},
        ]

        result = prepare_blocks_for_ocr(blocks, page_width=800, page_height=1000)
        labels = [block["metadata"].get("label", "title") for block in result]

        self.assertEqual(labels, ["title", "left-1", "left-2", "right-1", "right-2"])

    def test_clamp_bbox_keeps_crop_non_empty_inside_page(self) -> None:
        self.assertEqual(clamp_bbox([-10, 20, -5, 20], 100, 80), [0.0, 20.0, 1.0, 21.0])


if __name__ == "__main__":
    unittest.main()
