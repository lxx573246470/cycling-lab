import type { ReactNode } from "react";

export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="flex items-start justify-between mb-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
        {description && <p className="text-sm text-slate-500 mt-1">{description}</p>}
      </div>
      {actions && <div className="flex gap-2">{actions}</div>}
    </div>
  );
}

export function Card({
  title,
  children,
  actions,
  className = "",
}: {
  title?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={`bg-white border border-slate-200 rounded-lg p-5 ${className}`}>
      {title && (
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">{title}</h2>
          {actions}
        </div>
      )}
      {children}
    </div>
  );
}

export function Field({
  label,
  error,
  hint,
  className,
  children,
}: {
  label: string;
  error?: string;
  hint?: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <label className={`block ${className ?? ""}`}>
      <span className="text-sm text-slate-700 mb-1 block">{label}</span>
      {children}
      {hint && !error && <span className="text-xs text-slate-400 mt-1 block">{hint}</span>}
      {error && <span className="text-xs text-red-600 mt-1 block">{error}</span>}
    </label>
  );
}

export function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="p-3 text-sm text-red-700 bg-red-50 border border-red-200 rounded">
      {message}
    </div>
  );
}

export function Spinner() {
  return (
    <div className="flex items-center justify-center p-8 text-sm text-slate-500">
      <div className="animate-pulse">Loading…</div>
    </div>
  );
}
