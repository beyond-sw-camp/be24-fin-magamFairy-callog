from __future__ import annotations

import unittest

from app.block_postprocess import prepare_blocks_for_ocr
from app.visual_text import merge_visual_text_blocks


def visual_block(sequence: int, bbox: list[float]) -> dict:
    return {
        "type": "Text",
        "bbox": bbox,
        "score": 0.72,
        "metadata": {
            "source": "visual_text",
            "role": "ocr_region",
            "sequence": sequence,
            "candidate_id": f"visual-text-{sequence:03d}",
        },
    }


class VisualTextMergeTests(unittest.TestCase):
    def test_merges_same_line_candidates(self) -> None:
        blocks = [
            visual_block(1, [55, 160, 220, 196]),
            visual_block(2, [245, 162, 430, 198]),
        ]

        merged = merge_visual_text_blocks(blocks, page_width=800, page_height=1000)

        self.assertEqual(len(merged), 1)
        self.assertEqual(merged[0]["bbox"], [55.0, 160.0, 430.0, 198.0])
        self.assertEqual(merged[0]["metadata"]["source"], "visual_text_merged")
        self.assertEqual(merged[0]["metadata"]["merge_type"], "line")
        self.assertEqual(
            merged[0]["metadata"]["child_candidate_ids"],
            ["visual-text-001", "visual-text-002"],
        )

    def test_merges_nearby_lines_into_paragraph(self) -> None:
        blocks = [
            visual_block(1, [80, 260, 610, 296]),
            visual_block(2, [82, 310, 650, 346]),
        ]

        merged = merge_visual_text_blocks(blocks, page_width=900, page_height=1100)

        self.assertEqual(len(merged), 1)
        self.assertEqual(merged[0]["metadata"]["merge_type"], "paragraph")
        self.assertEqual(merged[0]["bbox"], [80.0, 260.0, 650.0, 346.0])

    def test_keeps_cta_like_short_line_out_of_body_paragraph(self) -> None:
        blocks = [
            visual_block(1, [80, 260, 610, 296]),
            visual_block(2, [82, 310, 650, 346]),
            visual_block(3, [82, 376, 240, 412]),
        ]

        merged = merge_visual_text_blocks(blocks, page_width=900, page_height=1100)

        self.assertEqual(len(merged), 1)
        self.assertEqual(
            merged[0]["metadata"]["child_candidate_ids"],
            ["visual-text-001", "visual-text-002"],
        )

    def test_keeps_title_and_body_separate_when_heights_differ(self) -> None:
        blocks = [
            visual_block(1, [70, 110, 280, 166]),
            visual_block(2, [302, 112, 560, 168]),
            visual_block(3, [74, 188, 600, 218]),
        ]

        merged = merge_visual_text_blocks(blocks, page_width=900, page_height=1100)

        self.assertEqual(len(merged), 1)
        self.assertEqual(merged[0]["metadata"]["merge_type"], "line")
        self.assertEqual(
            merged[0]["metadata"]["child_candidate_ids"],
            ["visual-text-001", "visual-text-002"],
        )

    def test_does_not_merge_oversized_existing_regions(self) -> None:
        blocks = [
            visual_block(1, [705, 554, 1122, 971]),
            visual_block(2, [644, 919, 1122, 1241]),
        ]

        merged = merge_visual_text_blocks(blocks, page_width=1122, page_height=1402)

        self.assertEqual(merged, [])

    def test_merges_side_by_side_multiline_chunks(self) -> None:
        blocks = [
            visual_block(1, [138, 690, 357, 745]),
            visual_block(2, [430, 690, 1067, 801]),
            visual_block(3, [140, 746, 406, 801]),
        ]

        merged = merge_visual_text_blocks(blocks, page_width=1240, page_height=1754)

        self.assertEqual(len(merged), 1)
        self.assertEqual(merged[0]["bbox"], [138.0, 690.0, 1067.0, 801.0])

    def test_merges_large_display_heading_chunks(self) -> None:
        blocks = [
            visual_block(1, [59, 89, 604, 262]),
            visual_block(2, [49, 257, 494, 585]),
            visual_block(3, [558, 252, 852, 425]),
        ]

        merged = merge_visual_text_blocks(blocks, page_width=1122, page_height=1402)

        self.assertEqual(len(merged), 1)
        self.assertEqual(merged[0]["bbox"], [49.0, 89.0, 852.0, 585.0])

    def test_merged_visual_text_has_highest_ocr_priority(self) -> None:
        merged = merge_visual_text_blocks(
            [
                visual_block(1, [55, 160, 220, 196]),
                visual_block(2, [245, 162, 430, 198]),
            ],
            page_width=800,
            page_height=1000,
        )

        prepared = prepare_blocks_for_ocr(merged, page_width=800, page_height=1000)

        self.assertEqual(prepared[0]["analysis_hint"], "ocr")
        self.assertEqual(prepared[0]["metadata"]["ocr"]["priority"], 100)
        self.assertEqual(prepared[0]["metadata"]["ocr"]["mode"], "merged_text_ocr")


if __name__ == "__main__":
    unittest.main()
