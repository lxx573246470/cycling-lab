import { useParams } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  Card,
  ErrorBanner,
  PageHeader,
  Spinner,
} from "@/components/ui";
import {
  formatDuration,
  trainingFileApi,
} from "./trainingFileApi";
import { TrainingChart } from "./TrainingChart";
import { ZoneBar } from "./ZoneBar";

export function TrainingDetailPage() {
  const { id } = useParams({ from: "/trainings/$id" as any });
  const q = useQuery({
    queryKey: ["trainings", "detail", id],
    queryFn: () => trainingFileApi.get(id),
    enabled: Boolean(id),
  });
  const samplesQ = useQuery({
    queryKey: ["trainings", "samples", id, { page: 0, size: 4000 }],
    queryFn: () => trainingFileApi.samples(id, 0, 4000),
    enabled: Boolean(id),
  });

  if (q.isLoading) return <Spinner />;
  if (q.error) return <ErrorBanner message={(q.error as Error).message} />;
  if (!q.data) return null;
  const d = q.data;

  return (
    <>
      <PageHeader
        title={d.originalFilename}
        description={
          d.recordedAt
            ? `记录于 ${new Date(d.recordedAt).toLocaleString()} · ISO ${d.isoYear}-W${Math.min(d.isoWeek, 53).toString().padStart(2, "0")}`
            : `ISO ${d.isoYear}-W${Math.min(d.isoWeek, 53).toString().padStart(2, "0")}`
        }
      />

      {d.status === "FAILED" && d.failureMessage && (
        <ErrorBanner
          message={`解析失败：${d.failureMessage}。请重新上传或换一个 .fit 文件。`}
        />
      )}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        <Stat label="时长" value={formatDuration(d.session?.durationSec)} />
        <Stat
          label="距离"
          value={d.session?.distanceM != null ? `${(d.session.distanceM / 1000).toFixed(1)} km` : "-"}
        />
        <Stat
          label="平均功率"
          value={d.session?.avgPower != null ? `${d.session.avgPower} W` : "-"}
        />
        <Stat
          label="标准化功率"
          value={d.session?.normalizedPower != null ? `${d.session.normalizedPower} W` : "-"}
        />
        <Stat
          label="平均心率"
          value={d.session?.avgHr != null ? `${d.session.avgHr} bpm` : "-"}
        />
        <Stat
          label="最大心率"
          value={d.session?.maxHr != null ? `${d.session.maxHr} bpm` : "-"}
        />
        <Stat
          label="平均踏频"
          value={d.session?.avgCadence != null ? `${d.session.avgCadence} rpm` : "-"}
        />
        <Stat
          label="心率漂移"
          value={d.session?.hrDriftPct != null ? `${d.session.hrDriftPct.toFixed(1)}%` : "-"}
        />
      </div>

      {d.session && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-4">
          <Card title="功率曲线（实际记录）">
            {samplesQ.data ? (
              <TrainingChart samples={samplesQ.data.content} />
            ) : (
              <Spinner />
            )}
          </Card>
          <Card title="最佳滚动平均功率">
            <BestRollingTable rows={d.session.bestRolling ?? []} />
          </Card>
        </div>
      )}

      {d.session && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-4">
          <Card title="心率区间">
            <ZoneBar rows={d.session.hrZoneDistribution ?? []} unit="%" />
          </Card>
          <Card title="功率区间">
            <ZoneBar rows={d.session.powerZoneDistribution ?? []} unit="W" />
          </Card>
          <Card title="踏频区间">
            <ZoneBar rows={d.session.cadenceZoneDistribution ?? []} unit="rpm" />
          </Card>
        </div>
      )}

      {d.session?.tenMinSegments && d.session.tenMinSegments.length > 0 && (
        <Card title="10 分钟分段">
          <TenMinTable rows={d.session.tenMinSegments} />
        </Card>
      )}
    </>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-3">
      <div className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </div>
      <div className="text-lg font-semibold text-slate-900 mt-0.5">{value}</div>
    </div>
  );
}

function BestRollingTable({ rows }: { rows: any[] }) {
  if (!rows || rows.length === 0) {
    return <p className="text-sm text-slate-500 italic">没有最佳功率数据。</p>;
  }
  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="text-xs text-slate-500 uppercase tracking-wide">
          <th className="text-left py-1">窗口</th>
          <th className="text-right py-1">平均功率</th>
          <th className="text-right py-1">位置</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.windowSec} className="border-t border-slate-100">
            <td className="py-1 text-slate-700">
              {r.windowSec < 60
                ? `${r.windowSec}s`
                : `${Math.floor(r.windowSec / 60)} 分钟`}
            </td>
            <td className="py-1 text-right text-slate-900 font-mono">
              {Math.round(r.avgPower)} W
            </td>
            <td className="py-1 text-right text-slate-500 font-mono">
              {formatOffset(r.atOffsetSec)}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function TenMinTable({ rows }: { rows: any[] }) {
  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="text-xs text-slate-500 uppercase tracking-wide">
          <th className="text-left py-1">分段</th>
          <th className="text-right py-1">功率</th>
          <th className="text-right py-1">心率</th>
          <th className="text-right py-1">踏频</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.label} className="border-t border-slate-100">
            <td className="py-1 text-slate-700">{r.label}</td>
            <td className="py-1 text-right font-mono text-slate-900">
              {r.avgPower != null ? `${Math.round(r.avgPower)} W` : "-"}
            </td>
            <td className="py-1 text-right font-mono text-slate-700">
              {r.avgHr != null ? `${Math.round(r.avgHr)} bpm` : "-"}
            </td>
            <td className="py-1 text-right font-mono text-slate-700">
              {r.avgCadence != null ? `${Math.round(r.avgCadence)} rpm` : "-"}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function formatOffset(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}
