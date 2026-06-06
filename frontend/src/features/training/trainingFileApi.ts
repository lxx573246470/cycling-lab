import { z } from "zod";
import { api } from "@/lib/api";
import { useAuthStore } from "@/features/auth/authStore";

const API_BASE = import.meta.env.VITE_API_BASE ?? "/api/v1";

export const trainingFileStatusSchema = z.enum([
  "PENDING",
  "PARSING",
  "READY",
  "FAILED",
]);
export type TrainingFileStatus = z.infer<typeof trainingFileStatusSchema>;

export const trainingFileSummarySchema = z.object({
  id: z.string().uuid(),
  isoYear: z.number().int(),
  isoWeek: z.number().int(),
  originalFilename: z.string(),
  sportType: z.string(),
  sizeBytes: z.number().int(),
  status: trainingFileStatusSchema,
  recordedAt: z.string().nullable().optional(),
  createdAt: z.string().nullable().optional(),
});
export type TrainingFileSummary = z.infer<typeof trainingFileSummarySchema>;

export const trainingSessionSummarySchema = z.object({
  id: z.string().uuid(),
  startedAt: z.string().nullable().optional(),
  durationSec: z.number().int().nullable().optional(),
  distanceM: z.number().nullable().optional(),
  energyKj: z.number().nullable().optional(),
  avgHr: z.number().int().nullable().optional(),
  maxHr: z.number().int().nullable().optional(),
  avgPower: z.number().int().nullable().optional(),
  maxPower: z.number().int().nullable().optional(),
  normalizedPower: z.number().int().nullable().optional(),
  intensityFactor: z.number().nullable().optional(),
  trainingStressScore: z.number().nullable().optional(),
  avgCadence: z.number().int().nullable().optional(),
  maxCadence: z.number().int().nullable().optional(),
  hrDriftPct: z.number().nullable().optional(),
  hrZoneDistribution: z.array(z.any()).nullable().optional(),
  powerZoneDistribution: z.array(z.any()).nullable().optional(),
  cadenceZoneDistribution: z.array(z.any()).nullable().optional(),
  tenMinSegments: z.array(z.any()).nullable().optional(),
  bestRolling: z.array(z.any()).nullable().optional(),
});
export type TrainingSessionSummary = z.infer<typeof trainingSessionSummarySchema>;

export const trainingFileDetailSchema = trainingFileSummarySchema.extend({
  sha256: z.string(),
  failureMessage: z.string().nullable().optional(),
  updatedAt: z.string(),
  session: trainingSessionSummarySchema.nullable().optional(),
});
export type TrainingFileDetail = z.infer<typeof trainingFileDetailSchema>;

export const trainingSampleSchema = z.object({
  tOffsetSec: z.number().int(),
  hr: z.number().int().nullable().optional(),
  power: z.number().int().nullable().optional(),
  cadence: z.number().int().nullable().optional(),
  speedMps: z.number().nullable().optional(),
  altitudeM: z.number().nullable().optional(),
  lat: z.number().nullable().optional(),
  lon: z.number().nullable().optional(),
});
export type TrainingSample = z.infer<typeof trainingSampleSchema>;

export const samplePageSchema = z.object({
  sessionId: z.string().uuid(),
  content: z.array(trainingSampleSchema),
  page: z.number().int(),
  size: z.number().int(),
  totalElements: z.number().int(),
});
export type SamplePage = z.infer<typeof samplePageSchema>;

export const trainingFileApi = {
  list: (params: { page?: number; size?: number } = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null) qs.set(k, String(v));
    });
    const suffix = qs.toString() ? `?${qs.toString()}` : "";
    return api<{
      content: TrainingFileSummary[];
      page: number;
      size: number;
      totalElements: number;
      totalPages: number;
    }>(`/trainings/files${suffix}`);
  },
  get: (id: string) => api<TrainingFileDetail>(`/trainings/files/${id}`),
  delete: (id: string) =>
    api<void>(`/trainings/files/${id}`, { method: "DELETE" }),
  samples: (id: string, page = 0, size = 200) =>
    api<SamplePage>(`/trainings/files/${id}/samples?page=${page}&size=${size}`),
  /**
   * Multipart upload. The standard {@link api} helper sends JSON; uploads
   * need a FormData payload, so we hit fetch() directly with the bearer
   * token injected from the auth store.
   */
  async upload(file: File): Promise<TrainingFileDetail> {
    const form = new FormData();
    form.append("file", file);
    const res = await fetch(`${API_BASE}/trainings/files`, {
      method: "POST",
      headers: { Authorization: `Bearer ${useAuthStore.getState().accessToken ?? ""}` },
      body: form,
    });
    if (!res.ok) {
      let message = `Upload failed: ${res.status}`;
      try {
        const body = (await res.json()) as { message?: string };
        if (body.message) message = body.message;
      } catch {
        // ignore
      }
      throw new Error(message);
    }
    return (await res.json()) as TrainingFileDetail;
  },
};

export function formatBytes(n: number): string {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(2)} MB`;
}

export function formatDuration(sec: number | null | undefined): string {
  if (!sec || sec <= 0) return "-";
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  return h > 0
    ? `${h}h ${m.toString().padStart(2, "0")}m`
    : `${m}m ${s.toString().padStart(2, "0")}s`;
}

export function formatOffset(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}
