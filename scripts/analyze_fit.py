#!/usr/bin/env python3
"""Run the installed cycling-fit-analysis skill script from this project."""

from __future__ import annotations

import os
import runpy
import sys
from pathlib import Path


def main() -> None:
    codex_home = Path(os.environ.get("CODEX_HOME", Path.home() / ".codex"))
    script = codex_home / "skills" / "cycling-fit-analysis" / "scripts" / "analyze_fit.py"
    if not script.exists():
        raise SystemExit(
            "Missing cycling-fit-analysis skill script. "
            "Install the skill first, then rerun this command."
        )

    sys.argv[0] = str(script)
    runpy.run_path(str(script), run_name="__main__")


if __name__ == "__main__":
    main()
