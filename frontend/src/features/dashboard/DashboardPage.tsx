import { Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useAuthStore } from "@/features/auth/authStore";
import { profileApi } from "@/features/profile/profileApi";
import { libraryApi, CATEGORIES } from "@/features/library/libraryApi";
import { currentIsoWeek, planApi, summariseProgress, type WeeklyPlanSummary } from "@/features/plan/planApi";
import { Spinner } from "@/components/ui";

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const current = currentIsoWeek();

  const profile = useQuery({
    queryKey: ["profile"],
    queryFn: () => profileApi.get(),
  });
  const counts = useQuery({
    queryKey: ["library", "category-counts"],
    queryFn: () => libraryApi.categoryCounts(),
  });
  const plans = useQuery({
    queryKey: ["plans", "weeks", "dashboard", 0, 5],
    queryFn: () => planApi.list({ page: 0, size: 5 }),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>
        <p className="text-slate-500 mt-1">Welcome back, {user?.displayName ?? "Rider"}.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <ProfileWidget query={profile} />
        <LibraryWidget counts={counts} />
      </div>

      <CurrentWeekWidget current={current} plans={plans} />
    </div>
  );
}

function ProfileWidget({ query }: { query: ReturnType<typeof useQuery<Awaited<ReturnType<typeof profileApi.get>>, Error>> }) {
  if (query.isLoading) return <WidgetSkeleton title="Rider profile" />;
  const p = query.data;
  if (!p) {
    return (
      <div className="bg-amber-50 border border-amber-200 rounded-lg p-5">
        <div className="text-xs uppercase tracking-wide text-amber-700 font-semibold">Rider profile</div>
        <div className="mt-2 text-base font-semibold text-amber-900">Incomplete</div>
        <p className="text-sm text-amber-800 mt-1">
          Set FTP, max HR, height & weight so the system can compute zones.
        </p>
        <Link to={"/profile" as any} className="inline-block mt-3 text-sm text-brand-700 hover:underline">
          Open profile →
        </Link>
      </div>
    );
  }
  const bmi = (Number(p.weightKg) / Math.pow(Number(p.heightCm) / 100, 2)).toFixed(1);
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-5">
      <div className="flex items-start justify-between">
        <div>
          <div className="text-xs uppercase tracking-wide text-slate-400">Rider profile</div>
          <div className="mt-1 text-lg font-semibold text-slate-900">{p.displayName}</div>
          <div className="text-sm text-slate-500 mt-1">
            FTP {p.ftp}W · max HR {p.maxHr}bpm · BMI {bmi}
          </div>
        </div>
        <Link
          to={"/profile" as any}
          className="text-xs px-2 py-1 border border-slate-300 rounded hover:bg-slate-50"
        >
          Edit
        </Link>
      </div>
    </div>
  );
}

function LibraryWidget({ counts }: { counts: ReturnType<typeof useQuery<Awaited<ReturnType<typeof libraryApi.categoryCounts>>, Error>> }) {
  if (counts.isLoading) return <WidgetSkeleton title="Workout library" />;
  const c = counts.data ?? {};
  const total = Object.values(c).reduce((a, b) => a + b, 0);
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-5">
      <div className="flex items-start justify-between">
        <div>
          <div className="text-xs uppercase tracking-wide text-slate-400">Workout library</div>
          <div className="mt-1 text-lg font-semibold text-slate-900">{total} templates</div>
          <div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-500">
            {CATEGORIES.map((cat) => (
              <span key={cat.code}>
                {cat.label}: <span className="font-medium text-slate-700">{c[cat.code] ?? 0}</span>
              </span>
            ))}
          </div>
        </div>
        <Link
          to={"/library" as any}
          className="text-xs px-2 py-1 border border-slate-300 rounded hover:bg-slate-50"
        >
          Open
        </Link>
      </div>
    </div>
  );
}

function CurrentWeekWidget({
  current,
  plans,
}: {
  current: { year: number; week: number };
  plans: ReturnType<typeof useQuery<Awaited<ReturnType<typeof planApi.list>>, Error>>;
}) {
  if (plans.isLoading) return <WidgetSkeleton title={`This week (W${current.week} · ${current.year})`} />;
  const all = plans.data?.content ?? [];
  const thisWeek = all.find((p) => p.isoYear === current.year && p.isoWeek === current.week) ?? null;
  const recent = all.filter((p) => !(p.isoYear === current.year && p.isoWeek === current.week)).slice(0, 3);

  return (
    <div className="bg-white border border-slate-200 rounded-lg p-5">
      <div className="flex items-start justify-between">
        <div>
          <div className="text-xs uppercase tracking-wide text-slate-400">
            This week · W{current.week} · {current.year}
          </div>
          {thisWeek ? (
            <>
              <div className="mt-1 text-lg font-semibold text-slate-900">
                {thisWeek.title ?? "Untitled plan"}
              </div>
              <div className="text-sm text-slate-500 mt-1">
                {summariseProgress(thisWeek.progress)}
              </div>
            </>
          ) : (
            <>
              <div className="mt-1 text-lg font-semibold text-slate-900">No plan yet</div>
              <p className="text-sm text-slate-500 mt-1">
                Plan your training for this ISO week.
              </p>
            </>
          )}
        </div>
        <div className="flex gap-2">
          {thisWeek && (
            <Link
              to={"/plans/$id" as any}
              params={{ id: thisWeek.id } as any}
              className="text-xs px-2 py-1 border border-slate-300 rounded hover:bg-slate-50"
            >
              Open
            </Link>
          )}
          <Link
            to={"/plans/new" as any}
            search={{ isoYear: current.year, isoWeek: current.week } as any}
            className="text-xs px-2 py-1 rounded bg-brand-500 hover:bg-brand-600 text-white"
          >
            {thisWeek ? "Plan next week" : "Plan this week"}
          </Link>
        </div>
      </div>

      {recent.length > 0 && (
        <div className="mt-4 border-t border-slate-100 pt-3">
          <div className="text-xs uppercase tracking-wide text-slate-400 mb-2">Recent</div>
          <ul className="space-y-1 text-sm">
            {recent.map((p) => (
              <RecentRow key={p.id} plan={p} />
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function RecentRow({ plan }: { plan: WeeklyPlanSummary }) {
  return (
    <li>
      <Link
        to={"/plans/$id" as any}
        params={{ id: plan.id } as any}
        className="flex items-center justify-between hover:bg-slate-50 px-2 py-1 rounded"
      >
        <span className="text-slate-700">
          {plan.isoYear} · W{String(plan.isoWeek).padStart(2, "0")}
          {plan.title ? ` — ${plan.title}` : ""}
        </span>
        <span className="text-xs text-slate-400">{summariseProgress(plan.progress)}</span>
      </Link>
    </li>
  );
}

function WidgetSkeleton({ title }: { title: string }) {
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-5">
      <div className="text-xs uppercase tracking-wide text-slate-400">{title}</div>
      <Spinner />
    </div>
  );
}