import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/features/auth/authStore";
import { ErrorBanner, PageHeader, Spinner } from "@/components/ui";
import { adminApi, type AdminUserDto, type UserRole, type UserStatus } from "./adminApi";

const roleLabels: Record<UserRole, string> = {
  USER: "普通用户",
  ADMIN: "管理员",
};

const statusLabels: Record<UserStatus, string> = {
  ACTIVE: "启用",
  DISABLED: "停用",
};

/**
 * Admin user management. The /api/v1/admin/** endpoints are gated on
 * ROLE_ADMIN, so we also gate the page client-side on the local user
 * role: non-admins see a "forbidden" hint instead of a confusing 403.
 */
export function AdminUsersPage() {
  const me = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const [q, setQ] = useState("");
  const [role, setRole] = useState<"" | UserRole>("");
  const [status, setStatus] = useState<"" | UserStatus>("");

  const list = useQuery({
    queryKey: ["admin", "users", { q, role, status }],
    queryFn: () =>
      adminApi.list({
        q: q || undefined,
        role: role || undefined,
        status: status || undefined,
        page: 0,
        size: 100,
      }),
  });

  const patch = useMutation({
    mutationFn: (args: { id: string; role: UserRole; status: UserStatus }) =>
      adminApi.patch(args.id, { role: args.role, status: args.status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "users"] }),
  });

  const remove = useMutation({
    mutationFn: (id: string) => adminApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "users"] }),
  });

  const isAdmin = me?.role === "ADMIN";

  if (!isAdmin) {
    return (
      <div className="p-6 border border-amber-200 bg-amber-50 text-amber-800 rounded">
        当前账号不是管理员，无法访问用户管理。
      </div>
    );
  }

  return (
    <>
      <PageHeader
        title="用户管理"
        description="搜索、筛选和管理用户账号。设为管理员后可访问本页面；停用账号会暂停登录但保留数据。"
      />

      {patch.error && <ErrorBanner message={(patch.error as Error).message} />}
      {remove.error && <ErrorBanner message={(remove.error as Error).message} />}
      {list.error && <ErrorBanner message={(list.error as Error).message} />}
      {list.isLoading && <Spinner />}

      <div className="flex gap-2 mb-3">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="邮箱或显示名"
          className="flex-1 px-3 py-1.5 text-sm border border-slate-300 rounded"
        />
        <select
          value={role}
          onChange={(e) => setRole(e.target.value as "" | UserRole)}
          className="px-3 py-1.5 text-sm border border-slate-300 rounded"
        >
          <option value="">全部角色</option>
          <option value="USER">{roleLabels.USER}</option>
          <option value="ADMIN">{roleLabels.ADMIN}</option>
        </select>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as "" | UserStatus)}
          className="px-3 py-1.5 text-sm border border-slate-300 rounded"
        >
          <option value="">全部状态</option>
          <option value="ACTIVE">{statusLabels.ACTIVE}</option>
          <option value="DISABLED">{statusLabels.DISABLED}</option>
        </select>
      </div>

      <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="text-left px-3 py-2">邮箱</th>
              <th className="text-left px-3 py-2">显示名</th>
              <th className="text-left px-3 py-2">角色</th>
              <th className="text-left px-3 py-2">状态</th>
              <th className="text-left px-3 py-2">创建时间</th>
              <th className="text-right px-3 py-2">操作</th>
            </tr>
          </thead>
          <tbody>
            {list.data?.content.map((u) => (
              <UserRow
                key={u.id}
                user={u}
                isMe={me?.id === u.id}
                onChangeRole={(r) => patch.mutate({ id: u.id, role: r, status: u.status })}
                onChangeStatus={(s) => patch.mutate({ id: u.id, role: u.role, status: s })}
                onDelete={() => {
                  if (confirm(`删除 ${u.email}？这会清除该用户的数据。`)) remove.mutate(u.id);
                }}
              />
            ))}
            {list.data && list.data.content.length === 0 && (
              <tr>
                <td colSpan={6} className="text-center text-slate-500 py-6">
                  当前筛选条件下没有用户。
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function UserRow({
  user,
  isMe,
  onChangeRole,
  onChangeStatus,
  onDelete,
}: {
  user: AdminUserDto;
  isMe: boolean;
  onChangeRole: (r: UserRole) => void;
  onChangeStatus: (s: UserStatus) => void;
  onDelete: () => void;
}) {
  const createdAt = useMemo(
    () => new Date(user.createdAt).toLocaleDateString(),
    [user.createdAt],
  );
  return (
    <tr className="border-t border-slate-100">
      <td className="px-3 py-2 text-slate-700 font-mono text-xs">{user.email}</td>
      <td className="px-3 py-2 text-slate-900">{user.displayName}</td>
      <td className="px-3 py-2">
        <select
          value={user.role}
          onChange={(e) => onChangeRole(e.target.value as UserRole)}
          className="px-2 py-0.5 text-xs border border-slate-300 rounded"
        >
          <option value="USER">{roleLabels.USER}</option>
          <option value="ADMIN">{roleLabels.ADMIN}</option>
        </select>
      </td>
      <td className="px-3 py-2">
        <select
          value={user.status}
          onChange={(e) => onChangeStatus(e.target.value as UserStatus)}
          className="px-2 py-0.5 text-xs border border-slate-300 rounded"
        >
          <option value="ACTIVE">{statusLabels.ACTIVE}</option>
          <option value="DISABLED">{statusLabels.DISABLED}</option>
        </select>
      </td>
      <td className="px-3 py-2 text-slate-500 text-xs">{createdAt}</td>
      <td className="px-3 py-2 text-right">
        <button
          type="button"
          onClick={onDelete}
          disabled={isMe}
          title={isMe ? "不能删除自己的账号" : undefined}
          className="text-xs px-2 py-1 rounded border border-slate-300 hover:bg-slate-50 text-slate-500 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          删除
        </button>
      </td>
    </tr>
  );
}
