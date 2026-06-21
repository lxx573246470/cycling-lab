import { useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  CATEGORIES,
  libraryApi,
  parseStructure,
  templateCreateSchema,
  totalDurationSec,
  type Structure,
  type TemplateCreateInput,
  type WorkoutTemplateDto,
} from "./libraryApi";
import { ErrorBanner, Field, PageHeader } from "@/components/ui";
import { PowerCurvePreview } from "./PowerCurvePreview";
import { BlockEditor } from "./BlockEditor";

const inputCls =
  "w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent";

const blankStructure: Structure = {
  blocks: [
    { type: "warmup", durationSec: 600, powerLow: 0.45, powerHigh: 0.6 },
    { type: "steady", durationSec: 1800, power: 0.65 },
    { type: "cooldown", durationSec: 300, powerLow: 0.55, powerHigh: 0.4 },
  ],
};

const blankTags = "outdoor, z2";

export function LibraryNewPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { register, handleSubmit, formState, setValue, watch } = useForm<TemplateCreateInput>({
    resolver: zodResolver(templateCreateSchema),
    defaultValues: {
      name: "",
      category: "endurance",
      intensity: "Z2",
      descriptionMd: "",
      structureJson: JSON.stringify(blankStructure),
      tags: blankTags.split(",").map((t) => t.trim()).filter(Boolean),
    },
  });

  const [structure, setStructure] = useState<Structure>(blankStructure);
  const structureJson = watch("structureJson");
  const [tagsText, setTagsText] = useState(blankTags);

  const create = useMutation({
    mutationFn: (v: TemplateCreateInput) => libraryApi.create(v),
    onSuccess: (created: WorkoutTemplateDto) => {
      qc.invalidateQueries({ queryKey: ["library"] });
      navigate({ to: "/library/$id" as any, params: { id: created.id } as any });
    },
  });

  const onSubmit = handleSubmit((values) => {
    const structure = parseStructure(values.structureJson);
    if (!structure) {
      create.mutate(values); // surface the validation error from backend
      return;
    }
    create.mutate({ ...values, structureJson: JSON.stringify(structure) });
  });

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      <PageHeader
        title="新建训练模板"
        description="创建可复用的训练区块。功率值按 FTP 比例填写，例如 0.65 表示 65% FTP。"
        actions={
          <button
            type="submit"
            disabled={create.isPending}
            className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
          >
            {create.isPending ? "创建中…" : "创建"}
          </button>
        }
      />

      {create.error && <ErrorBanner message={create.error.message} />}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="space-y-4">
          <div className="bg-white border border-slate-200 rounded-lg p-4 space-y-3">
            <Field label="名称" error={formState.errors.name?.message}>
              <input className={inputCls} placeholder="Z2 Base Ride" {...register("name")} />
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="分类" error={formState.errors.category?.message}>
                <select className={inputCls} {...register("category")}>
                  {CATEGORIES.map((c) => (
                    <option key={c.code} value={c.code}>{c.label}</option>
                  ))}
                </select>
              </Field>
              <Field label="强度" error={formState.errors.intensity?.message}>
                <input className={inputCls} placeholder="Z2 / sweet-spot / VO2" {...register("intensity")} />
              </Field>
            </div>
            <Field label="标签（用英文逗号分隔）" error={formState.errors.tags?.message as string | undefined}>
              <input
                className={inputCls}
                value={tagsText}
                onChange={(e) => {
                  setTagsText(e.target.value);
                  const list = e.target.value
                    .split(",")
                    .map((t) => t.trim())
                    .filter(Boolean);
                  setValue("tags", list, { shouldValidate: true });
                }}
              />
            </Field>
            <Field label="描述（Markdown）">
              <textarea className={`${inputCls} min-h-[80px]`} {...register("descriptionMd")} />
            </Field>
          </div>

          <div className="bg-white border border-slate-200 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">训练区块</h2>
              <span className="text-xs text-slate-500">
                总计：{Math.round(totalDurationSec(structure) / 60)} 分钟
              </span>
            </div>
            <BlockEditor
              structure={structure}
              onChange={(s) => {
                setStructure(s);
                setValue("structureJson", JSON.stringify(s), { shouldValidate: true });
              }}
            />
            {formState.errors.structureJson && (
              <p className="text-xs text-red-600 mt-2">
                {formState.errors.structureJson.message as string}
              </p>
            )}
          </div>
        </div>

        <div className="space-y-4">
          <div className="bg-white border border-slate-200 rounded-lg p-4">
            <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide mb-2">
              功率曲线预览
            </h2>
            <PowerCurvePreview structure={structure} />
          </div>
          <div className="text-xs text-slate-400">
            原始 structure_json:
            <pre className="mt-2 p-2 bg-slate-50 rounded text-[11px] overflow-auto max-h-40">
              {structureJson}
            </pre>
          </div>
        </div>
      </div>
    </form>
  );
}
