# cycling-lab

个人骑行训练实验室，用于沉淀骑手档案、周训练计划、FIT 文件、训练笔记、截图、复盘和可复用训练文件。

## 目录结构

```text
cycling-lab/
├── profile/                 # 骑手长期档案、阈值、设备和偏好
├── plans/                   # 可复用训练课库、周计划和阶段计划
├── training/                # 每周训练数据、FIT、截图和训练笔记
├── review/                  # 周复盘、阶段复盘
├── workouts/                # 可导入骑行平台的训练文件
├── scripts/                 # 本项目辅助脚本
└── skills/                  # Codex 项目技能说明
```

## 工作流

1. 在 `profile/rider-profile.md` 维护身高、体重、最大心率、FTP、设备和训练目标。
2. 在 `plans/library/` 维护可复用训练课模板。
3. 在 `plans/2026/week-21/weekly-plan.md` 写本周训练安排，每日计划引用 `plans/library/` 中的模板。
4. 如果 AI 生成了新的训练课，先新增到 `plans/library/` 对应分类，再在周计划中引用。
5. 将 FIT 文件放入 `training/2026/week-21/fit/`。
6. 使用 `scripts/analyze_fit.py` 生成训练分析笔记到 `training/2026/week-21/notes/`。
7. 将关键截图放入 `training/2026/week-21/screenshots/`。
8. 在 `review/2026/week-21-review.md` 完成本周复盘。

## 计划复用

`plans/library/` 是训练课模板库，按训练类型分类：

- `endurance/`: Z2、有氧基础、长距离耐力。
- `recovery/`: 恢复骑、休息日、低压力活动。
- `intervals/`: 甜区、阈值、VO2max、冲刺等间歇。
- `testing/`: FTP、心率阈值、基准测试和测试准备。
- `strength/`: 力量、灵活性、核心训练。

周计划不复制完整训练课内容，只引用模板。例如：

```markdown
| 周二 | [Z2 有氧基础骑](../../library/endurance/z2-base-ride.md) | 有氧容量 | training/2026/week-21/notes/ |  |
```

## FIT 分析

本仓库已经加入项目技能说明，见 `skills/cycling-lab/SKILL.md`。如果本机已安装 Codex 的 `cycling-fit-analysis` skill，可直接运行：

```bash
python3 scripts/analyze_fit.py training/2026/week-21/fit/activity.fit \
  --age 31 \
  --height-cm 176 \
  --weight-kg 70 \
  --out-dir training/2026/week-21/notes
```
