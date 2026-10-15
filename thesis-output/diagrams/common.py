"""Đường dẫn dùng chung cho các script sinh sơ đồ."""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "scripts"))
SRC = HERE / "src"
SRC.mkdir(exist_ok=True)
