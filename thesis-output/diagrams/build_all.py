"""Sinh toàn bộ file .drawio nguồn của sơ đồ. Chạy: python3 diagrams/build_all.py [tên module ...]"""
import importlib
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402,F401

MODULES = ["gen_usecase", "gen_activity", "gen_sequence", "gen_class", "gen_erd", "gen_arch"]

if __name__ == "__main__":
    for name in (sys.argv[1:] or MODULES):
        try:
            mod = importlib.import_module(name)
        except ModuleNotFoundError as e:
            print(f"skip {name}: {e}")
            continue
        mod.build()
        print(f"built {name}")
