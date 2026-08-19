"""Puts this directory on sys.path so the tests can import the `sofascore` package."""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
