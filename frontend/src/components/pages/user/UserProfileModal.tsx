import { authApi } from "@/client/api/auth-api"
import { getErrorMessage } from "@/client/errors"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useAuth } from "@/contexts/auth-context"
import { getToken, saveSession } from "@/lib/auth-storage"
import { KeyRound, Shield, User as UserIcon } from "lucide-react"
import { useEffect, useState } from "react"
import { toast } from "sonner"

interface UserProfileModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function UserProfileModal({ open, onOpenChange }: UserProfileModalProps) {
  const { user } = useAuth()

  // Profile form state
  const [name, setName] = useState(user?.name || "")
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl || "")
  const [profileLoading, setProfileLoading] = useState(false)

  // Password form state
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [passwordLoading, setPasswordLoading] = useState(false)

  useEffect(() => {
    if (open && user) {
      setName(user.name || "")
      setAvatarUrl(user.avatarUrl || "")
      setCurrentPassword("")
      setNewPassword("")
      setConfirmPassword("")
    }
  }, [open, user])

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) {
      toast.error("Vui lòng nhập họ và tên")
      return
    }

    setProfileLoading(true)
    try {
      const updatedUser = await authApi.updateProfile({
        name: name.trim(),
        avatarUrl: avatarUrl.trim() || undefined,
      })
      const token = getToken()
      if (token) {
        saveSession(token, updatedUser)
      }
      toast.success("Cập nhật thông tin thành công!")
      onOpenChange(false)
    } catch (err) {
      toast.error(getErrorMessage(err, "Không thể cập nhật thông tin"))
    } finally {
      setProfileLoading(false)
    }
  }

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!currentPassword) {
      toast.error("Vui lòng nhập mật khẩu hiện tại")
      return
    }
    if (newPassword.length < 8) {
      toast.error("Mật khẩu mới phải có tối thiểu 8 ký tự")
      return
    }
    if (newPassword !== confirmPassword) {
      toast.error("Mật khẩu xác nhận không khớp")
      return
    }

    setPasswordLoading(true)
    try {
      await authApi.changePassword({
        currentPassword,
        newPassword,
      })
      toast.success("Đổi mật khẩu thành công! Phiên đăng nhập đã được gia hạn.")
      setCurrentPassword("")
      setNewPassword("")
      setConfirmPassword("")
      onOpenChange(false)
    } catch (err) {
      toast.error(getErrorMessage(err, "Không thể đổi mật khẩu"))
    } finally {
      setPasswordLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-full bg-primary/10 text-primary shrink-0">
              <UserIcon className="h-5 w-5" aria-hidden />
            </div>
            <div>
              <DialogTitle>Tài khoản cá nhân</DialogTitle>
              <DialogDescription className="mt-0.5">
                Quản lý thông tin hồ sơ và bảo mật tài khoản của bạn.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <DialogBody className="py-2">
          <Tabs defaultValue="profile" className="space-y-4">
            <TabsList className="w-full grid grid-cols-2">
              <TabsTrigger value="profile" className="gap-2">
                <UserIcon className="h-4 w-4" aria-hidden />
                Thông tin hồ sơ
              </TabsTrigger>
              <TabsTrigger value="security" className="gap-2">
                <KeyRound className="h-4 w-4" aria-hidden />
                Đổi mật khẩu
              </TabsTrigger>
            </TabsList>

            {/* Tab 1: Profile */}
            <TabsContent value="profile" className="space-y-4 pt-1">
              <form onSubmit={handleUpdateProfile} className="space-y-4">
                <div className="flex items-center gap-4 p-3 rounded-xl bg-muted/40 border border-border">
                  <div className="relative h-14 w-14 rounded-full overflow-hidden bg-primary/10 flex items-center justify-center text-primary font-bold text-lg border-2 border-primary/20 shrink-0">
                    {avatarUrl ? (
                      <img
                        src={avatarUrl}
                        alt="Avatar preview"
                        className="h-full w-full object-cover"
                        onError={(e) => {
                          e.currentTarget.style.display = "none"
                        }}
                      />
                    ) : (
                      user?.name?.charAt(0).toUpperCase() || "U"
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-sm truncate">{user?.name}</p>
                      {user?.role === "ADMIN" && (
                        <Badge
                          variant="outline"
                          className="text-[10px] bg-amber-500/10 text-amber-600 border-amber-500/30"
                        >
                          Admin
                        </Badge>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground truncate">{user?.email}</p>
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label
                    htmlFor="user-name"
                    className="text-xs font-semibold uppercase tracking-wider text-muted-foreground"
                  >
                    Họ và tên
                  </label>
                  <Input
                    id="user-name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Nhập họ và tên..."
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label
                    htmlFor="user-avatar"
                    className="text-xs font-semibold uppercase tracking-wider text-muted-foreground"
                  >
                    Link ảnh đại diện (URL https)
                  </label>
                  <Input
                    id="user-avatar"
                    type="url"
                    value={avatarUrl}
                    onChange={(e) => setAvatarUrl(e.target.value)}
                    placeholder="https://example.com/avatar.jpg (để trống nếu muốn xóa)"
                  />
                  <p className="text-[11px] text-muted-foreground">
                    Chỉ chấp nhận link ảnh an toàn bắt đầu bằng https://.
                  </p>
                </div>

                <div className="space-y-1.5">
                  <label
                    htmlFor="user-email-readonly"
                    className="text-xs font-semibold uppercase tracking-wider text-muted-foreground"
                  >
                    Địa chỉ Email (Chỉ đọc)
                  </label>
                  <Input
                    id="user-email-readonly"
                    value={user?.email || ""}
                    disabled
                    className="bg-muted/50 cursor-not-allowed"
                  />
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                    Hủy
                  </Button>
                  <Button type="submit" disabled={profileLoading}>
                    {profileLoading ? "Đang lưu..." : "Lưu thay đổi"}
                  </Button>
                </div>
              </form>
            </TabsContent>

            {/* Tab 2: Security */}
            <TabsContent value="security" className="space-y-4 pt-1">
              <form onSubmit={handleChangePassword} className="space-y-4">
                <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-xs text-amber-800 dark:text-amber-300 flex items-start gap-2">
                  <Shield className="h-4 w-4 shrink-0 mt-0.5" />
                  <span>
                    Khi đổi mật khẩu thành công, toàn bộ token đăng nhập cũ sẽ bị thu hồi server-side để đảm bảo an toàn
                    tuyệt đối.
                  </span>
                </div>

                <div className="space-y-1.5">
                  <label
                    htmlFor="current-pwd"
                    className="text-xs font-semibold uppercase tracking-wider text-muted-foreground"
                  >
                    Mật khẩu hiện tại
                  </label>
                  <Input
                    id="current-pwd"
                    type="password"
                    autoComplete="current-password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label
                    htmlFor="new-pwd"
                    className="text-xs font-semibold uppercase tracking-wider text-muted-foreground"
                  >
                    Mật khẩu mới (tối thiểu 8 ký tự)
                  </label>
                  <Input
                    id="new-pwd"
                    type="password"
                    autoComplete="new-password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="••••••••"
                    minLength={8}
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label
                    htmlFor="confirm-pwd"
                    className="text-xs font-semibold uppercase tracking-wider text-muted-foreground"
                  >
                    Nhập lại mật khẩu mới
                  </label>
                  <Input
                    id="confirm-pwd"
                    type="password"
                    autoComplete="new-password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="••••••••"
                    minLength={8}
                    required
                  />
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                    Hủy
                  </Button>
                  <Button type="submit" disabled={passwordLoading}>
                    {passwordLoading ? "Đang xử lý..." : "Cập nhật mật khẩu"}
                  </Button>
                </div>
              </form>
            </TabsContent>
          </Tabs>
        </DialogBody>
      </DialogContent>
    </Dialog>
  )
}
