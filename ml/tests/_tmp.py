"""A temporary directory that cleans up on Windows too, on any interpreter the pipeline runs on.

`tempfile.TemporaryDirectory(ignore_cleanup_errors=True)` exists only from Python 3.10, and the only interpreter installed
on this machine is 3.9: every test that used it failed in setUp, so the whole ml test suite was unrunnable here. On 3.9 the
cleanup error is swallowed by hand, which is exactly what the flag does.
"""
import shutil
import sys
import tempfile


def temp_dir():
    if sys.version_info >= (3, 10):
        return tempfile.TemporaryDirectory(ignore_cleanup_errors=True)

    class _TempDir(tempfile.TemporaryDirectory):
        def cleanup(self):
            try:
                super().cleanup()
            except OSError:
                shutil.rmtree(self.name, ignore_errors=True)

    return _TempDir()
