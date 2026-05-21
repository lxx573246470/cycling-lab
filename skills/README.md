# Project Skills

本目录记录 `cycling-lab` 项目内约定的 Codex skill。可读版本在 `skills/`，Codex 项目版本同步放在 `.codex/skills/`。

- `cycling-lab/`: 本仓库的训练数据、周计划、FIT 分析和周复盘工作流。
- `cycling-fit-analysis/`: FIT 文件解析、训练指标计算、Markdown 训练笔记生成。

本仓库的 `scripts/analyze_fit.py` 优先调用 `skills/cycling-fit-analysis/scripts/analyze_fit.py`，找不到时才回退到本机全局安装的 `cycling-fit-analysis` skill。
