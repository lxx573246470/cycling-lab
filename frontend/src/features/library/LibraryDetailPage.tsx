import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  CATEGORIES,
  categoryLabel,
  libraryApi,
  parseStructure,
  sourceLabel,
  templatePutSchema,
  totalDurationSec,
  type Block,
  type Structure,
  type WorkoutTemplateDto,
  type TemplatePutInput,
} from "./libraryApi";
import { BlockEditor } from "./BlockEditor";
import { PowerCurvePreview } from "./PowerCurvePreview";
import { GenerateZwoPanel, WorkoutFileListSection } from "@/features/workout/GenerateZwoPanel";
import { Card, ErrorBanner, Field, PageHeader, Spinner } from "@/components/ui";

const inputCls =
  "w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent";

export function LibraryDetailPage() {
  const { id } = useParams({ strict: false }) as { id: string };
  const navigate = useNavigate();
  const qc = useQueryClient();
  const query = useQuery({
    queryKey: ["library", "detail", id],
    queryFn: () => libraryApi.get(id),
    enabled: !!id,
  });

  if (query.isLoading) return <Spinner />;
  if (query.error) return <ErrorBanner message={(query.error as Error).message} />;
  if (!query.data) return <ErrorBanner message="没有找到模板" />;

  return <DetailForm key={query.data.id} initial={query.data} navigate={navigate} qc={qc} />;
}

