import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate } from "@tanstack/react-router";
import { loginSchema, registerSchema, type LoginInput, type RegisterInput } from "@/features/auth/authApi";
import { useAuthStore } from "@/features/auth/authStore";
import { ApiError } from "@/lib/api";

export function LoginPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const setSession = useAuthStore((s) => s.setSession);
  const navigate = useNavigate();

  const loginForm = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });
  const registerForm = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: "", displayName: "", password: "" },
  });

  const onLogin = loginForm.handleSubmit(async (values) => {
    setError(null);
    setSubmitting(true);
    try {
      const res = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(values),
      });
      if (!res.ok) {
        const body = (await res.json().catch(() => ({}))) as { message?: string };
        throw new ApiError(res.status, "AUTH_FAILED", body.message ?? "登录失败");
      }
      const data = (await res.json()) as Awaited<ReturnType<typeof import("@/features/auth/authApi").login>>;
      setSession({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: data.user,
      });
      navigate({ to: "/" });
    } catch (e) {
      setError(e instanceof Error ? e.message : "登录失败");
    } finally {
      setSubmitting(false);
    }
  });

  const onRegister = registerForm.handleSubmit(async (values) => {
    setError(null);
    setSubmitting(true);
    try {
      const res = await fetch("/api/v1/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(values),
      });
      if (!res.ok) {
        const body = (await res.json().catch(() => ({}))) as { message?: string };
        throw new ApiError(res.status, "REGISTER_FAILED", body.message ?? "注册失败");
      }
      const data = (await res.json()) as Awaited<ReturnType<typeof import("@/features/auth/authApi").register>>;
      setSession({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: data.user,
      });
      navigate({ to: "/" });
    } catch (e) {
      setError(e instanceof Error ? e.message : "注册失败");
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 p-4">
      <div className="w-full max-w-md bg-white shadow-lg rounded-2xl p-8">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-slate-900">Cycling Lab</h1>
          <p className="text-sm text-slate-500 mt-1">个人骑行训练实验室</p>
        </div>
        <div className="flex gap-2 mb-6 bg-slate-100 rounded-lg p-1">
          <button
            type="button"
            className={`flex-1 py-1.5 text-sm rounded-md ${mode === "login" ? "bg-white shadow" : "text-slate-500"}`}
            onClick={() => { setMode("login"); setError(null); }}
          >
            登录
          </button>
          <button
            type="button"
            className={`flex-1 py-1.5 text-sm rounded-md ${mode === "register" ? "bg-white shadow" : "text-slate-500"}`}
            onClick={() => { setMode("register"); setError(null); }}
          >
            注册
          </button>
        </div>
        {error && (
          <div className="mb-4 p-3 text-sm text-red-700 bg-red-50 border border-red-200 rounded">
            {error}
          </div>
        )}
        {mode === "login" ? (
          <form onSubmit={onLogin} className="space-y-4">
            <Field label="邮箱" error={loginForm.formState.errors.email?.message}>
              <input
                type="email"
                className="input"
                placeholder="you@example.com"
                {...loginForm.register("email")}
              />
            </Field>
            <Field label="密码" error={loginForm.formState.errors.password?.message}>
              <input
                type="password"
                className="input"
                placeholder="******"
                {...loginForm.register("password")}
              />
            </Field>
            <button type="submit" className="btn-primary w-full" disabled={submitting}>
              {submitting ? "登录中..." : "登录"}
            </button>
          </form>
        ) : (
          <form onSubmit={onRegister} className="space-y-4">
            <Field label="邮箱" error={registerForm.formState.errors.email?.message}>
              <input
                type="email"
                className="input"
                placeholder="you@example.com"
                {...registerForm.register("email")}
              />
            </Field>
            <Field label="显示名" error={registerForm.formState.errors.displayName?.message}>
              <input
                type="text"
                className="input"
                placeholder="昵称"
                {...registerForm.register("displayName")}
              />
            </Field>
            <Field label="密码（≥ 8 位）" error={registerForm.formState.errors.password?.message}>
              <input
                type="password"
                className="input"
                placeholder="********"
                {...registerForm.register("password")}
              />
            </Field>
            <button type="submit" className="btn-primary w-full" disabled={submitting}>
              {submitting ? "注册中..." : "创建账号"}
            </button>
          </form>
        )}
        <style>{`
          .input { @apply w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent; }
          .btn-primary { @apply py-2 px-4 rounded-md bg-brand-500 hover:bg-brand-600 text-white text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed; }
        `}</style>
      </div>
    </div>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="text-sm text-slate-700 mb-1 block">{label}</span>
      {children}
      {error && <span className="text-xs text-red-600 mt-1 block">{error}</span>}
    </label>
  );
}
