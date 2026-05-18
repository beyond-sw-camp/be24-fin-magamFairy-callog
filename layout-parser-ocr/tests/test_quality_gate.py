from __future__ import annotations

import unittest

from tools.quality_gate import TARGET_SCORE, ahp_weights, consistency_ratio, evaluate, pairwise_matrix


class QualityGateTests(unittest.TestCase):
    def test_ahp_weights_are_normalized_and_consistent(self) -> None:
        estimates = {
            "api": 0.16,
            "layout": 0.24,
            "ocr": 0.32,
            "reliability": 0.18,
            "verification": 0.10,
        }
        weights = ahp_weights(estimates)
        ratio = consistency_ratio(pairwise_matrix(estimates), list(weights.values()))

        self.assertAlmostEqual(sum(weights.values()), 1.0)
        self.assertLessEqual(ratio, 0.1)

    def test_repository_passes_target_score(self) -> None:
        report = evaluate()

        self.assertTrue(report["passed"], report)
        self.assertGreaterEqual(report["score"], TARGET_SCORE)
        self.assertLessEqual(report["ahp"]["consistency_ratio"], 0.1)


if __name__ == "__main__":
    unittest.main()
