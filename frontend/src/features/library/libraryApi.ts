import { z } from "zod";
import { api } from "@/lib/api";

export const CATEGORIES = [
  { code: "endurance", label: "Endurance" },
  { code: "recovery", label: "Recovery" },
  { code: "intervals", label: "Intervals" },
  { code: "outdoor", label: "Outdoor" },
  { code: "testing", label: "Testing" },
  { code: "strength", label: "Strength" },
] as const;

export type CategoryCode = (typeof CATEGORIES)[number]["code"];

export const blockSchema = z.union([
  z.object({
    type: z.literal("warmup"),
    durationSec: z.number().int().positive(),
    powerLow: z.number().min(0).max(1.5),
    powerHigh: z.number().min(0).max(1.5),
  }).refine((v) => v.powerLow < v.powerHigh, { message: "powerLow must be < powerHigh" }),
  z.object({
    type: z.literal("steady"),
    durationSec: z.number().int().positive(),
    power: z.number().min(0).max(1.5),
  }),
  z.object({
    type: z.literal("cooldown"),
    durationSec: z.number().int().positive(),
    powerLow: z.number().min(0).max(1.5),
    powerHigh: z.number().min(0).max(1.5),
  }).refine((v) => v.powerLow > v.powerHigh, { message: "cooldown powerLow must be > powerHigh" }),
  z.object({
    type: z.literal("rest"),
    durationSec: z.number().int().positive(),
  }),
  z.object({
    type: z.literal("intervals"),
    repeats: z.number().int().min(1),
    on: z.object({ durationSec: z.number().int().positive(), power: z.number().min(0).max(1.5) }),
    off: z.object({ durationSec: z.number().int().positive(), power: z.number().min(0).max(1.5) }),
  }),
]);
export type Block = z.infer<typeof blockSchema>;

export const structureSchema = z.object({
  blocks: z.array(blockSchema).min(1),
});
export type Structure = z.infer<typeof structureSchema>;

const templateBaseSchema = z.object({
  name: z.string().min(1).max(128),
  category: z.enum(["endurance", "recovery", "intervals", "outdoor", "testing", "strength", "uncategorized"]),
  intensity: z.string().max(32).optional().nullable(),
  descriptionMd: z.string().max(20_000).optional().nullable(),
  structureJson: z.string().min(2),
  tags: z.array(z.string().min(1).max(32)).max(16).default([]),
});

export const templateCreateSchema = templateBaseSchema.refine((v) => {
  try { structureSchema.parse(JSON.parse(v.structureJson)); return true; }
  catch { return false; }
}, { message: "structure_json 校验失败", path: ["structureJson"] });
export type TemplateCreateInput = z.infer<typeof templateCreateSchema>;

export const templatePutSchema = templateBaseSchema.extend({
  changeNote: z.string().max(255).optional().nullable(),
});
export type TemplatePutInput = z.infer<typeof templatePutSchema>;


export const templatePatchSchema = z.object({
  name: z.string().min(1).max(128).optional(),
  category: z.enum(["endurance", "recovery", "intervals", "outdoor", "testing", "strength", "uncategorized"]).optional(),
  intensity: z.string().max(32).optional().nullable(),
  descriptionMd: z.string().max(20_000).optional().nullable(),
  tags: z.array(z.string().min(1).max(32)).max(16).optional(),
  archived: z.boolean().optional(),
});

export interface WorkoutTemplateDto {
  id: string;
  name: string;
  category: string;
  intensity: string | null;
  tags: string[];
  descriptionMd: string | null;
  structureJson: string;
  structure: { blockCount: number; totalDurationSec: number };
  source: "MANUAL" | "IMPORT" | "AI_SUGGESTED";
  archived: boolean;
  currentVersion: number;
  createdAt: string;
  updatedAt: string;
}

export interface WorkoutTemplateListItem {
  id: string;
  name: string;
  category: string;
  intensity: string | null;
  tags: string[];
  blockCount: number;
  totalDurationSec: number;
  archived: boolean;
  updatedAt: string;
}

export interface VersionSummary {
  version: number;
  changeNote: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CategoryDto {
  code: string;
  label: string;
}

export const libraryApi = {
  list: (params: { category?: string; q?: string; tag?: string; archived?: boolean; page?: number; size?: number } = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") qs.set(k, String(v));
    });
    return api<PageResponse<WorkoutTemplateListItem>>(`/library/templates?${qs.toString()}`);
  },
  get: (id: string) => api<WorkoutTemplateDto>(`/library/templates/${id}`),
  create: (body: TemplateCreateInput) =>
    api<WorkoutTemplateDto>("/library/templates", { method: "POST", body }),
  replace: (id: string, body: z.infer<typeof templatePutSchema>) =>
    api<WorkoutTemplateDto>(`/library/templates/${id}`, { method: "PUT", body }),
  patch: (id: string, body: z.infer<typeof templatePatchSchema>) =>
    api<WorkoutTemplateDto>(`/library/templates/${id}`, { method: "PATCH", body }),
  archive: (id: string) => api<void>(`/library/templates/${id}`, { method: "DELETE" }),
  duplicate: (id: string, name?: string) =>
    api<WorkoutTemplateDto>(`/library/templates/${id}/duplicate`, {
      method: "POST",
      body: name ? { name } : {},
    }),
  versions: (id: string) =>
    api<VersionSummary[]>(`/library/templates/${id}/versions`),
  version: (id: string, version: number) =>
    api<{ version: number; structureJson: string; changeNote: string | null; createdAt: string }>(
      `/library/templates/${id}/versions/${version}`
    ),
  categories: () => api<CategoryDto[]>("/library/categories"),
  categoryCounts: () => api<Record<string, number>>("/library/category-counts"),
};

export function parseStructure(structureJson: string): Structure | null {
  try {
    return structureSchema.parse(JSON.parse(structureJson));
  } catch {
    return null;
  }
}

export function totalDurationSec(structure: Structure): number {
  return structure.blocks.reduce((sum, b) => {
    switch (b.type) {
      case "warmup":
      case "steady":
      case "cooldown":
      case "rest":
        return sum + b.durationSec;
      case "intervals":
        return sum + b.repeats * (b.on.durationSec + b.off.durationSec);
    }
  }, 0);
}

export function formatDuration(seconds: number): string {
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const remaining = seconds % 60;
  if (minutes < 60) return remaining === 0 ? `${minutes}m` : `${minutes}m${remaining}s`;
  const hours = Math.floor(minutes / 60);
  const remMin = minutes % 60;
  return remMin === 0 ? `${hours}h` : `${hours}h${remMin}m`;
}
