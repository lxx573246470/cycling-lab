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
- Before analysis, use `$normalize-fit-filenames` to rename the raw file to a stable date-based filename under `training/YYYY/week-NN/fit/`.
- Write generated notes to `training/YYYY/week-NN/notes/`.
- Link the normalized source FIT from the generated note.
- Pass known profile values from `profile/rider-profile.md` to the analyzer: age, height, weight, max heart rate, FTP, and athlete name.
- If the user asks for a completed workout summary and has not provided recovery context, ask them to provide: 日期, HRV, 静息心率, 睡眠时长/质量, 主观疲劳 1-10, 腿部感觉, 今天是否有训练计划, and 备注（饮酒、熬夜、生病、压力、炎热、脱水等）. Use these as optional context for fatigue/readiness interpretation alongside the FIT data and recent training load.
- After generating the note, summarize the ride in chat with the note path, the 10-point training-goal match score, primary intensity, plan-stage execution, heart-rate drift, cadence/power stability, and the next training implication.
- If screenshots are provided with the FIT file, store them under `training/YYYY/week-NN/screenshots/` and link them from the note when useful.
- After adding or changing training notes with TSS or power data, refresh `training/load-summary.csv`:

```bash
python3 scripts/update_training_load.py
```

Training-load policy: TSS, IF, and NP/FTP use the FTP that applied at the time of the activity. Do not recalculate historical TSS after FTP changes; record the FTP used in `training/load-summary.csv`.

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

When creating a new `plans/YYYY/week-NN/weekly-plan.md`, also create the matching training directories before finishing:

```text
training/YYYY/week-NN/fit/
training/YYYY/week-NN/notes/
training/YYYY/week-NN/screenshots/
```

Use the same ISO year and week number as the weekly plan. Verify the directories exist even when the weekly plan is created before any training files are available.
Add a `.gitkeep` file to each empty directory so the weekly training structure remains present after a Git commit. Remove the placeholder only if the repository convention requires it when real files are added.

Minimum planning context:

- `profile/rider-profile.md` for current FTP, max heart rate, equipment, constraints, known fit/body issues, and the rider's short-, medium-, and long-term training goals.
- The current week plan at `plans/YYYY/week-NN/weekly-plan.md`, including both planned workouts and the `实际记录` column.
- The specific daily plan being created or changed, if it already exists under `plans/YYYY/week-NN/`.
- The latest 3-5 training notes from `training/YYYY/week-NN/notes/` and, when needed, the previous week notes too.
- The latest `review/YYYY/week-NN-review.md` when available.
- Any recent fitting, injury, fatigue, schedule, or equipment notes that are linked from the weekly plan or latest review.

When planning, explicitly reconcile planned vs actual training. If the user skipped, shortened, downgraded, or substituted a recent workout, adjust the next 1-3 days rather than blindly preserving the original sequence. Account for current FTP, max heart rate, recent fatigue, heart-rate drift, cadence/power stability, target event goals, available weekday time, weekend availability, and recovery from the last few sessions.

When available, include HRV, resting heart rate, sleep quality, subjective fatigue, leg feel, illness/stress/heat/dehydration notes, and whether the user already has a training plan today in readiness decisions. Treat HRV as a trend against the rider's own baseline, not as a single-day pass/fail metric.

Do not "make up" missed intensity by stacking it onto the next day. If recent training shows high fatigue, weak muscle activation, unusual pain, poor sleep, overtime work, or high heart-rate response, reduce duration, intensity, or complexity and document the reason in the daily plan.

### Goal Alignment

Weekly plans must explicitly connect the next 7 days to `profile/rider-profile.md` goals, not just to recent fatigue.

- Extract the rider's stated `近期目标`, `中期目标`, and `长期目标` before choosing weekly structure.
- If the medium-term goal includes long-distance or climbing targets, include a clear progression mechanism in the weekly plan, such as weekend long-ride duration/distance/elevation progression, fueling practice, heat adaptation, pacing discipline, or climbing cadence work.
- Do not let a recent bad ride automatically turn the whole week into low-risk recovery if that would ignore the rider's goal and preferences. Instead, preserve goal-relevant work where reasonable and add downgrade gates for fatigue, heat, heart-rate drift, pain, or poor sleep.
- In every weekly plan, include a short section or bullets explaining how the week serves the profile goals, for example "200 km progression", "climbing durability", "FTP development", or "weight-management aerobic volume".
- If a plan is deliberately conservative despite an ambitious profile goal, state the reason and what target-specific work resumes next.

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

#### ERG Power Command Compensation

Treat the training target and the ZWO ERG command as separate values when `profile/rider-profile.md` defines an ERG compensation factor.

- Keep the daily/weekly plan's prescribed watts and FTP percentages as the physiological training target. Do not silently redefine FTP or claim that the rider's target intensity increased.
- Apply the profile compensation to main work intervals and sustained endurance platform segments. Leave warmup/cooldown ramps, recovery valleys, FTP tests, sprints, free rides, and outdoor workouts uncompensated unless the profile explicitly says otherwise.
- Calculate the integer command watts first, then derive the ZWO fraction:

```text
zwo_command_watts = round(training_target_watts * (1 + compensation_rate))
zwo_power_fraction = zwo_command_watts / ftp
```

- Write `Power` or `OnPower` from `zwo_power_fraction`, normally to three decimal places. Keep `OffPower` uncompensated when it is a recovery valley.
- Example with a 2% profile compensation and FTP 201 W: prescribe 150 W in the plan, write 153 W in the ZWO file, and use `Power="0.761"` because `153 / 201 = 0.761` after rounding.
- Add an `ERG 指令补偿` line to the daily plan's `## Zwift 文件` section. Show both the training target and compensated ZWO command for each distinct main-work power so the difference is auditable.
- When analyzing the completed workout, compare actual power with the physiological training target, not the compensated command. Also record the command value separately when evaluating whether the compensation is calibrated.
- Do not stack this device compensation on top of an ad hoc workout-intensity increase. Apply adjustment or downgrade rules to the training target first, then calculate the compensated ZWO command once.

When the profile has no current compensation value, or when the trainer calibration, firmware, power source, or connection path changed, estimate a new value only from reliable completed ERG data:

- Use at least 6 complete constant-power main-work segments across at least 3 sessions.
- Include segments at least 3 minutes long with stable cadence, correct plan alignment, and no dropout, manual override, ERG spiral-of-death, or obvious execution failure.
- Exclude ramps, cooldowns, recovery valleys, sprints, FTP tests, free rides, truncated segments, and segments limited by fatigue rather than trainer control.
- Use the median of `actual_average_power / zwo_command_power` as the robust center, invert it to obtain the correction, round to the nearest 1 percentage point, and cap the initial automatic compensation at 3%.
- Recheck after 3-5 further qualified sessions. Change the stored rate only when the residual direction is consistent across sessions; do not react to a single workout.

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
