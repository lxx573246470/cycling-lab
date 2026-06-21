import { Link } from "@tanstack/react-router";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ErrorBanner, Field, Spinner } from "@/components/ui";
import {
  workoutFileApi,
  formatBytes,
  sportTypeLabel,
  type WorkoutFileCreateInput,
  type WorkoutFileDto,
} from "./workoutFileApi";

const inputCls =
  "w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent";

export function GenerateZwoPanel({
  defaultStructureJson,
  sourceTemplateId,
  onCreated,
}: {
  defaultStructureJson: string;
  sourceTemplateId?: string;
  onCreated?: (dto: WorkoutFileDto) => void;
}) {
  const qc = useQueryClient();
  const [name, setName] = useState("我的训练");
  const [description, setDescription] = useState("");
  const [tagsText, setTagsText] = useState("");
  const [sportType, setSportType] = useState<"bike" | "run" | "row">("bike");
  const [error, setError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: (body: WorkoutFileCreateInput) => workoutFileApi.create(body),
    onSuccess: (dto) => {
      qc.invalidateQueries({ queryKey: ["workout-files"] });
      onCreated?.(dto);
    },
    onError: (e) => setError((e as Error).message),
  });

  const onGenerate = () => {
    setError(null);
    create.mutate({
      name: name.trim(),
      description: description.trim() || null,
      sportType,
      tags: tagsText
        .split(",")
        .map((t) => t.trim())
        .filter(Boolean),
      sourceTemplateId: sourceTemplateId ?? null,
      structureJson: defaultStructureJson,
    });
  };

  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 space-y-3">
      <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">
        生成 Zwift .zwo 文件
      </h2>
      <p className="text-xs text-slate-500">
        根据当前结构生成可导入骑行台软件的 XML 文件。文件会保存一份结构副本，之后修改源模板不会影响已生成的 .zwo。
      </p>

      {error && <ErrorBanner message={error} />}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
        <Field label="名称">
          <input
            className={inputCls}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Z2 Base Ride"
          />
        </Field>
        <Field label="运动类型">
          <select
            className={inputCls}
            value={sportType}
            onChange={(e) => setSportType(e.target.value as "bike" | "run" | "row")}
          >
            <option value="bike">骑行</option>
            <option value="run">跑步</option>
            <option value="row">划船</option>
          </select>
        </Field>
      </div>
      <Field label="描述" hint="可选">
        <textarea
          className={`${inputCls} min-h-[60px]`}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </Field>
      <Field label="标签（用英文逗号分隔）" hint="可选">
        <input
          className={inputCls}
          value={tagsText}
          onChange={(e) => setTagsText(e.target.value)}
          placeholder="z2, sweet-spot, week-21"
        />
      </Field>

      <div className="flex justify-end">
        <button
          type="button"
          onClick={onGenerate}
          disabled={create.isPending || !name.trim()}
          className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
        >
          {create.isPending ? "生成中…" : "生成 .zwo"}
        </button>
      </div>

      {create.data && (
        <div className="mt-2 p-3 rounded bg-emerald-50 border border-emerald-200 text-sm">
          <div className="font-medium text-emerald-900">
            已生成 {create.data.name} · {formatBytes(create.data.sizeBytes)}
          </div>
          <div className="text-emerald-800 text-xs mt-1">
            来源： {create.data.sourceTemplateId ? "模板库" : "当前结构"}.
            可导入 Zwift / TrainingPeaks / Rouvy。
          </div>
          <div className="flex gap-3 mt-2">
            <button
              type="button"
              onClick={() =>
                workoutFileApi.download(create.data!.id, `${create.data!.name}.zwo`)
              }
              className="text-xs px-2 py-1 rounded bg-emerald-600 text-white hover:bg-emerald-700"
            >
              下载 .zwo
            </button>
            <Link
              to={"/workouts" as any}
              className="text-xs px-2 py-1 rounded border border-emerald-300 text-emerald-800 hover:bg-emerald-50"
            >
              查看全部文件
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}

export function WorkoutFileListSection({
  title,
  refreshKey,
}: {
  title?: string;
  refreshKey?: string;
}) {
  const q = useQuery({
    queryKey: ["workout-files", "list", refreshKey ?? "default"],
    queryFn: () => workoutFileApi.list({ page: 0, size: 10 }),
  });

  if (q.isLoading) return <Spinner />;
  const items = q.data?.content ?? [];
  if (items.length === 0) {
    return (
      <div className="text-sm text-slate-500 p-4 border border-dashed border-slate-300 rounded text-center">
        还没有生成训练文件。
      </div>
    );
  }
  return (
    <div className="space-y-2">
      {title && (
        <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">
          {title}
        </h3>
      )}
      {items.map((f) => (
        <div
          key={f.id}
          className="bg-white border border-slate-200 rounded-lg p-3 flex items-center justify-between"
        >
          <div className="min-w-0 flex-1">
            <div className="text-sm font-medium text-slate-900 truncate">
              {f.name}
            </div>
            <div className="text-xs text-slate-400 mt-0.5">
              {f.format} · {formatBytes(f.sizeBytes)} · {sportTypeLabel(f.sportType)} ·{" "}
              {new Date(f.createdAt).toLocaleString()}
            </div>
          </div>
          <button
            type="button"
            onClick={() => workoutFileApi.download(f.id, `${f.name}.zwo`)}
            className="text-xs px-2 py-1 rounded border border-slate-300 hover:bg-slate-50 ml-3"
          >
            下载
          </button>
        </div>
      ))}
    </div>
  );
}