function DetailForm({
  initial,
  navigate,
  qc,
}: {
  initial: WorkoutTemplateDto;
  navigate: ReturnType<typeof useNavigate>;
  qc: ReturnType<typeof useQueryClient>;
}) {
  const structure = parseStructure(initial.structureJson) ?? { blocks: [] as Block[] } as Structure;
  const [liveStructure, setLiveStructure] = useState<Structure>(structure);
  const [tagsText, setTagsText] = useState(initial.tags.join(", "));

  useEffect(() => {
    setLiveStructure(parseStructure(initial.structureJson) ?? { blocks: [] } as Structure);
    setTagsText(initial.tags.join(", "));
  }, [initial]);

  const { register, handleSubmit, formState, setValue, watch } = useForm<TemplatePutInput>({
    resolver: zodResolver(templatePutSchema),
    defaultValues: {
      name: initial.name,
      category: initial.category as TemplatePutInput["category"],
      intensity: initial.intensity ?? "",
      descriptionMd: initial.descriptionMd ?? "",
      structureJson: initial.structureJson,
      tags: initial.tags,
      changeNote: "",
    },
  });

  const replace = useMutation({
    mutationFn: (v: TemplatePutInput) => libraryApi.replace(initial.id, v),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["library"] });
    },
  });

  const patch = useMutation({
    mutationFn: (v: { name?: string; category?: string; intensity?: string | null; descriptionMd?: string | null; tags?: string[]; archived?: boolean }) =>
      libraryApi.patch(initial.id, v as any),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["library"] });
    },
  });

  const duplicate = useMutation({
    mutationFn: () => libraryApi.duplicate(initial.id),
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ["library"] });
      navigate({ to: "/library/$id" as any, params: { id: created.id } as any });
    },
  });

  const onReplace = handleSubmit((values) => {
    const parsed = parseStructure(values.structureJson);
    if (!parsed) return;
    replace.mutate({ ...values, structureJson: JSON.stringify(parsed) });
  });

  const onPatchMeta = (delta: Partial<TemplatePutInput>) => {
    patch.mutate({
      ...delta,
      category: delta.category as any,
    } as any);
  };

  return (
    <form onSubmit={onReplace} className="space-y-6">
      <PageHeader
        title={initial.name}
        description={`版本 ${initial.currentVersion} · ${categoryLabel(initial.category)} · ${sourceLabel(initial.source)}`}
        actions={
          <>
            <Link
              to={"/library/$id/versions" as any}
              params={{ id: initial.id } as any}
              className="px-3 py-1.5 text-sm rounded border border-slate-300 hover:bg-slate-50"
            >
              历史版本（{initial.currentVersion}）
            </Link>
            <button
              type="button"
              onClick={() => duplicate.mutate()}
              className="px-3 py-1.5 text-sm rounded border border-slate-300 hover:bg-slate-50"
            >
              复制
            </button>
            <button
              type="button"
              onClick={() => {
                if (!confirm(`归档 "${initial.name}"?`)) return;
                patch.mutate({ archived: true }, {
                  onSuccess: () => navigate({ to: "/library" as any as any as any }),
                });
              }}
              className="px-3 py-1.5 text-sm rounded border border-red-200 text-red-600 hover:bg-red-50"
            >
              归档
            </button>
            <button
              type="submit"
              disabled={replace.isPending}
              className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
            >
              {replace.isPending ? "保存中…" : "保存为新版本"}
            </button>
          </>
        }
      />

      {(replace.error || patch.error) && (
        <ErrorBanner message={(replace.error || patch.error)?.message ?? "保存失败"} />
      )}
      {initial.archived && (
        <div className="p-3 text-sm bg-amber-50 border border-amber-200 rounded text-amber-800">
          这个模板已归档。需要修改时请先在列表页恢复。
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="space-y-4">
          <Card title="基础信息">
            <div className="grid grid-cols-2 gap-3">
              <Field label="名称" error={formState.errors.name?.message}>
                <input className={inputCls} {...register("name")} />
              </Field>
              <Field label="分类" error={formState.errors.category?.message}>
                <select className={inputCls} {...register("category")}>
                  {CATEGORIES.map((c) => (
                    <option key={c.code} value={c.code}>{c.label}</option>
                  ))}
                </select>
              </Field>
              <Field label="强度">
                <input className={inputCls} {...register("intensity")} />
              </Field>
              <Field label="标签（用英文逗号分隔）">
                <input
                  className={inputCls}
                  value={tagsText}
                  onChange={(e) => {
                    setTagsText(e.target.value);
                    const list = e.target.value.split(",").map((t) => t.trim()).filter(Boolean);
                    setValue("tags", list, { shouldValidate: true });
                  }}
                  onBlur={() => onPatchMeta({ tags: watch("tags") })}
                />
              </Field>
              <Field label="描述" className="col-span-2">
                <textarea
                  className={`${inputCls} min-h-[80px]`}
                  {...register("descriptionMd")}
                  onBlur={() => onPatchMeta({ descriptionMd: watch("descriptionMd") })}
                />
              </Field>
            </div>
          </Card>

          <Card title="训练区块">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs text-slate-500">
                总计 {Math.round(totalDurationSec(liveStructure) / 60)} 分钟
              </span>
            </div>
            <BlockEditor
              structure={liveStructure}
              readOnly={initial.archived}
              onChange={(s) => {
                setLiveStructure(s);
                setValue("structureJson", JSON.stringify(s), { shouldValidate: true });
              }}
            />
            <Field label="变更说明" hint="保存新版本时建议填写。" className="mt-3" error={formState.errors.changeNote?.message}>
              <input className={inputCls} placeholder="例如：缩短热身，增加放松段" {...register("changeNote")} />
            </Field>
          </Card>
        </div>

        <div className="space-y-4">
          <Card title="功率曲线">
            <PowerCurvePreview structure={liveStructure} />
          </Card>
          <Card title="原始 structure_json">
            <pre className="text-[11px] overflow-auto max-h-40 bg-slate-50 p-2 rounded">
              {JSON.stringify(liveStructure, null, 2)}
            </pre>
          </Card>
        </div>
      </div>

      <GenerateZwoPanel
        defaultStructureJson={JSON.stringify(liveStructure)}
        sourceTemplateId={initial.id}
      />

      <WorkoutFileListSection title="由这个模板生成的文件" refreshKey={`from-${initial.id}`} />
    </form>
  );
}
