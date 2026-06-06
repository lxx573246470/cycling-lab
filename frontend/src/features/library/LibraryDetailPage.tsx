import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  CATEGORIES,
  libraryApi,
  parseStructure,
  templatePutSchema,
  totalDurationSec,
  type Block,
  type Structure,
  type WorkoutTemplateDto,
  type TemplatePutInput,
} from "./libraryApi";
import { BlockEditor } from "./BlockEditor";
import { PowerCurvePreview } from "./PowerCurvePreview";
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
  if (!query.data) return <ErrorBanner message="Not found" />;

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
        description={`Version ${initial.currentVersion} · ${initial.category} · ${initial.source}`}
        actions={
          <>
            <Link
              to={"/library/$id/versions" as any}
              params={{ id: initial.id } as any}
              className="px-3 py-1.5 text-sm rounded border border-slate-300 hover:bg-slate-50"
            >
              History ({initial.currentVersion})
            </Link>
            <button
              type="button"
              onClick={() => duplicate.mutate()}
              className="px-3 py-1.5 text-sm rounded border border-slate-300 hover:bg-slate-50"
            >
              Duplicate
            </button>
            <button
              type="button"
              onClick={() => {
                if (!confirm(`Archive "${initial.name}"?`)) return;
                patch.mutate({ archived: true }, {
                  onSuccess: () => navigate({ to: "/library" as any as any as any }),
                });
              }}
              className="px-3 py-1.5 text-sm rounded border border-red-200 text-red-600 hover:bg-red-50"
            >
              Archive
            </button>
            <button
              type="submit"
              disabled={replace.isPending}
              className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
            >
              {replace.isPending ? "Saving…" : "Save (new version)"}
            </button>
          </>
        }
      />

      {(replace.error || patch.error) && (
        <ErrorBanner message={(replace.error || patch.error)?.message ?? "Save failed"} />
      )}
      {initial.archived && (
        <div className="p-3 text-sm bg-amber-50 border border-amber-200 rounded text-amber-800">
          This template is archived. Restore it from the list view to make changes.
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="space-y-4">
          <Card title="Metadata">
            <div className="grid grid-cols-2 gap-3">
              <Field label="Name" error={formState.errors.name?.message}>
                <input className={inputCls} {...register("name")} />
              </Field>
              <Field label="Category" error={formState.errors.category?.message}>
                <select className={inputCls} {...register("category")}>
                  {CATEGORIES.map((c) => (
                    <option key={c.code} value={c.code}>{c.label}</option>
                  ))}
                </select>
              </Field>
              <Field label="Intensity">
                <input className={inputCls} {...register("intensity")} />
              </Field>
              <Field label="Tags (comma-separated)">
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
              <Field label="Description" className="col-span-2">
                <textarea
                  className={`${inputCls} min-h-[80px]`}
                  {...register("descriptionMd")}
                  onBlur={() => onPatchMeta({ descriptionMd: watch("descriptionMd") })}
                />
              </Field>
            </div>
          </Card>

          <Card title="Blocks">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs text-slate-500">
                Total {Math.round(totalDurationSec(liveStructure) / 60)}m
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
            <Field label="Change note" hint="Required when saving a new version." className="mt-3" error={formState.errors.changeNote?.message}>
              <input className={inputCls} placeholder="e.g. tightened warmup, added cooldown" {...register("changeNote")} />
            </Field>
          </Card>
        </div>

        <div className="space-y-4">
          <Card title="Power curve">
            <PowerCurvePreview structure={liveStructure} />
          </Card>
          <Card title="Raw structure_json">
            <pre className="text-[11px] overflow-auto max-h-40 bg-slate-50 p-2 rounded">
              {JSON.stringify(liveStructure, null, 2)}
            </pre>
          </Card>
        </div>
      </div>
    </form>
  );
}
