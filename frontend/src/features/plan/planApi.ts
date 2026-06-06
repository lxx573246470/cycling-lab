import { z } from "zod";
import { api } from "@/lib/api";

// ---------- week helpers -------------------------------------------------

/** Jan 4 is always in ISO week 1; we walk back to the Monday of that week. */
function mondayOfIsoWeek1(year: number): Date {
  const jan4 = new Date(Date.UTC(year, 0, 4));
  // 0 = Sun, 1 = Mon, ... shift so Monday=0
  const day = jan4.getUTCDay();
  const shiftToMonday = ((day + 6) % 7);
  return new Date(Date.UTC(jan4.getUTCFullYear(), jan4.getUTCMonth(), jan4.getUTCDate() - shiftToMonday));
}

export function isoDatesOf(year: number, week: number): string[] {
  if (week < 1 || week > 53) {
    throw new Error(`isoWeek out of range: ${week}`);
  }
  const monday = mondayOfIsoWeek1(year);
  const target = new Date(Date.UTC(monday.getUTCFullYear(), monday.getUTCMonth(), monday.getUTCDate() + (week - 1) * 7));
  // sanity check the year of the resulting Monday
  const backYear = isoWeekBasedYear(target);
  if (backYear !== year) {
    throw new Error(`isoWeek ${week} does not exist in year ${year}`);
  }
  const out: string[] = [];
  for (let i = 0; i < 7; i++) {
    const d = new Date(Date.UTC(target.getUTCFullYear(), target.getUTCMonth(), target.getUTCDate() + i));
    out.push(toIsoDate(d));
  }
  return out;
}

export function currentIsoWeek(now: Date = new Date()): { year: number; week: number } {
  return { year: isoWeekBasedYear(now), week: isoWeekOfYear(now) };
}

function toIsoDate(d: Date): string {
  // YYYY-MM-DD
  return d.toISOString().slice(0, 10);
}

function isoWeekBasedYear(d: Date): number {
  // Move the date to the nearest Thursday in the same ISO year.
  const thursday = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() + 3 - ((d.getUTCDay() + 6) % 7)));
  return thursday.getUTCFullYear();
}

function isoWeekOfYear(d: Date): number {
  // ISO week number = the ordinal week of the Thursday in the same ISO year.
  const thursday = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() + 3 - ((d.getUTCDay() + 6) % 7)));
  const jan1 = new Date(Date.UTC(thursday.getUTCFullYear(), 0, 1));
  return Math.floor((thursday.getTime() - jan1.getTime()) / (7 * 24 * 3600 * 1000)) + 1;
}

const WEEKDAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"] as const;
export function weekdayLabel(weekday: number): string {
  // ISO weekday 1..7 = Mon..Sun
  if (weekday < 1 || weekday > 7 || !Number.isInteger(weekday)) return '?';
  return WEEKDAY_LABELS[weekday - 1] ?? "?";
}

export function formatWeekRange(weekStart: string, weekEnd: string): string {
  // weekStart / weekEnd are YYYY-MM-DD
  return `${weekStart} – ${weekEnd}`;
}

// ---------- schemas + types ---------------------------------------------

export const DAILY_STATUSES = ["PLANNED", "DONE", "PARTIAL", "SKIPPED", "RESCHEDULED"] as const;
export type DailyStatus = (typeof DAILY_STATUSES)[number];

export const dailyStatusSchema = z.enum(DAILY_STATUSES);

export const dailyPlanSchema = z.object({
  id: z.string().uuid(),
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  weekday: z.number().int().min(1).max(7),
  targetText: z.string().max(4000).nullable().optional(),
  templateId: z.string().uuid().nullable().optional(),
  templateVersion: z.number().int().nullable().optional(),
  templateName: z.string().nullable().optional(),
  notesMd: z.string().max(20000).nullable().optional(),
  status: dailyStatusSchema,
  actualSessionId: z.string().uuid().nullable().optional(),
  updatedAt: z.string(),
});

export type DailyPlan = z.infer<typeof dailyPlanSchema>;

export const weeklyPlanProgressSchema = z.object({
  total: z.number().int(),
  planned: z.number().int(),
  done: z.number().int(),
  partial: z.number().int(),
  skipped: z.number().int(),
  rescheduled: z.number().int(),
});
export type WeeklyPlanProgress = z.infer<typeof weeklyPlanProgressSchema>;

