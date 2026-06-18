import { useQuery } from "@tanstack/react-query";
import { profileApi, type DerivedZones } from "./profileApi";
import { Card, ErrorBanner, PageHeader, Spinner } from "@/components/ui";

const ZONE_NAMES = ["Z1", "Z2", "Z3", "Z4", "Z5", "Z6", "Z7"];

export function ProfileZonesPage() {
  const query = useQuery({
    queryKey: ["profile", "derived-zones"],
    queryFn: () => profileApi.derivedZones(),
  });

  if (query.isLoading) return <Spinner />;
  if (query.error) {
    return (
      <>
        <PageHeader
          title="训练分区"
          description="根据骑手档案计算心率区间和 Coggan 7 区功率区间。"
        />
        <ErrorBanner message={(query.error as Error).message} />
      </>
    );
  }
  const z = query.data as DerivedZones;

  return (
    <>
      <PageHeader
        title="训练分区"
        description={`基于最大心率 ${z.maxHr} 和 FTP ${z.ftp} 计算，更新时间：${z.computedAt}。`}
      />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card title="心率区间">
          <ZoneTable
            rows={z.hrZones.map((row) => ({
              zone: row.zone,
              name: row.name,
              pctLow: row.low,
              pctHigh: row.high,
              lo: row.bpmLow,
              hi: row.bpmHigh,
              unit: "bpm",
            }))}
          />
        </Card>
        <Card title="功率区间">
          <ZoneTable
            rows={z.powerZones.map((row) => ({
              zone: row.zone,
              name: row.name,
              pctLow: row.low,
              pctHigh: row.high,
              lo: row.wattsLow,
              hi: row.wattsHigh,
              unit: "W",
            }))}
          />
        </Card>
        <Card title="踏频范围">
          <div className="text-3xl font-bold text-slate-900">
            {z.cadenceRange.low}–{z.cadenceRange.high} <span className="text-base text-slate-500">rpm</span>
          </div>
          <p className="text-sm text-slate-500 mt-2">
            在骑手档案页修改踏频上下限后，这里会自动更新。
          </p>
        </Card>
      </div>
      <div className="mt-4 text-xs text-slate-400">
        区间编号：{ZONE_NAMES.join(", ")}。修改档案中的 FTP 或最大心率后会重新计算。
      </div>
    </>
  );
}

function ZoneTable({
  rows,
}: {
  rows: {
    zone: number;
    name: string;
    pctLow: number;
    pctHigh: number;
    lo: number;
    hi: number;
    unit: string;
  }[];
}) {
  const maxHi = Math.max(...rows.map((r) => r.hi));
  return (
    <table className="w-full text-sm">
      <thead className="text-xs uppercase text-slate-400">
        <tr>
          <th className="text-left py-1 pr-2">Z</th>
          <th className="text-left py-1 pr-2">名称</th>
          <th className="text-left py-1 pr-2">范围</th>
          <th className="text-left py-1 pr-2">比例</th>
          <th className="text-left py-1 pr-2 w-24">图示</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => {
          const widthPct = Math.max(2, (r.hi / maxHi) * 100);
          return (
            <tr key={r.zone} className="border-t border-slate-100">
              <td className="py-1 pr-2 font-mono">{r.zone}</td>
              <td className="py-1 pr-2">{r.name}</td>
              <td className="py-1 pr-2 font-mono">
                {r.lo}–{r.hi} {r.unit}
              </td>
              <td className="py-1 pr-2 text-xs text-slate-500">
                {Math.round(r.pctLow * 100)}–{Math.round(r.pctHigh * 100)}%
              </td>
              <td className="py-1 pr-2">
                <div className="h-2 bg-slate-100 rounded">
                  <div
                    className="h-2 rounded bg-brand-500"
                    style={{ width: `${widthPct}%` }}
                  />
                </div>
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
