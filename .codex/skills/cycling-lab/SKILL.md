---
name: cycling-lab
description: Maintain this cycling-lab repository, including rider profile, weekly plans, FIT file analysis notes, screenshots, ZWO workouts, and weekly reviews.
---

# Cycling Lab

## Repository Workflow

Use this repository structure:

- `profile/rider-profile.md`: Long-lived rider profile, goals, thresholds, devices, constraints.
- `plans/library/`: Reusable workout plan library, grouped by category.
- `plans/YYYY/week-NN/weekly-plan.md`: Weekly training plan that references reusable templates.
- `training/YYYY/week-NN/fit/`: Raw `.fit` files.
- `training/YYYY/week-NN/notes/`: Generated or manually edited training notes.
- `training/YYYY/week-NN/screenshots/`: Ride screenshots and charts.
- `review/YYYY/week-NN-review.md`: Weekly review.
- `workouts/zwo/`: Zwift `.zwo` workout files.
- `scripts/`: Project helper scripts.

## FIT Analysis

When analyzing FIT files, prefer the installed global `cycling-fit-analysis` skill. From this repository, call:

```bash
python3 scripts/analyze_fit.py training/2026/week-21/fit/activity.fit \
  --age 31 \
  --height-cm 176 \
  --weight-kg 70 \
  --out-dir training/2026/week-21/notes
```

Use rider data from `profile/rider-profile.md` when available. If max heart rate or FTP is unknown, label zone guidance as estimated instead of definitive.

## Plan Reuse

When creating or editing daily/weekly plans:

- Search `plans/library/` for an existing reusable plan first.
- If a suitable plan exists, reference it from the weekly plan with a relative Markdown link.
- If no suitable plan exists, create a new reusable plan under the right category, then reference it from the weekly plan.
- Do not duplicate full workout details inside weekly plans.
- If a workout needs a Zwift import file, place the `.zwo` file in `workouts/zwo/` and link it from the reusable plan.

## Note Style

Training notes should be Markdown with YAML frontmatter, stable headings, and practical coaching observations:

- Immediate observations
- Risks and opportunities
- Next 1-4 weeks
- Links to the relevant FIT file and screenshots when available