export const weeklyPlanDtoSchema = z.object({
  id: z.string().uuid(),
  isoYear: z.number().int(),
  isoWeek: z.number().int(),
  weekStart: z.string(),
  weekEnd: z.string(),
  title: z.string().nullable().optional(),
  goalMd: z.string().nullable().optional(),
  days: z.array(dailyPlanSchema),
  progress: weeklyPlanProgressSchema,
  createdAt: z.string(),
  updatedAt: z.string(),
});
export type WeeklyPlanDto = z.infer<typeof weeklyPlanDtoSchema>;

export const weeklyPlanSummarySchema = z.object({
  id: z.string().uuid(),
  isoYear: z.number().int(),
  isoWeek: z.number().int(),
  title: z.string().nullable().optional(),
  progress: weeklyPlanProgressSchema,
  updatedAt: z.string(),
});
export type WeeklyPlanSummary = z.infer<typeof weeklyPlanSummarySchema>;

export const pageResponseSchema = <T extends z.ZodTypeAny>(item: T) =>
  z.object({
    content: z.array(item),
    page: z.number().int(),
    size: z.number().int(),
    totalElements: z.number().int(),
    totalPages: z.number().int(),
  });

export const weeklyPlanCreateSchema = z.object({
  isoYear: z.number().int().min(2000).max(2100),
  isoWeek: z.number().int().min(1).max(53),
  title: z.string().max(128).optional().nullable(),
  goalMd: z.string().max(20000).optional().nullable(),
});
export type WeeklyPlanCreateInput = z.infer<typeof weeklyPlanCreateSchema>;

export const weeklyPlanUpdateSchema = z.object({
  title: z.string().max(128).optional().nullable(),
  goalMd: z.string().max(20000).optional().nullable(),
});
export type WeeklyPlanUpdateInput = z.infer<typeof weeklyPlanUpdateSchema>;

export const dailyPlanUpdateSchema = z.object({
  targetText: z.string().max(4000).optional().nullable(),
  // templateIdPresent is a "sentinel" flag: when true we clear the
  // template reference; when false / undefined we leave it alone (or set it
  // via the next field).
  templateIdPresent: z.boolean().optional(),
  templateId: z.string().uuid().optional().nullable(),
  templateVersion: z.number().int().optional().nullable(),
  notesMd: z.string().max(20000).optional().nullable(),
  status: dailyStatusSchema.optional(),
});
export type DailyPlanUpdateInput = z.infer<typeof dailyPlanUpdateSchema>;

// ---------- API client ---------------------------------------------------

export const planApi = {
  list: (params: { page?: number; size?: number } = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null) qs.set(k, String(v));
    });
    const suffix = qs.toString() ? `?${qs.toString()}` : "";
    return api<{ content: WeeklyPlanSummary[]; page: number; size: number; totalElements: number; totalPages: number }>(
      `/plans/weeks${suffix}`,
    );
  },
  get: (id: string) => api<WeeklyPlanDto>(`/plans/weeks/${id}`),
  create: (body: WeeklyPlanCreateInput) =>
    api<WeeklyPlanDto>("/plans/weeks", { method: "POST", body }),
  update: (id: string, body: WeeklyPlanUpdateInput) =>
    api<WeeklyPlanDto>(`/plans/weeks/${id}`, { method: "PUT", body }),
  delete: (id: string) => api<void>(`/plans/weeks/${id}`, { method: "DELETE" }),
  updateDay: (weekId: string, dayId: string, body: DailyPlanUpdateInput) =>
    api<DailyPlan>(`/plans/weeks/${weekId}/days/${dayId}`, { method: "PUT", body }),
};

export function summariseProgress(p: WeeklyPlanProgress): string {
  if (p.total === 0) return "no days";
  if (p.done === p.total) return `done ${p.done}/${p.total}`;
  const parts: string[] = [];
  if (p.done) parts.push(`${p.done} done`);
  if (p.partial) parts.push(`${p.partial} partial`);
  if (p.planned) parts.push(`${p.planned} planned`);
  if (p.skipped) parts.push(`${p.skipped} skipped`);
  if (p.rescheduled) parts.push(`${p.rescheduled} rescheduled`);
  return parts.join(" · ");
}