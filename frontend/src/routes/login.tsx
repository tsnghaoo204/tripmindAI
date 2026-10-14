import { authApi } from "@/client/api/auth-api"
import { getErrorMessage } from "@/client/errors"
import { Logo } from "@/components/common/Logo"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/contexts/auth-context"
import { getToken } from "@/lib/auth-storage"
import { Link, createFileRoute, redirect, useRouter } from "@tanstack/react-router"
import { useState } from "react"
import { toast } from "sonner"

export const Route = createFileRoute("/login")({
  validateSearch: (search: Record<string, unknown>): { redirect?: string } => ({
    redirect: typeof search.redirect === "string" ? search.redirect : undefined,
  }),
  beforeLoad: () => {
    if (getToken()) throw redirect({ to: "/" })
  },
  component: LoginPage,
})

// Only follow same-app relative paths after login
function safeRedirect(target?: string) {
  return target?.startsWith("/") && !target.startsWith("//") ? target : "/"
}

function LoginPage() {
  const router = useRouter()
  const { redirect: redirectTo } = Route.useSearch()
  const { login } = useAuth()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const { accessToken, user } = await authApi.login({ email, password })
      login(accessToken, user)
      toast.success("Đăng nhập thành công!")
      router.history.push(safeRedirect(redirectTo))
    } catch (err) {
      toast.error(getErrorMessage(err, "Đăng nhập thất bại"))
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
          <CardDescription>Chào mừng trở lại! Hãy nhập thông tin để tiếp tục lên kế hoạch du lịch.</CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-4">
            <div className="space-y-1.5">
              <label htmlFor="login-email" className="text-sm font-medium">
                Email
              </label>
              <Input
                id="login-email"
                type="email"
                autoComplete="email"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="space-y-1.5">
              <label htmlFor="login-password" className="text-sm font-medium">
                Mật khẩu
              </label>
              <Input
                id="login-password"
                type="password"
                autoComplete="current-password"
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
