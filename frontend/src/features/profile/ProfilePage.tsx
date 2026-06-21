import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import { profileApi, riderProfileSchema, type RiderProfile, type RiderProfileDto } from "./profileApi";
import { Card, ErrorBanner, Field, PageHeader, Spinner } from "@/components/ui";

const inputCls =
  "w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent";

const blankProfile: RiderProfile = {
  displayName: "",
  heightCm: 175,
  weightKg: 70,
  maxHr: 200,
  restingHr: 50,
  thresholdHr: 0 as unknown as number,
  ftp: 200,
  cadenceLow: 80,
  cadenceHigh: 95,
  bikes: [],
  powerMeter: "",
  hrStrap: "",
  headUnit: "",
  goals: {},
  preferences: {},
  isPublic: false,
};

function toFormValues(dto: RiderProfileDto | null): RiderProfile {
  if (!dto) return blankProfile;
  return {
    displayName: dto.displayName,
    heightCm: dto.heightCm,
    weightKg: Number(dto.weightKg),
    maxHr: dto.maxHr,
    restingHr: dto.restingHr ?? 0 as unknown as number,
    thresholdHr: dto.thresholdHr ?? 0 as unknown as number,
    ftp: dto.ftp,
    cadenceLow: dto.cadenceLow,
    cadenceHigh: dto.cadenceHigh,
    bikes: dto.bikes ?? [],
    powerMeter: dto.powerMeter ?? "",
    hrStrap: dto.hrStrap ?? "",
    headUnit: dto.headUnit ?? "",
    goals: dto.goals ?? {},
    preferences: dto.preferences ?? {},
    isPublic: dto.isPublic,
  };
}

export function ProfilePage() {
  const qc = useQueryClient();
  const query = useQuery({
    queryKey: ["profile"],
    queryFn: () => profileApi.get(),
  });

  const save = useMutation({
    mutationFn: (v: RiderProfile) => profileApi.put(v),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["profile"] });
      qc.invalidateQueries({ queryKey: ["profile", "derived-zones"] });
    },
  });

  if (query.isLoading) return <Spinner />;
  if (query.error) return <ErrorBanner message={(query.error as Error).message} />;

  const initial = toFormValues(query.data ?? null);
  return <ProfileForm initial={initial} save={save} hasProfile={!!query.data} />;
}

function ProfileForm({
  initial,
  save,
  hasProfile,
}: {
  initial: RiderProfile;
  save: ReturnType<typeof useMutation<RiderProfileDto, Error, RiderProfile>>;
  hasProfile: boolean;
}) {
  const { register, handleSubmit, formState, watch, reset, setValue } = useForm<RiderProfile>({
    resolver: zodResolver(riderProfileSchema),
    defaultValues: initial,
  });
  const [goalsText, setGoalsText] = useState(() =>
    Object.entries(initial.goals).map(([k, v]) => `${k}: ${v}`).join("\n"),
  );

  useEffect(() => {
    reset(initial);
    setGoalsText(Object.entries(initial.goals).map(([k, v]) => `${k}: ${v}`).join("\n"));
  }, [initial, reset]);

  const onSubmit = handleSubmit((values) => save.mutate(values));

  const height = watch("heightCm");
  const weight = watch("weightKg");
  const bmi = height && weight ? (Number(weight) / Math.pow(Number(height) / 100, 2)).toFixed(1) : null;

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      <PageHeader
        title="骑手档案"
        description={
          hasProfile
            ? "修改任意字段后保存；FTP 或最大心率变化会立即重新计算训练分区。"
            : "还没有保存骑手档案。填写基础信息后会自动计算训练分区。"
        }
        actions={
          <>
            <Link
              to={"/profile/zones" as any}
              className="px-3 py-1.5 text-sm rounded border border-slate-300 hover:bg-slate-50"
            >
              查看分区
            </Link>
            <button
              type="submit"
              disabled={save.isPending}
              className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
            >
              {save.isPending ? "保存中…" : hasProfile ? "保存修改" : "创建档案"}
            </button>
          </>
        }
      />

      {save.error && <ErrorBanner message={save.error.message} />}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card title="基础信息">
          <div className="grid grid-cols-2 gap-3">
            <Field label="显示名" error={formState.errors.displayName?.message}>
              <input className={inputCls} {...register("displayName")} />
            </Field>
            <Field label="身高（cm）" error={formState.errors.heightCm?.message}>
              <input type="number" className={inputCls} {...register("heightCm", { valueAsNumber: true })} />
            </Field>
            <Field label="体重（kg）" error={formState.errors.weightKg?.message}>
              <input type="number" step="0.1" className={inputCls} {...register("weightKg", { valueAsNumber: true })} />
            </Field>
            {bmi && (
              <Field label="BMI（自动计算）">
                <div className="px-3 py-2 border border-slate-200 rounded-md text-sm text-slate-600 bg-slate-50">
                  {bmi}
                </div>
              </Field>
            )}
          </div>
        </Card>

        <Card title="功率与心率">
          <div className="grid grid-cols-2 gap-3">
            <Field label="最大心率（bpm）" error={formState.errors.maxHr?.message}>
              <input type="number" className={inputCls} {...register("maxHr", { valueAsNumber: true })} />
            </Field>
            <Field label="静息心率（bpm）" hint="可选" error={formState.errors.restingHr?.message}>
              <input type="number" className={inputCls} {...register("restingHr", { valueAsNumber: true })} />
            </Field>
            <Field label="阈值心率（bpm）" hint="可选" error={formState.errors.thresholdHr?.message}>
              <input type="number" className={inputCls} {...register("thresholdHr", { valueAsNumber: true })} />
            </Field>
            <Field label="FTP（瓦）" error={formState.errors.ftp?.message}>
              <input type="number" className={inputCls} {...register("ftp", { valueAsNumber: true })} />
            </Field>
            <Field label="踏频下限（rpm）" error={formState.errors.cadenceLow?.message}>
              <input type="number" className={inputCls} {...register("cadenceLow", { valueAsNumber: true })} />
            </Field>
            <Field label="踏频上限（rpm）" error={formState.errors.cadenceHigh?.message}>
              <input type="number" className={inputCls} {...register("cadenceHigh", { valueAsNumber: true })} />
            </Field>
          </div>
        </Card>

        <Card title="设备">
          <div className="grid grid-cols-1 gap-3">
            <Field label="功率计">
              <input className={inputCls} placeholder="例如：Quarq DZero" {...register("powerMeter")} />
            </Field>
            <Field label="心率带">
              <input className={inputCls} placeholder="例如：Wahoo Tickr" {...register("hrStrap")} />
            </Field>
            <Field label="码表">
              <input className={inputCls} placeholder="例如：Wahoo Roam" {...register("headUnit")} />
            </Field>
          </div>
        </Card>

        <Card title="目标（可选）">
          <Field label="备注" hint="每行一条，例如：short_term: 每周 5 小时 Z2">
            <textarea
              className={`${inputCls} min-h-[140px]`}
              value={goalsText}
              onChange={(e) => {
                setGoalsText(e.target.value);
                const next: Record<string, string> = {};
                e.target.value
                  .split("\n")
                  .map((l) => l.trim())
                  .filter(Boolean)
                  .forEach((line) => {
                    const idx = line.indexOf(":");
                    if (idx > 0) next[line.slice(0, idx).trim()] = line.slice(idx + 1).trim();
                    else next[line] = "";
                  });
                setValue("goals", next, { shouldValidate: false, shouldDirty: true });
              }}
            />
          </Field>
        </Card>
      </div>
    </form>
  );
}
