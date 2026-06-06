import { z } from "zod";
import { api } from "@/lib/api";

export const reviewScopeSchema = z.enum(["WEEK", "PHASE"]);
export type ReviewScope = z.infer<typeof reviewScopeSchema>;

export const reviewDtoSchema = z.object({
  id: z.string().uuid(),
  scope: reviewScopeSchema,
  scopeId: z.string().uuid().nullable().optional(),
  isoYear: z.number().int().nullable().optional(),
  isoWeek: z.number().int().nullable().optional(),
  periodStart: z.string().nullable().optional(),
  periodEnd: z.string().nullable().optional(),
  title: z.string(),
  contentMd: z.string(),
  metrics: z.any().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
});
export type ReviewDto = z.infer<typeof reviewDtoSchema>;

export const reviewCreateSchema = z.object({
  scope: reviewScopeSchema,
  scopeId: z.string().uuid().optional().nullable(),
  isoYear: z.number().int().optional().nullable(),
  isoWeek: z.number().int().optional().nullable(),
  periodStart: z.string().optional().nullable(),
  periodEnd: z.string().optional().nullable(),
  title: z.string().min(1).max(200),
  contentMd: z.string().max(100_000),
  metrics: z.any().optional().nullable(),
});
export type ReviewCreateInput = z.infer<typeof reviewCreateSchema>;

export const reviewUpdateSchema = z.object({
  isoYear: z.number().int(),
  isoWeek: z.number().int(),
  periodStart: z.string().optional().nullable(),
  periodEnd: z.string().optional().nullable(),
  title: z.string().min(1).max(200),
  contentMd: z.string().max(100_000),
  metrics: z.any().optional().nullable(),
});
export type ReviewUpdateInput = z.infer<typeof reviewUpdateSchema>;

export const reviewApi = {
  list: (params: { page?: number; size?: number } = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null) qs.set(k, String(v));
    });
    const suffix = qs.toString() ? `?${qs.toString()}` : "";
    return api<{
      content: ReviewDto[];
      page: number;
      size: number;
      totalElements: number;
      totalPages: number;
    }>(`/reviews${suffix}`);
  },
  get: (id: string) => api<ReviewDto>(`/reviews/${id}`),
  byWeek: (isoYear: number, isoWeek: number) =>
    api<ReviewDto>(`/reviews/by-week?isoYear=${isoYear}&isoWeek=${isoWeek}`),
  create: (body: ReviewCreateInput) =>
    api<ReviewDto>("/reviews", { method: "POST", body }),
  update: (id: string, body: ReviewUpdateInput) =>
    api<ReviewDto>(`/reviews/${id}`, { method: "PUT", body }),
  delete: (id: string) =>
    api<void>(`/reviews/${id}`, { method: "DELETE" }),
};