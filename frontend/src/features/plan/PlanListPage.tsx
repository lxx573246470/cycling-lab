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
        title="周计划"
        description="每个 ISO 周对应一个计划，创建后会自动生成 7 天卡片，可逐日编辑。"
        actions={
          <Link
            to={"/plans/new" as any}
            search={{ isoYear: current.year, isoWeek: current.week } as any}
            className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium"
          >
            新建本周
          </Link>
        }
      />

      {query.error && <ErrorBanner message={(query.error as Error).message} />}
      {query.isLoading && <Spinner />}

      {query.data && query.data.content.length === 0 && (
        <div className="text-sm text-slate-500 p-6 border border-dashed border-slate-300 rounded text-center">
          还没有周计划。{" "}
          <Link
            to={"/plans/new" as any}
            search={{ isoYear: current.year, isoWeek: current.week } as any}
            className="text-brand-600 hover:underline"
          >
            为 {current.year} 年 W{current.week} 创建一个
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
              if (!confirm(`删除 ${p.isoYear} 年 W${p.isoWeek} 的周计划？`)) return;
              await remove.mutateAsync(p.id);
            }}
          />
        ))}
      </div>

      {query.data && query.data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-slate-500 mt-4">
          <span>
            {page * size + 1}–{Math.min((page + 1) * size, query.data.totalElements)} / {query.data.totalElements}
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-3 py-1 text-sm rounded border border-slate-300 disabled:opacity-50 hover:bg-slate-50"
            >
              上一页
            </button>
            <button
              type="button"
              disabled={page + 1 >= query.data.totalPages}
              onClick={() => setPage((p) => p + 1)}
              className="px-3 py-1 text-sm rounded border border-slate-300 disabled:opacity-50 hover:bg-slate-50"
            >
              下一页
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
          {summariseProgress(plan.progress)} · 更新于 {new Date(plan.updatedAt).toLocaleString()}
        </div>
      </button>
      <div className="flex gap-3 opacity-0 group-hover:opacity-100 transition-opacity">
        <button type="button" onClick={onOpen} className="text-xs text-slate-500 hover:text-slate-700">
          打开
        </button>
        <button type="button" onClick={onDelete} className="text-xs text-red-500 hover:text-red-700">
          删除
        </button>
      </div>
    </div>
  );
}
