import { adminApi } from "@/client/api/admin-api"
import { getErrorMessage } from "@/client/errors"
import type { AdminUserRow, UserRole } from "@/client/types"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { queryKeys } from "@/hooks/query-keys"
import { MIN_PASSWORD_LENGTH } from "@/lib/validation"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { Pencil, User as UserIcon } from "lucide-react"
import { useEffect, useState } from "react"
import { toast } from "sonner"

const SELECT_CLASS = "w-full rounded-lg border border-border bg-background px-3 py-2 text-xs outline-none"

export function CreateUserDialog({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [name, setName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [role, setRole] = useState<UserRole>("USER")

  const createMutation = useMutation({
    mutationFn: () => adminApi.createUser({ name, email, password, role }),
    onSuccess: () => {
      toast.success("Đã tạo người dùng mới thành công!")
      setName("")
      setEmail("")
      setPassword("")
      setRole("USER")
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users })
      onClose()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi tạo người dùng")),
  })

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="p-2 rounded-lg bg-primary/10 text-primary">
            <UserIcon className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <DialogTitle>Thêm Tài Khoản Mới</DialogTitle>
            <DialogDescription>Tạo tài khoản người dùng hoặc quản trị viên</DialogDescription>
          </div>
        </DialogHeader>

        <form
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate()
          }}
          className="flex flex-col min-h-0"
        >
          <DialogBody className="space-y-4 text-xs">
            <div className="space-y-1.5">
              <label htmlFor="new-user-name" className="text-xs font-medium">
                Họ và tên
              </label>
              <Input
                id="new-user-name"
                required
                maxLength={120}
                placeholder="VD: Nguyễn Văn B"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <label htmlFor="new-user-email" className="text-xs font-medium">
                Địa chỉ Email
              </label>
              <Input
                id="new-user-email"
                type="email"
                required
                placeholder="user@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <label htmlFor="new-user-password" className="text-xs font-medium">
                Mật khẩu ban đầu
              </label>
              <Input
                id="new-user-password"
                type="password"
                required
                minLength={MIN_PASSWORD_LENGTH}
                maxLength={50}
                autoComplete="new-password"
                placeholder={`Tối thiểu ${MIN_PASSWORD_LENGTH} ký tự`}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <label htmlFor="new-user-role" className="text-xs font-medium">
                Vai trò (Role)
              </label>
              <select
                id="new-user-role"
                className={SELECT_CLASS}
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
              >
                <option value="USER">Người dùng thường (USER)</option>
                <option value="ADMIN">Quản trị viên (ADMIN)</option>
              </select>
            </div>
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="outline" size="sm" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" size="sm" disabled={createMutation.isPending}>
              {createMutation.isPending ? "Đang tạo..." : "Xác nhận tạo tài khoản"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export function EditUserDialog({ user, onClose }: { user: AdminUserRow | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [name, setName] = useState("")
  const [role, setRole] = useState<UserRole>("USER")
  const [active, setActive] = useState(true)
  const [password, setPassword] = useState("")

  useEffect(() => {
    if (!user) return
    setName(user.name || "")
    setRole(user.role || "USER")
    setActive(user.active !== false)
    setPassword("")
  }, [user])

  const updateMutation = useMutation({
    mutationFn: (userId: number) =>
      adminApi.updateUser(userId, { name, role, active, password: password.trim() || undefined }),
    onSuccess: () => {
      toast.success("Đã cập nhật thông tin tài khoản!")
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.users })
      onClose()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi cập nhật tài khoản")),
  })

  return (
    <Dialog open={user != null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="p-2 rounded-lg bg-primary/10 text-primary">
            <Pencil className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <DialogTitle>Chỉnh Sửa Tài Khoản</DialogTitle>
            <DialogDescription>{user?.email}</DialogDescription>
          </div>
        </DialogHeader>

        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (user) updateMutation.mutate(user.id)
          }}
          className="flex flex-col min-h-0"
        >
          <DialogBody className="space-y-4 text-xs">
            <div className="space-y-1.5">
              <label htmlFor="edit-user-name" className="text-xs font-medium">
                Họ và tên
              </label>
              <Input
                id="edit-user-name"
                required
                maxLength={120}
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <label htmlFor="edit-user-role" className="text-xs font-medium">
                Vai trò (Role)
              </label>
              <select
                id="edit-user-role"
                className={SELECT_CLASS}
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
              >
                <option value="USER">USER (Người dùng thường)</option>
                <option value="ADMIN">ADMIN (Quản trị viên)</option>
              </select>
            </div>
            <fieldset className="space-y-1.5">
              <legend className="text-xs font-medium">Trạng thái hoạt động</legend>
              <div className="flex items-center gap-4 pt-1">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="radio" name="activeStatus" checked={active} onChange={() => setActive(true)} />
                  <span>Kích hoạt</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="radio" name="activeStatus" checked={!active} onChange={() => setActive(false)} />
                  <span className="text-muted-foreground">Tạm khóa</span>
                </label>
              </div>
            </fieldset>
            <div className="space-y-1.5 pt-2 border-t">
              <label htmlFor="edit-user-password" className="text-xs font-medium">
                Đặt lại mật khẩu mới (bỏ trống nếu giữ nguyên)
              </label>
              <Input
                id="edit-user-password"
                type="password"
                minLength={MIN_PASSWORD_LENGTH}
                maxLength={50}
                autoComplete="new-password"
                placeholder={`Để trống nếu không đổi (tối thiểu ${MIN_PASSWORD_LENGTH} ký tự)`}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          </DialogBody>
          <DialogFooter>
            <Button type="button" variant="outline" size="sm" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" size="sm" disabled={updateMutation.isPending}>
              {updateMutation.isPending ? "Đang lưu..." : "Lưu thay đổi"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
