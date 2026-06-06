import { useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ErrorBanner, PageHeader, Spinner } from "@/components/ui";
import { currentIsoWeek, planApi, summariseProgress, type WeeklyPlanSummary } from "./planApi";

export function PlanListPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const size = 20;

  const query = useQuery({
    queryKey: ["plans", "weeks", page, size],
    queryFn: () => planApi.list({ page, size }),
  });

  const remove = useMutation({
    mutationFn: (id: string) => planApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["plans"] }),
  });

  const current = currentIsoWeek();

  return (
    <>
      <PageHeader
        title="Weekly plans"
        description="One plan per ISO week. Each plan auto-fills seven day cards that you can edit individually."
        actions={
          <Link
            to={"/plans/new" as any}
            search={{ isoYear: current.year, isoWeek: current.week } as any}
            className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium"
          >
            New week
          </Link>
        }
      />

      {query.error && <ErrorBanner message={(query.error as Error).message} />}
      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
        <div className="text-sm text-slate-500 p-6 border border-dashed border-slate-300 rounded text-center">
          No weekly plans yet.{" "}
          <Link
            to={"/plans/new" as any}
            search={{ isoYear: current.year, isoWeek: current.week } as any}
            className="text-brand-600 hover:underline"
          >
            Create one for W{current.week} of {current.year}
          </Link>
          .
        </div>
      )}

      <div className="space-y-2">
        {query.data?.content.map((p) => (
          <PlanRow
            key={p.id}
            plan={p}
            onOpen={() => navigate({ to: "/plans/$id" as any, params: { id: p.id } as any })}
            onDelete={async () => {
              if (!confirm(`Delete plan for W${p.isoWeek} of ${p.isoYear}?`)) return;
              await remove.mutateAsync(p.id);
            }}
          />
        ))}
      </div>

      {query.data && query.data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-slate-500 mt-4">
          <span>
            {page * size + 1}–{Math.min((page + 1) * size, query.data.totalElements)} of {query.data.totalElements}
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-3 py-1 text-sm rounded border border-slate-300 disabled:opacity-50 hover:bg-slate-50"
            >
              Prev
            </button>
            <button
              type="button"
              disabled={page + 1 >= query.data.totalPages}
              onClick={() => setPage((p) => p + 1)}
              className="px-3 py-1 text-sm rounded border border-slate-300 disabled:opacity-50 hover:bg-slate-50"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </>
  );
}

function PlanRow({
  plan,
  onOpen,
  onDelete,
}: {
  plan: WeeklyPlanSummary;
  onOpen: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 flex items-center justify-between group">
      <button type="button" onClick={onOpen} className="text-left min-w-0 flex-1">
        <div className="flex items-baseline gap-2">
          <span className="text-base font-semibold text-slate-900">
            {plan.isoYear} · W{String(plan.isoWeek).padStart(2, "0")}
          </span>
          {plan.title && <span className="text-sm text-slate-700 truncate">— {plan.title}</span>}
        </div>
        <div className="text-xs text-slate-400 mt-0.5">
          {summariseProgress(plan.progress)} · updated {new Date(plan.updatedAt).toLocaleString()}
        </div>
      </button>
      <div className="flex gap-3 opacity-0 group-hover:opacity-100 transition-opacity">
        <button type="button" onClick={onOpen} className="text-xs text-slate-500 hover:text-slate-700">
          Open
        </button>
        <button type="button" onClick={onDelete} className="text-xs text-red-500 hover:text-red-700">
          Delete
        </button>
      </div>
    </div>
  );
}