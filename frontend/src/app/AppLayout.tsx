import { Outlet, useNavigate } from "@tanstack/react-router";
import { useAuthStore } from "@/features/auth/authStore";

export function AppLayout() {
  const user = useAuthStore((s) => s.user);
  const clear = useAuthStore((s) => s.clear);
  const navigate = useNavigate();

  const onLogout = () => {
    clear();
    navigate({ to: "/login" });
  };

  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b border-slate-200 bg-white">
        <div className="max-w-6xl mx-auto px-6 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-lg font-semibold text-slate-900">Cycling Lab</span>
            <span className="text-xs text-slate-400">v0.1</span>
          </div>
          {user && (
            <div className="flex items-center gap-3 text-sm">
              <span className="text-slate-600">{user.displayName}</span>
              <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-600 text-xs">
                {user.role}
              </span>
              <button
                onClick={onLogout}
                className="px-3 py-1 text-sm rounded border border-slate-300 hover:bg-slate-50"
              >
                退出
              </button>
            </div>
          )}
        </div>
      </header>
      <main className="flex-1 bg-slate-50">
        <div className="max-w-6xl mx-auto px-6 py-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
