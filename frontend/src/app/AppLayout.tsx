import { Link, Outlet, useNavigate, useRouterState } from "@tanstack/react-router";
import { useAuthStore } from "@/features/auth/authStore";

const navItems = [
  { to: "/", label: "Dashboard" },
  { to: "/profile", label: "Rider Profile" },
  { to: "/profile/zones", label: "Zones" },
  { to: "/library", label: "Workout Library" },
  { to: "/plans", label: "Weekly Plans" },
  { to: "/workouts", label: "Workout Files" },
  { to: "/trainings", label: "Trainings" },
  { to: "/reviews", label: "Reviews" },
];

export function AppLayout() {
  const user = useAuthStore((s) => s.user);
  const clear = useAuthStore((s) => s.clear);
  const navigate = useNavigate();
  const { location } = useRouterState();

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
            <span className="text-xs text-slate-400">M1</span>
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
                Sign out
              </button>
            </div>
          )}
        </div>
      </header>
      <div className="flex-1 bg-slate-50">
        <div className="max-w-6xl mx-auto px-6 py-6 flex gap-6">
          <aside className="w-48 shrink-0">
            <nav className="space-y-1">
              {navItems.map((item) => {
                const active = location.pathname === item.to ||
                  (item.to !== "/" && location.pathname.startsWith(item.to));
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    className={`block px-3 py-2 rounded text-sm ${
                      active
                        ? "bg-brand-50 text-brand-700 font-medium"
                        : "text-slate-600 hover:bg-slate-100"
                    }`}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </aside>
          <main className="flex-1 min-w-0">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}