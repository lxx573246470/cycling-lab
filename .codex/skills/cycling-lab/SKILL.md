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

When analyzing FIT files, prefer the project-local `skills/cycling-fit-analysis` skill. From this repository, call:

```bash
python3 scripts/analyze_fit.py training/2026/week-21/fit/activity.fit \
  --age 31 \
  --height-cm 176 \
  --weight-kg 70 \
  --out-dir training/2026/week-21/notes
```

Use rider data from `profile/rider-profile.md` when available. If max heart rate or FTP is unknown, label zone guidance as estimated instead of definitive.

For uploaded FIT files:

- Determine the activity date from the FIT session timestamp when possible; otherwise use the current local date.
- Store the raw file under `training/YYYY/week-NN/fit/` using a stable date-based filename if the upload name is unclear.
- Write generated notes to `training/YYYY/week-NN/notes/`.
- Pass known profile values from `profile/rider-profile.md` to the analyzer: age, height, weight, max heart rate, FTP, and athlete name.
- After generating the note, summarize the ride in chat with the note path, the 10-point training-goal match score, primary intensity, plan-stage execution, heart-rate drift, cadence/power stability, and the next training implication.
- If screenshots are provided with the FIT file, store them under `training/YYYY/week-NN/screenshots/` and link them from the note when useful.

Preferred command shape:

```bash
python3 scripts/analyze_fit.py training/YYYY/week-NN/fit/activity.fit \
  --athlete-name lxx \
  --age 31 \
  --height-cm 176 \
  --weight-kg 70 \
  --max-hr 203 \
  --ftp 200 \
  --out-dir training/YYYY/week-NN/notes
```

## Plan Reuse

When creating or editing daily/weekly plans:

- Search `plans/library/` for an existing reusable plan first.
- If a suitable plan exists, reference it from the weekly plan with a relative Markdown link.
- If no suitable plan exists, create a new reusable plan under the right category, then reference it from the weekly plan.
- Do not duplicate full workout details inside weekly plans.
- If a workout needs a Zwift import file, place the `.zwo` file in `workouts/zwo/` and link it from the reusable plan.

## Plan Generation

Before generating or changing plans, read enough recent context to make the next workout fit the current week instead of treating it as an isolated template.

Minimum planning context:

- `profile/rider-profile.md` for current FTP, max heart rate, goals, equipment, constraints, and known fit or body issues.
- The current week plan at `plans/YYYY/week-NN/weekly-plan.md`, including both planned workouts and the `实际记录` column.
- The specific daily plan being created or changed, if it already exists under `plans/YYYY/week-NN/`.
- The latest 3-5 training notes from `training/YYYY/week-NN/notes/` and, when needed, the previous week notes too.
- The latest `review/YYYY/week-NN-review.md` when available.
- Any recent fitting, injury, fatigue, schedule, or equipment notes that are linked from the weekly plan or latest review.

When planning, explicitly reconcile planned vs actual training. If the user skipped, shortened, downgraded, or substituted a recent workout, adjust the next 1-3 days rather than blindly preserving the original sequence. Account for current FTP, max heart rate, recent fatigue, heart-rate drift, cadence/power stability, target event goals, available weekday time, weekend availability, and recovery from the last few sessions.

Do not "make up" missed intensity by stacking it onto the next day. If recent training shows high fatigue, weak muscle activation, unusual pain, poor sleep, overtime work, or high heart-rate response, reduce duration, intensity, or complexity and document the reason in the daily plan.

Support two plan types:

### Zwift / Indoor Power Workouts

Use for weekday structured training, any indoor-trainer plan, any Zwift plan, or any request for precise power control.

- Create or reuse a Markdown workout plan under `plans/library/` or `plans/YYYY/week-NN/`.
- For every indoor-trainer, indoor cycling, power-based, or Zwift workout plan, create a matching `.zwo` file under `workouts/zwo/`.
- Add `workout_file: "../../../workouts/zwo/<filename>.zwo"` to the daily plan frontmatter when the plan is under `plans/YYYY/week-NN/`.
- Include a `## Zwift 文件` section in the Markdown plan with the ZWO link, FTP reference value, and total duration.
- Prescribe intervals by FTP percentage and duration; include cadence targets only when useful.
- Include warmup, main set, cooldown, target purpose, adjustment rules, and expected RPE/heart-rate response.
- Keep weekly-plan entries concise and link to the detailed plan plus the ZWO file.
- If the user says "室内", "骑行台", "Zwift", "zwo", or "智能骑行台", treat the plan as a Zwift/Indoor Power Workout unless they explicitly say not to create a ZWO file.

### Weekend Outdoor Road Rides

Use for weekend rides, endurance rides, long routes, climbing practice, or any ride where terrain and execution cannot be controlled precisely.

- Recommend route type: flat, rolling, hilly/mountain, or mixed.
- Prescribe target distance, elevation gain, expected duration, intensity cap, and bailout option.
- Give control guidance as ranges, not exact intervals: heart-rate zones, power ranges as FTP percentage, RPE, and cadence where relevant.
- Separate guidance for flats, rollers, climbs, descents, and final hour if the ride is long.
- Include fueling and hydration plan:
  - Carbohydrate target in g/hour, with examples using gels, drink mix, bars, or normal food.
  - Fluid target in ml/hour, adjusted for heat when known.
  - Sodium target in mg/hour or salt-tablet timing when relevant.
  - Caffeine guidance only if the user asks or the ride is long enough to justify it.
- Include pre-ride meal timing, on-bike fueling schedule, emergency reserve, and post-ride recovery.
- Prefer conservative progression after hard weeks or high heart-rate drift. For recovery weeks, reduce either distance, elevation, or intensity rather than stacking all three.

Outdoor ride output should be usable without a route file. Minimum sections:

- Ride objective
- Route choice
- Distance/elevation/duration target
- Intensity control
- Terrain execution
- Fueling and hydration
- Bailout and adjustment rules
- What data to review afterward

## Note Style

Training notes should be Markdown with YAML frontmatter, stable headings, and practical coaching observations:

- Immediate observations
- Risks and opportunities
- Next 1-4 weeks
- Links to the relevant FIT file and screenshots when available
