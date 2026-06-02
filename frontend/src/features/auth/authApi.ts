import { z } from "zod";
import { api } from "@/lib/api";
import type { UserInfo } from "@/features/auth/authStore";

export const loginSchema = z.object({
  email: z.string().email("请输入合法邮箱"),
  password: z.string().min(6, "至少 6 位"),
});
export type LoginInput = z.infer<typeof loginSchema>;

export const registerSchema = loginSchema.extend({
  displayName: z.string().min(2, "至少 2 个字符").max(100),
  password: z.string().min(8, "至少 8 位"),
});
export type RegisterInput = z.infer<typeof registerSchema>;

const tokenResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  expiresIn: z.number(),
  user: z.object({
    id: z.string(),
    email: z.string(),
    displayName: z.string(),
    role: z.enum(["USER", "ADMIN"]),
  }),
});

export type TokenResponse = z.infer<typeof tokenResponseSchema>;

export async function login(input: LoginInput): Promise<TokenResponse> {
  const raw = await api<unknown>("/auth/login", {
    method: "POST",
    body: input,
    skipAuth: true,
  });
  return tokenResponseSchema.parse(raw);
}

export async function register(input: RegisterInput): Promise<TokenResponse> {
  const raw = await api<unknown>("/auth/register", {
    method: "POST",
    body: input,
    skipAuth: true,
  });
  return tokenResponseSchema.parse(raw);
}

export async function fetchMe(): Promise<UserInfo> {
  const raw = await api<unknown>("/auth/me");
  return z
    .object({
      id: z.string(),
      email: z.string(),
      displayName: z.string(),
      role: z.enum(["USER", "ADMIN"]),
    })
    .parse(raw);
}
