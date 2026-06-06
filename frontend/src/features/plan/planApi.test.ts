import { describe, it, expect } from "vitest";
import {
  currentIsoWeek,
  DAILY_STATUSES,
  formatWeekRange,
  isoDatesOf,
  planApi,
  summariseProgress,
  weekdayLabel,
  weeklyPlanCreateSchema,
  type WeeklyPlanProgress,
} from "./planApi";

describe("isoDatesOf", () => {
  it("returns 7 consecutive days starting Monday for 2026 W24", () => {
    // 2026-01-04 is a Sunday, so the Monday of W1 is 2025-12-29
    // W24 = 2025-12-29 + 23 * 7 days = 2026-06-08 (Mon)
    const dates = isoDatesOf(2026, 24);
    expect(dates).toHaveLength(7);
    expect(dates[0]).toBe("2026-06-08");
    expect(dates[6]).toBe("2026-06-14");
  });

  it("returns 7 consecutive days for 2026 W01", () => {
    // 2026-01-04 is a Sunday; Monday of W1 is 2025-12-29
    const dates = isoDatesOf(2026, 1);
    expect(dates[0]).toBe("2025-12-29");
    expect(dates[6]).toBe("2026-01-04");
  });

  it("handles 2020 which has 53 weeks", () => {
    const dates = isoDatesOf(2020, 53);
    expect(dates[0]).toBe("2020-12-28");
    expect(dates[6]).toBe("2021-01-03");
  });

  it("rejects week 53 in a 52-week year (2025)", () => {
    expect(() => isoDatesOf(2025, 53)).toThrow();
  });

  it("rejects week 0 or 54", () => {
    expect(() => isoDatesOf(2026, 0)).toThrow();
    expect(() => isoDatesOf(2026, 54)).toThrow();
  });
});

describe("currentIsoWeek", () => {
  it("returns ISO (2026, 24) for 2026-06-10 (a Wednesday)", () => {
    const d = new Date(Date.UTC(2026, 5, 10)); // June 10, 2026
    const { year, week } = currentIsoWeek(d);
    expect(year).toBe(2026);
    expect(week).toBe(24);
  });

  it("returns ISO (2026, 1) for 2026-01-01 (a Thursday)", () => {
    const d = new Date(Date.UTC(2026, 0, 1));
    const { year, week } = currentIsoWeek(d);
    expect(year).toBe(2026);
    expect(week).toBe(1);
  });
});

describe("weekdayLabel", () => {
  it("maps ISO 1..7 to Mon..Sun", () => {
    expect(weekdayLabel(1)).toBe("Mon");
    expect(weekdayLabel(7)).toBe("Sun");
  });
  it("falls back to ? for out-of-range", () => {
    expect(weekdayLabel(0)).toBe("?");
    expect(weekdayLabel(8)).toBe("?");
  });
});

describe("formatWeekRange", () => {
  it("joins start and end with an en-dash", () => {
    expect(formatWeekRange("2026-06-08", "2026-06-14")).toBe("2026-06-08 \u2013 2026-06-14");
  });
});

describe("summariseProgress", () => {
  const empty: WeeklyPlanProgress = { total: 0, planned: 0, done: 0, partial: 0, skipped: 0, rescheduled: 0 };
  it("returns 'no days' for empty progress", () => {
    expect(summariseProgress(empty)).toBe("no days");
  });
  it("returns 'done n/n' when all done", () => {
    expect(summariseProgress({ ...empty, total: 7, done: 7 })).toBe("done 7/7");
  });
  it("lists each non-zero status", () => {
    expect(
      summariseProgress({ total: 5, planned: 2, done: 1, partial: 1, skipped: 1, rescheduled: 0 }),
    ).toBe("1 done · 1 partial · 2 planned · 1 skipped");
  });
});

describe("weeklyPlanCreateSchema", () => {
  it("accepts the canonical inputs", () => {
    const parsed = weeklyPlanCreateSchema.parse({
      isoYear: 2026, isoWeek: 24, title: "Build", goalMd: "x"
    });
    expect(parsed.isoYear).toBe(2026);
  });
  it("rejects week 0 or 54", () => {
    expect(() => weeklyPlanCreateSchema.parse({ isoYear: 2026, isoWeek: 0 })).toThrow();
    expect(() => weeklyPlanCreateSchema.parse({ isoYear: 2026, isoWeek: 54 })).toThrow();
  });
  it("rejects year < 2000", () => {
    expect(() => weeklyPlanCreateSchema.parse({ isoYear: 1999, isoWeek: 24 })).toThrow();
  });
});

describe("DAILY_STATUSES", () => {
  it("lists the 5 expected statuses in canonical order", () => {
    expect(DAILY_STATUSES).toEqual(["PLANNED", "DONE", "PARTIAL", "SKIPPED", "RESCHEDULED"]);
  });
});

describe("planApi object shape", () => {
  it("exposes the five endpoints", () => {
    expect(typeof planApi.list).toBe("function");
    expect(typeof planApi.get).toBe("function");
    expect(typeof planApi.create).toBe("function");
    expect(typeof planApi.update).toBe("function");
    expect(typeof planApi.delete).toBe("function");
    expect(typeof planApi.updateDay).toBe("function");
  });
});