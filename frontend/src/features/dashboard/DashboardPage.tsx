import { useAuthStore } from "@/features/auth/authStore";

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">仪表盘</h1>
        <p className="text-slate-500 mt-1">欢迎回来，{user?.displayName ?? "骑手"}。</p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {placeholderCards.map((c) => (
          <div key={c.title} className="bg-white border border-slate-200 rounded-lg p-5">
            <div className="text-xs uppercase tracking-wide text-slate-400">{c.tag}</div>
            <div className="mt-1 text-lg font-semibold text-slate-900">{c.title}</div>
            <div className="text-sm text-slate-500 mt-2">{c.body}</div>
          </div>
        ))}
      </div>
      <div className="bg-white border border-dashed border-slate-300 rounded-lg p-6 text-sm text-slate-500">
        M0 阶段：脚手架已就绪。后续里程碑（M1 起）会在这里落地：档案、训练课库、周计划、FIT 上传、ZWO 生成器、AI 助手。
      </div>
    </div>
  );
}

const placeholderCards = [
  { tag: "M1", title: "档案 & 训练课库", body: "骑手档案、阈值、设备；可复用训练课模板。" },
  { tag: "M2", title: "ZWO 课程文件生成器", body: "表单配置热身/间歇，预览并下载 .zwo。" },
  { tag: "M3", title: "FIT 上传 + 分析图表", body: "上传 .fit → 自动解析 → 心率/功率/踏频曲线。" },
  { tag: "M4", title: "复盘 & 笔记", body: "周复盘 Markdown 编辑器 + 训练笔记。" },
  { tag: "M5", title: "AI 辅助", body: "基于档案与近期训练生成周计划 / 训练解读。" },
  { tag: "M6", title: "多用户完善", body: "用户管理、权限、审计。" },
];
