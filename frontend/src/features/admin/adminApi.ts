import { z } from "zod";
import { api } from "@/lib/api";

export const userRoleSchema = z.enum(["USER", "ADMIN"]);
export type UserRole = z.infer<typeof userRoleSchema>;

export const userStatusSchema = z.enum(["ACTIVE", "DISABLED"]);
export type UserStatus = z.infer<typeof userStatusSchema>;

export const adminUserDtoSchema = z.object({
  id: z.string().uuid(),
  email: z.string(),
  displayName: z.string(),
  role: userRoleSchema,
  status: userStatusSchema,
  createdAt: z.string(),
  updatedAt: z.string(),
});
export type AdminUserDto = z.infer<typeof adminUserDtoSchema>;

export const adminUserPatchSchema = z.object({
  role: userRoleSchema,
  status: userStatusSchema,
});
export type AdminUserPatchInput = z.infer<typeof adminUserPatchSchema>;

export const adminApi = {
  list: (params: { q?: string; role?: string; status?: string; page?: number; size?: number } = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") qs.set(k, String(v));
    });
    const suffix = qs.toString() ? `?${qs.toString()}` : "";
    return api<{
      content: AdminUserDto[];
      page: number;
      size: number;
      totalElements: number;
      totalPages: number;
    }>(`/admin/users${suffix}`);
  },
  get: (id: string) => api<AdminUserDto>(`/admin/users/${id}`),
  patch: (id: string, body: AdminUserPatchInput) =>
    api<AdminUserDto>(`/admin/users/${id}`, { method: "PATCH", body }),
  delete: (id: string) =>
    api<void>(`/admin/users/${id}`, { method: "DELETE" }),
};