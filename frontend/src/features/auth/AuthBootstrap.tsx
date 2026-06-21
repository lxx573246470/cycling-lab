import { useEffect, type ReactNode } from "react";
import { AUTH_DISABLED, useAuthStore } from "@/features/auth/authStore";
import { fetchMe } from "@/features/auth/authApi";

export function AuthBootstrap({ children }: { children: ReactNode }) {
  const { accessToken, user, setSession, clear } = useAuthStore();

  useEffect(() => {
    if (AUTH_DISABLED) {
      if (user) return;
      let cancelled = false;
      fetchMe()
        .then((me) => {
          if (cancelled) return;
          setSession({ accessToken: "local-auth-disabled", refreshToken: "local-auth-disabled", user: me });
        })
        .catch(() => {
          if (!cancelled) clear();
        });
      return () => {
        cancelled = true;
      };
    }
    if (!accessToken) return;
    if (user) return;
    let cancelled = false;
    fetchMe()
      .then((me) => {
        if (cancelled) return;
        const { refreshToken } = useAuthStore.getState();
        if (refreshToken) {
          setSession({ accessToken, refreshToken, user: me });
        }
      })
      .catch(() => {
        if (!cancelled) clear();
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, user, setSession, clear]);

  return <>{children}</>;
}
