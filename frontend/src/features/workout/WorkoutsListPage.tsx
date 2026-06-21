import { useQuery } from "@tanstack/react-query";
import { PageHeader, Spinner, ErrorBanner } from "@/components/ui";
import { workoutFileApi, formatBytes, sportTypeLabel, type WorkoutFileSummary } from "./workoutFileApi";

export function WorkoutsListPage() {
  const q = useQuery({
    queryKey: ["workout-files", "page"],
    queryFn: () => workoutFileApi.list({ page: 0, size: 50 }),
  });

  return (
    <>
      <PageHeader
        title="已生成训练文件"
        description="这里保存从模板库或临时结构生成的 ZWO 文件。每个文件都是独立快照，可导入 Zwift、TrainingPeaks、Rouvy 等软件。"
      />

      {q.error && <ErrorBanner message={(q.error as Error).message} />}
      {q.isLoading && <Spinner />}

      {q.data && q.data.content.length === 0 && (
        <div className="text-sm text-slate-500 p-6 border border-dashed border-slate-300 rounded text-center">
          还没有生成训练文件。前往{" "}
          <a href="/library" className="text-brand-600 hover:underline">
            训练模板库
          </a>{" "}
          打开模板后点击“生成 .zwo”。
        </div>
      )}

      <div className="space-y-2">
        {q.data?.content.map((f) => (
          <WorkoutFileRow key={f.id} file={f} />
        ))}
      </div>
    </>
  );
}

function WorkoutFileRow({ file }: { file: WorkoutFileSummary }) {
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 flex items-center justify-between">
      <div className="min-w-0 flex-1">
        <div className="text-sm font-semibold text-slate-900 truncate">
          {file.name}
        </div>
        <div className="text-xs text-slate-500 mt-0.5">
          {file.format} · {formatBytes(file.sizeBytes)} · {sportTypeLabel(file.sportType)}
        </div>
        <div className="text-xs text-slate-400 mt-0.5">
          创建于 {new Date(file.createdAt).toLocaleString()}
          {file.sourceTemplateId && " · 来自模板库"}
        </div>
        {file.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-1.5">
            {file.tags.map((t) => (
              <span
                key={t}
                className="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600"
              >
                {t}
              </span>
            ))}
          </div>
        )}
      </div>
      <button
        type="button"
        onClick={() => workoutFileApi.download(file.id, `${file.name}.zwo`)}
        className="text-sm px-3 py-1.5 rounded border border-slate-300 hover:bg-slate-50 ml-4"
      >
        下载 .zwo
      </button>
    </div>
  );
}
