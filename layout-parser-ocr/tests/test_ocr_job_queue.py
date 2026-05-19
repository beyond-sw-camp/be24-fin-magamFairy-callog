from __future__ import annotations

import tempfile
import unittest
from importlib.util import find_spec
from pathlib import Path


if find_spec("fastapi") is not None:
    from app.ocr_engine import OcrJobStore, OcrResponse
else:
    OcrJobStore = None
    OcrResponse = None


class OcrJobQueueTests(unittest.TestCase):
    @unittest.skipIf(OcrJobStore is None, "fastapi is not installed in the local test runner")
    def test_job_store_tracks_completion_result(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir_name:
            temp_dir = Path(temp_dir_name)
            upload_path = temp_dir / "upload.png"
            upload_path.write_bytes(b"fake")

            store = OcrJobStore()
            created = store.create("job-1", upload_path, temp_dir, "image/png", "sample.png")
            self.assertEqual(created.status, "queued")

            store.mark_running("job-1")
            running = store.get("job-1")
            self.assertIsNotNone(running)
            self.assertEqual(running.status, "running")

            result = OcrResponse(text="recognized text", lines=[], pageCount=1)
            store.mark_completed("job-1", result)

            completed = store.get("job-1")
            self.assertIsNotNone(completed)
            self.assertEqual(completed.status, "completed")
            self.assertEqual(completed.result.text, "recognized text")
            self.assertIsNone(completed.error)

    @unittest.skipIf(OcrJobStore is None, "fastapi is not installed in the local test runner")
    def test_job_store_retention_removes_only_terminal_jobs(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir_name:
            temp_dir = Path(temp_dir_name)
            upload_path = temp_dir / "upload.png"
            upload_path.write_bytes(b"fake")

            store = OcrJobStore()
            store.create("done", upload_path, temp_dir, "image/png", "done.png")
            store.create("running", upload_path, temp_dir, "image/png", "running.png")
            store.mark_completed("done", OcrResponse(text="ok", lines=[], pageCount=1))
            store.mark_running("running")

            removed = store.cleanup_terminal(max_age_seconds=0)

            self.assertEqual(removed, 1)
            self.assertIsNone(store.get("done"))
            self.assertIsNotNone(store.get("running"))


if __name__ == "__main__":
    unittest.main()
