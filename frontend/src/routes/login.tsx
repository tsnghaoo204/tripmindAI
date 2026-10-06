import { createFileRoute, Link, useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Logo } from "@/components/common/Logo"
import { useAuth } from "@/contexts/auth-context"
import { toast } from "sonner"
import { apiClient } from "@/client/client"

export const Route = createFileRoute("/login")({
  component: LoginPage,
})

function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const res = await apiClient.post("/api/auth/login", { email, password })
      if (res.data?.success && res.data?.data) {
        const { accessToken, user } = res.data.data
        login(accessToken, user)
        toast.success("Đăng nhập thành công!")
        navigate({ to: "/" })
      } else {
        toast.error(res.data?.message || "Đăng nhập thất bại")
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Không thể kết nối đến máy chủ")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-muted/30">
      <Card className="w-full max-w-md shadow-lg border-border">
        <CardHeader className="text-center space-y-2">
          <div className="flex justify-center mb-2">
            <Logo />
          </div>
          <CardTitle className="text-2xl font-bold">Đăng nhập tài khoản</CardTitle>
          <CardDescription>
            Chào mừng trở lại! Hãy nhập thông tin để tiếp tục lên kế hoạch du lịch.
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-sm font-medium">Email</label>
              <Input
                type="email"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium">Mật khẩu</label>
              <Input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
          </CardContent>
          <CardFooter className="flex flex-col gap-4">
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? "Đang xử lý..." : "Đăng nhập"}
            </Button>

            {/* Quick Demo Accounts */}
            <div className="w-full border-t border-border/60 pt-3 space-y-2">
              <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider block text-center">
                Đăng nhập nhanh tài khoản mẫu:
              </span>
              <div className="grid grid-cols-2 gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="text-xs border-amber-500/30 text-amber-600 dark:text-amber-400 hover:bg-amber-500/10"
                  onClick={() => {
                    setEmail("admin@tripmind.com")
                    setPassword("Admin@123456")
                  }}
                >
                  👑 Admin (Quản trị)
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="text-xs border-primary/30 text-primary hover:bg-primary/10"
                  onClick={() => {
                    setEmail("john@example.com")
                    setPassword("password123")
                  }}
                >
                  👤 User Thường
                </Button>
              </div>
            </div>

            <p className="text-xs text-center text-muted-foreground">
              Chưa có tài khoản?{" "}
              <Link to="/register" className="text-primary hover:underline font-semibold">
                Đăng ký ngay
              </Link>
            </p>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}
