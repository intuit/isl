"""pytest configuration."""
import sys
from pathlib import Path

# Ensure isl-python is on the path
sys.path.insert(0, str(Path(__file__).parent.parent))
