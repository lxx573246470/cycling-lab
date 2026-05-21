#!/usr/bin/env python3
"""Run the project cycling-fit-analysis skill script."""

from __future__ import annotations

import os
import runpy
import sys
from pathlib import Path


def main() -> None:
    project_root = Path(__file__).resolve().parents[1]
    project_script = project_root / "skills" / "cycling-fit-analysis" / "scripts" / "analyze_fit.py"
    codex_home = Path(os.environ.get("CODEX_HOME", Path.home() / ".codex"))
    global_script = codex_home / "skills" / "cycling-fit-analysis" / "scripts" / "analyze_fit.py"
    script = project_script if project_script.exists() else global_script

    if not script.exists():
        raise SystemExit(
            "Missing cycling-fit-analysis skill script in this project or CODEX_HOME. "
            "Restore skills/cycling-fit-analysis or install the global skill, then rerun this command."
        )

    sys.argv[0] = str(script)
    runpy.run_path(str(script), run_name="__main__")


if __name__ == "__main__":
    main()
