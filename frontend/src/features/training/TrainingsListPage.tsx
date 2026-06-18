import { useRef, useState } from "react";
import { useNavigate, useRouter } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ErrorBanner, PageHeader, Spinner } from "@/components/ui";
import {
  formatBytes,
  sportTypeLabel,
  trainingFileApi,
  trainingFileStatusLabels,
  type TrainingFileSummary,
} from "./trainingFileApi";

export function TrainingsListPage() {
  const qc = useQueryClient();
  const router = useRouter();
  const navigate = useNavigate();
  const fileInput = useRef<HTMLInputElement>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const list = useQuery({
    queryKey: ["trainings", "list"],
    queryFn: () => trainingFileApi.list({ page: 0, size: 50 }),
  });

  const upload = useMutation({
    mutationFn: (file: File) => trainingFileApi.upload(file),
    onSuccess: async () => {
      setUploadError(null);
      await qc.invalidateQueries({ queryKey: ["trainings", "list"] });
      router.invalidate();
    },
    onError: (err: Error) => setUploadError(err.message),
  });

  const remove = useMutation({
    mutationFn: (id: string) => trainingFileApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["trainings", "list"] }),
  });

  const onPick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) upload.mutate(f);
    e.target.value = "";
  };

  return (
    <>
      <PageHeader
        title="训练记录"
        description="上传码表、骑行台或 Garmin Connect 导出的 FIT 文件。文件会在本地解析和保存，可查看指标、最佳功率和逐秒记录。"
        actions={
          <button
            type="button"
            onClick={() => fileInput.current?.click()}
            className="px-3 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium"
            disabled={upload.isPending}
          >
            {upload.isPending ? "上传中..." : "上传 .fit"}
          </button>
        }
      />

      <input
        ref={fileInput}
        type="file"
        accept=".fit"
        className="hidden"
        onChange={onPick}
      />

      {uploadError && <ErrorBanner message={uploadError} />}
      {list.error && <ErrorBanner message={(list.error as Error).message} />}

      {list.isLoading && <Spinner />}

      {list.data && list.data.content.length === 0 && (
        <div className="text-sm text-slate-500 p-6 border border-dashed border-slate-300 rounded text-center">
          还没有训练记录。点击“上传 .fit”添加一个。
        </div>
      )}

      <div className="space-y-2">
        {list.data?.content.map((f) => (
          <TrainingRow
            key={f.id}
            file={f}
            onOpen={() =>
              navigate({ to: "/trainings/$id" as any, params: { id: f.id } as any })
            }
            onDelete={() => {
              if (confirm(`删除 ${f.originalFilename}?`)) remove.mutate(f.id);
            }}
          />
        ))}
      </div>
    </>
  );
}

function TrainingRow({
  file,
  onOpen,
  onDelete,
}: {
  file: TrainingFileSummary;
  onOpen: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 flex items-center justify-between">
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onOpen}
            className="text-sm font-semibold text-brand-700 hover:underline truncate text-left"
          >
            {file.originalFilename}
          </button>
          <StatusBadge status={file.status} />
        </div>
        <div className="text-xs text-slate-500 mt-0.5">
          ISO {file.isoYear}-W{Math.min(file.isoWeek, 53).toString().padStart(2, "0")}
          {" · "}
          {formatBytes(file.sizeBytes)} · {sportTypeLabel(file.sportType)}
        </div>
        <div className="text-xs text-slate-400 mt-0.5">
          {file.recordedAt
            ? `记录于 ${new Date(file.recordedAt).toLocaleString()}`
            : file.createdAt
              ? `上传于 ${new Date(file.createdAt).toLocaleString()}`
              : ""}
        </div>
      </div>
      <button
        type="button"
        onClick={onDelete}
        className="text-sm px-3 py-1.5 rounded border border-slate-300 hover:bg-slate-50 ml-4 text-slate-500"
      >
        删除
      </button>
    </div>
  );
}

function StatusBadge({ status }: { status: keyof typeof trainingFileStatusLabels }) {
  const colors: Record<string, string> = {
    READY: "bg-green-50 text-green-700 border-green-200",
    PARSING: "bg-amber-50 text-amber-700 border-amber-200",
    PENDING: "bg-slate-50 text-slate-700 border-slate-200",
    FAILED: "bg-red-50 text-red-700 border-red-200",
  };
  const cls = colors[status] ?? colors.PENDING;
  return (
    <span
      className={`text-[10px] font-semibold uppercase tracking-wide px-1.5 py-0.5 rounded border ${cls}`}
    >
      {trainingFileStatusLabels[status] ?? status}
    </span>
  );
}
