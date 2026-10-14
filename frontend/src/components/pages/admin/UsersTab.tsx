import { adminApi } from "@/client/api/admin-api"
import { getErrorMessage } from "@/client/errors"
import type { AdminUserRow } from "@/client/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/contexts/auth-context"
import { queryKeys } from "@/hooks/query-keys"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { CheckCircle2, Pencil, Plus, Search, Trash2, XCircle } from "lucide-react"
import { useMemo, useState } from "react"
import { toast } from "sonner"
import { AdminQueryError } from "./AdminQueryError"
import { CreateUserDialog, EditUserDialog } from "./UserDialogs"

export const adminUsersQuery = {
  queryKey: queryKeys.admin.users,
  queryFn: () => adminApi.getUsers(0, 50),
}

export function UsersTab() {
  const { user: currentUser } = useAuth()
  const queryClient = useQueryClient()
  const { data, isPending, isError, error } = useQuery(adminUsersQuery)
  const [search, setSearch] = useState("")
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<AdminUserRow | null>(null)

  const users = data?.content ?? []
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return users
    return users.filter(
      (u) => u.name?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q) || String(u.id).includes(q),
    )
  }, [users, search])

  const deleteMutation = useMutation({
    mutationFn: (userId: number) => adminApi.deleteUser(userId),
    onSuccess: () => {
      toast.success("Đã xoá tài khoản người dùng thành công!")
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users })
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi xoá người dùng")),
  })

  const handleDelete = (u: AdminUserRow) => {
    if (currentUser?.id === u.id) {
      toast.error("Không thể xoá tài khoản admin đang đăng nhập của chính bạn!")
      return
    }
    if (
      window.confirm(
        `Bạn có chắc muốn xoá tài khoản '${u.name}' (${u.email})? Mọi dữ liệu chuyến đi liên quan sẽ bị xoá vĩnh viễn.`,
      )
    ) {
      deleteMutation.mutate(u.id)
    }
  }

  if (isError) return <AdminQueryError error={error} />

  return (
    <Card className="overflow-hidden border-border/80 shadow-sm">
      <div className="p-4 border-b bg-muted/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="font-bold text-sm">
              Danh sách tài khoản ({filtered.length} / {data?.totalElements ?? users.length})
            </h3>
            <Badge variant="secondary" className="text-[10px]">
              Đầy đủ Thêm / Sửa / Xoá
            </Badge>
          </div>
          <p className="text-[11px] text-muted-foreground">
            Admin có thể tạo tài khoản, đổi quyền USER/ADMIN, khóa/mở và xoá tài khoản
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative w-48 sm:w-64">
            <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" aria-hidden />
            <Input
              aria-label="Tìm tài khoản"
              placeholder="Tìm theo tên, email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-8 h-8 text-xs bg-background"
            />
          </div>
          <Button size="sm" onClick={() => setIsCreateOpen(true)} className="h-8 text-xs gap-1.5 cursor-pointer">
            <Plus className="h-3.5 w-3.5" /> Thêm User
          </Button>
        </div>
      </div>

      <div className="overflow-x-auto text-xs">
        <table className="w-full text-left">
          <thead className="bg-muted/50 text-muted-foreground font-semibold">
            <tr>
              <th className="p-3">ID</th>
              <th className="p-3">Họ tên &amp; Email</th>
              <th className="p-3">Vai trò</th>
              <th className="p-3">Trạng thái</th>
              <th className="p-3">Số chuyến</th>
              <th className="p-3">Ngày tạo</th>
              <th className="p-3 text-right">Thao tác Admin</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border/40">
            {isPending ? (
              <tr>
                <td colSpan={7} className="p-8 text-center text-muted-foreground">
                  Đang tải danh sách tài khoản...
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="p-8 text-center text-muted-foreground">
                  Không tìm thấy tài khoản nào phù hợp.
                </td>
              </tr>
            ) : (
              filtered.map((u) => {
                const isSelf = currentUser?.id === u.id
                return (
                  <tr key={u.id} className="hover:bg-muted/20 transition-colors">
                    <td className="p-3 font-mono font-medium text-muted-foreground">{u.id}</td>
                    <td className="p-3">
                      <div className="flex items-center gap-2.5">
                        <div
                          aria-hidden
                          className="h-8 w-8 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold shrink-0"
                        >
                          {u.name?.charAt(0) || "U"}
                        </div>
                        <div>
                          <div className="font-semibold text-foreground flex items-center gap-1.5">
                            {u.name}
                            {isSelf && <Badge className="text-[9px] px-1 py-0 bg-primary/20 text-primary">Bạn</Badge>}
                          </div>
                          <div className="text-[11px] text-muted-foreground font-mono">{u.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className="p-3">
                      <Badge
                        variant={u.role === "ADMIN" ? "default" : "secondary"}
                        className={`text-[10px] ${u.role === "ADMIN" ? "bg-amber-500/20 text-amber-600 dark:text-amber-400 border border-amber-500/30" : ""}`}
                      >
                        {u.role === "ADMIN" ? "👑 ADMIN" : "👤 USER"}
                      </Badge>
                    </td>
                    <td className="p-3">
                      {u.active !== false ? (
                        <span className="text-emerald-500 font-medium flex items-center gap-1">
                          <CheckCircle2 className="h-3.5 w-3.5" aria-hidden /> Hoạt động
                        </span>
                      ) : (
                        <span className="text-destructive font-medium flex items-center gap-1">
                          <XCircle className="h-3.5 w-3.5" aria-hidden /> Tạm khóa
                        </span>
                      )}
                    </td>
                    <td className="p-3 font-semibold">{u.tripCount || 0}</td>
                    <td className="p-3 text-muted-foreground text-[11px]">
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString("vi-VN") : "--"}
                    </td>
                    <td className="p-3 text-right">
                      <div className="inline-flex items-center gap-1">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setEditingUser(u)}
                          className="h-7 px-2.5 text-xs gap-1 border-primary/30 text-primary hover:bg-primary/10 cursor-pointer"
                          aria-label={`Sửa tài khoản ${u.name}`}
                        >
                          <Pencil className="h-3 w-3" /> Sửa
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleDelete(u)}
                          disabled={isSelf || deleteMutation.isPending}
                          className="h-7 px-2.5 text-xs gap-1 border-destructive/30 text-destructive hover:bg-destructive/10 cursor-pointer disabled:opacity-40"
                          aria-label={isSelf ? "Không thể xoá chính bạn" : `Xoá tài khoản ${u.name}`}
                        >
                          <Trash2 className="h-3 w-3" /> Xoá
                        </Button>
                      </div>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      <CreateUserDialog isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} />
      <EditUserDialog user={editingUser} onClose={() => setEditingUser(null)} />
    </Card>
  )
}
