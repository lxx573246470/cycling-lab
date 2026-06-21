import { z } from "zod";
import { api } from "@/lib/api";
import { useAuthStore } from "@/features/auth/authStore";

export const workoutFileFormatSchema = z.enum(["ZWO", "ERG", "MRC", "ZML"]);
export type WorkoutFileFormat = z.infer<typeof workoutFileFormatSchema>;

export const workoutFileSummarySchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  sportType: z.string(),
  tags: z.array(z.string()),
  format: workoutFileFormatSchema,
  sizeBytes: z.number().int(),
  sourceTemplateId: z.string().uuid().nullable().optional(),
  createdAt: z.string(),
});
export type WorkoutFileSummary = z.infer<typeof workoutFileSummarySchema>;

export const workoutFileDtoSchema = workoutFileSummarySchema.extend({
  description: z.string().nullable().optional(),
  xml: z.string(),
  updatedAt: z.string(),
});
export type WorkoutFileDto = z.infer<typeof workoutFileDtoSchema>;

export const workoutFileCreateSchema = z.object({
  name: z.string().min(1).max(128),
  sportType: z.enum(["bike", "run", "row"]).optional(),
  description: z.string().max(20000).optional().nullable(),
  tags: z.array(z.string().min(1).max(32)).max(16).optional(),
  sourceTemplateId: z.string().uuid().optional().nullable(),
  structureJson: z.string().min(2),
});
export type WorkoutFileCreateInput = z.infer<typeof workoutFileCreateSchema>;

export const pageResponseSchema = <T extends z.ZodTypeAny>(item: T) =>
  z.object({
    content: z.array(item),
    page: z.number().int(),
    size: z.number().int(),
    totalElements: z.number().int(),
    totalPages: z.number().int(),
  });

const API_BASE = import.meta.env.VITE_API_BASE ?? "/api/v1";

export const workoutFileApi = {
  list: (params: { page?: number; size?: number } = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null) qs.set(k, String(v));
    });
    const suffix = qs.toString() ? `?${qs.toString()}` : "";
    return api<{ content: WorkoutFileSummary[]; page: number; size: number; totalElements: number; totalPages: number }>(
      `/workout-files${suffix}`,
    );
  },
  get: (id: string) => api<WorkoutFileDto>(`/workout-files/${id}`),
  create: (body: WorkoutFileCreateInput) =>
    api<WorkoutFileDto>("/workout-files", { method: "POST", body }),
  delete: (id: string) => api<void>(`/workout-files/${id}`, { method: "DELETE" }),
  /**
   * Browser-only download: fetch the raw bytes with the existing auth header
   * (the api() helper injects it), then trigger a save dialog.
   */
  async download(id: string, fileName: string): Promise<void> {
    const res = await fetch(`${API_BASE}/workout-files/${id}/download`, {
      headers: { Authorization: `Bearer ${getAccessToken()}` },
    });
    if (!res.ok) {
      throw new Error(`Download failed: ${res.status}`);
    }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  },
};

function getAccessToken(): string {
  return useAuthStore.getState().accessToken ?? "";
}

export function formatBytes(n: number): string {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(2)} MB`;
}

export function sportTypeLabel(sportType: string): string {
  const labels: Record<string, string> = {
    bike: "骑行",
    run: "跑步",
    row: "划船",
  };
  return labels[sportType] ?? sportType;
}
