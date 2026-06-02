import { useAuthStore } from "@/features/auth/authStore";

const API_BASE = import.meta.env.VITE_API_BASE ?? "/api/v1";

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string, public details?: unknown) {
    super(message);
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  signal?: AbortSignal;
  skipAuth?: boolean;
}

let refreshInFlight: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (refreshInFlight) return refreshInFlight;
  const { refreshToken, setAccessToken, setSession, clear, user } = useAuthStore.getState();
  if (!refreshToken) {
    clear();
    return null;
  }
  refreshInFlight = (async () => {
    try {
      const res = await fetch(`${API_BASE}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
      if (!res.ok) {
        clear();
        return null;
      }
      const data = (await res.json()) as {
        accessToken: string;
        refreshToken: string;
        user: typeof user;
      };
      if (data.user) {
        setSession({ accessToken: data.accessToken, refreshToken: data.refreshToken, user: data.user });
      } else {
        setAccessToken(data.accessToken);
      }
      return data.accessToken;
    } catch {
      clear();
      return null;
    } finally {
      refreshInFlight = null;
    }
  })();
  return refreshInFlight;
}

export async function api<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, signal, skipAuth } = opts;
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (!skipAuth) {
    const { accessToken } = useAuthStore.getState();
    if (accessToken) headers["Authorization"] = `Bearer ${accessToken}`;
  }

  const doFetch = async (token?: string) => {
    const finalHeaders = { ...headers };
    if (token) finalHeaders["Authorization"] = `Bearer ${token}`;
    return fetch(`${API_BASE}${path}`, {
      method,
      headers: finalHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    });
  };

  let res = await doFetch();
  if (res.status === 401 && !skipAuth) {
    const fresh = await refreshAccessToken();
    if (fresh) {
      res = await doFetch(fresh);
    }
  }

  if (!res.ok) {
    let code = "UNKNOWN";
    let message = `Request failed: ${res.status}`;
    let details: unknown;
    try {
      const errBody = (await res.json()) as { code?: string; message?: string; details?: unknown };
      if (errBody.code) code = errBody.code;
      if (errBody.message) message = errBody.message;
      details = errBody.details;
    } catch {
      // ignore body parse errors
    }
    if (res.status === 401) {
      useAuthStore.getState().clear();
    }
    throw new ApiError(res.status, code, message, details);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
