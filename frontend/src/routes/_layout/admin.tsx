import { createFileRoute, useNavigate, Link } from "@tanstack/react-router"
import { useState, useEffect, useMemo } from "react"
import { useAuth } from "@/contexts/auth-context"
import { adminApi, type AiUsageMetrics, type ToolExecutionAudit } from "@/client/api/admin-api"
import { CreateTripModal } from "@/components/pages/trips/CreateTripModal"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  ShieldCheck,
  Cpu,
  BarChart3,
  Users,
  Compass,
  Clock,
  Wrench,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  RotateCw,
  Plus,
  Pencil,
  Trash2,
  X,
  Eye,
  Lock,
  Mail,
  User as UserIcon,
  Search,
  DollarSign,
  Calendar,
} from "lucide-react"
import { toast } from "sonner"

export const Route = createFileRoute("/_layout/admin")({
  component: AdminPage,
})

function AdminPage() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const [activeTab, setActiveTab] = useState<"users" | "trips" | "metrics" | "tools">("users")
  const [metrics, setMetrics] = useState<AiUsageMetrics | null>(null)
  const [tools, setTools] = useState<ToolExecutionAudit[]>([])
  const [usersList, setUsersList] = useState<any[]>([])
  const [tripsList, setTripsList] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  // Search States
  const [userSearch, setUserSearch] = useState("")
  const [tripSearch, setTripSearch] = useState("")

  // Modal States: User
  const [isCreateUserOpen, setIsCreateUserOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<any | null>(null)

  // Modal States: Trip
  const [isCreateTripOpen, setIsCreateTripOpen] = useState(false)
  const [editingTrip, setEditingTrip] = useState<any | null>(null)

  // Create User Form State
  const [createName, setCreateName] = useState("")
  const [createEmail, setCreateEmail] = useState("")
  const [createPassword, setCreatePassword] = useState("")
  const [createRole, setCreateRole] = useState<"USER" | "ADMIN">("USER")
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Edit User Form State
  const [editName, setEditName] = useState("")
  const [editRole, setEditRole] = useState<"USER" | "ADMIN">("USER")
  const [editActive, setEditActive] = useState(true)
  const [editPassword, setEditPassword] = useState("")

  // Edit Trip Form State
  const [editTripName, setEditTripName] = useState("")
  const [editTripPhase, setEditTripPhase] = useState<string>("BEFORE")
  const [editTripBudget, setEditTripBudget] = useState<string>("")

  useEffect(() => {
    if (user && user.role !== "ADMIN") {
      toast.error("Bạn không có quyền truy cập trang quản trị")
      navigate({ to: "/" })
      return
    }
    loadData()
  }, [user])

  const loadData = async () => {
    try {
      setLoading(true)
      const [aiData, toolData, userData, tripData] = await Promise.all([
        adminApi.getAiUsage().catch(() => null),
        adminApi.getToolExecutions({ page: 0, size: 40 }).catch(() => ({ content: [] })),
        adminApi.getUsers(0, 50).catch(() => ({ content: [] })),
        adminApi.getTrips(0, 50).catch(() => ({ content: [] })),
      ])
      setMetrics(aiData)
      setTools(toolData?.content || [])
      setUsersList(userData?.content || [])
      setTripsList(tripData?.content || [])
    } catch {
      toast.error("Không thể tải dữ liệu quản trị")
    } finally {
      setLoading(false)
    }
  }

  // Handle Create User
  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSubmitting(true)
    try {
      await adminApi.createUser({
        name: createName,
        email: createEmail,
        password: createPassword,
        role: createRole,
      })
      toast.success("Đã tạo người dùng mới thành công!")
      setIsCreateUserOpen(false)
      setCreateName("")
      setCreateEmail("")
      setCreatePassword("")
      loadData()
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi tạo người dùng")
    } finally {
      setIsSubmitting(false)
    }
  }

  // Handle Open Edit User
  const handleOpenEditUser = (u: any) => {
    setEditingUser(u)
    setEditName(u.name || "")
    setEditRole(u.role || "USER")
    setEditActive(u.active !== false)
    setEditPassword("")
  }

  // Handle Save Edit User
  const handleSaveEditUser = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editingUser) return
    setIsSubmitting(true)
    try {
      await adminApi.updateUser(editingUser.id, {
        name: editName,
        role: editRole,
        active: editActive,
        password: editPassword.trim() || undefined,
      })
      toast.success("Đã cập nhật thông tin tài khoản!")
      setEditingUser(null)
      loadData()
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi cập nhật tài khoản")
    } finally {
      setIsSubmitting(false)
    }
  }

  // Handle Delete User
  const handleDeleteUser = async (u: any) => {
    if (user && user.id === u.id) {
      toast.error("Không thể xoá tài khoản admin đang đăng nhập của chính bạn!")
      return
    }
    if (!confirm(`Bạn có chắc muốn xoá tài khoản '${u.name}' (${u.email})? Mọi dữ liệu chuyến đi liên quan sẽ bị xoá vĩnh viễn.`)) {
      return
    }
    try {
      await adminApi.deleteUser(u.id)
      toast.success("Đã xoá tài khoản người dùng thành công!")
      loadData()
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi xoá người dùng")
    }
  }

  // Handle Open Edit Trip
  const handleOpenEditTrip = (t: any) => {
    setEditingTrip(t)
    setEditTripName(t.name || "")
    setEditTripPhase(t.phase || "BEFORE")
    setEditTripBudget(t.budget ? String(t.budget) : "")
  }

  // Handle Save Edit Trip
  const handleSaveEditTrip = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editingTrip) return
    setIsSubmitting(true)
    try {
      await adminApi.updateTrip(editingTrip.id, {
        name: editTripName.trim(),
        phase: editTripPhase,
        budget: editTripBudget ? Number(editTripBudget) : undefined,
      })
      toast.success("Đã cập nhật thông tin chuyến đi!")
      setEditingTrip(null)
      loadData()
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi cập nhật chuyến đi")
    } finally {
      setIsSubmitting(false)
    }
  }

  // Handle Delete Trip
  const handleDeleteTrip = async (t: any) => {
    if (!confirm(`Xác nhận xoá chuyến đi '${t.name || t.id}' khỏi hệ thống? Tất cả hoạt động, chi tiêu và checklist liên quan sẽ bị xoá.`)) {
      return
    }
    try {
      await adminApi.deleteTrip(t.id)
      toast.success("Đã xoá chuyến đi thành công!")
      loadData()
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi xoá chuyến đi")
    }
  }

  // Filtered Users
  const filteredUsers = useMemo(() => {
    if (!userSearch.trim()) return usersList
    const q = userSearch.toLowerCase()
    return usersList.filter(
      (u) =>
        u.name?.toLowerCase().includes(q) ||
        u.email?.toLowerCase().includes(q) ||
        String(u.id).includes(q),
    )
  }, [usersList, userSearch])

  // Filtered Trips
  const filteredTrips = useMemo(() => {
    if (!tripSearch.trim()) return tripsList
    const q = tripSearch.toLowerCase()
    return tripsList.filter(
      (t) =>
        t.name?.toLowerCase().includes(q) ||
        t.destinationName?.toLowerCase().includes(q) ||
        String(t.userId).includes(q) ||
        String(t.id).includes(q),
    )
  }, [tripsList, tripSearch])

  if (loading && usersList.length === 0) {
    return (
      <div className="py-24 text-center">
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary mb-3"></div>
        <p className="text-sm text-muted-foreground">Đang tải dữ liệu quản trị hệ thống...</p>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 sm:px-6 py-8 space-y-8">
      {/* Admin Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-2xl bg-gradient-to-tr from-amber-500/20 to-primary/20 text-amber-500 shadow-sm border border-amber-500/30">
            <ShieldCheck className="h-7 w-7" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-black tracking-tight">Hệ Thống Quản Trị Toàn Diện</h1>
              <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 font-bold">
                Admin Full Access
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">
              Thực hiện thêm, sửa, xoá tài khoản và kế hoạch du lịch; giám sát mô hình AI ReAct &amp; token
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto flex-wrap">
          <Button size="sm" variant="outline" onClick={loadData} className="gap-1.5 cursor-pointer">
            <RotateCw className="h-3.5 w-3.5" /> Làm mới
          </Button>
          {activeTab === "users" && (
            <Button
              size="sm"
              onClick={() => setIsCreateUserOpen(true)}
              className="gap-1.5 bg-primary text-primary-foreground shadow hover:opacity-95 cursor-pointer"
            >
              <Plus className="h-3.5 w-3.5" /> Thêm tài khoản mới
            </Button>
          )}
          {activeTab === "trips" && (
            <Button
              size="sm"
              onClick={() => setIsCreateTripOpen(true)}
              className="gap-1.5 bg-primary text-primary-foreground shadow hover:opacity-95 cursor-pointer"
            >
              <Plus className="h-3.5 w-3.5" /> Tạo kế hoạch mới
            </Button>
          )}
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="flex border-b border-border text-sm font-medium gap-2 overflow-x-auto">
        <button
          onClick={() => setActiveTab("users")}
          className={`py-3 px-4 border-b-2 font-semibold flex items-center gap-2 transition-colors cursor-pointer shrink-0 ${
            activeTab === "users"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <Users className="h-4 w-4" /> Quản Lý Tài Khoản ({usersList.length})
        </button>
        <button
          onClick={() => setActiveTab("trips")}
          className={`py-3 px-4 border-b-2 font-semibold flex items-center gap-2 transition-colors cursor-pointer shrink-0 ${
            activeTab === "trips"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <Compass className="h-4 w-4" /> Quản Lý Kế Hoạch ({tripsList.length})
        </button>
        <button
          onClick={() => setActiveTab("metrics")}
          className={`py-3 px-4 border-b-2 font-semibold flex items-center gap-2 transition-colors cursor-pointer shrink-0 ${
            activeTab === "metrics"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <BarChart3 className="h-4 w-4" /> Thống Kê AI
        </button>
        <button
          onClick={() => setActiveTab("tools")}
          className={`py-3 px-4 border-b-2 font-semibold flex items-center gap-2 transition-colors cursor-pointer shrink-0 ${
            activeTab === "tools"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <Wrench className="h-4 w-4" /> Nhật Ký Công Cụ ({tools.length})
        </button>
      </div>

      {/* TAB 1: USERS MANAGEMENT (CRUD) */}
      {activeTab === "users" && (
        <Card className="overflow-hidden border-border/80 shadow-sm">
          <div className="p-4 border-b bg-muted/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-bold text-sm">Danh sách tài khoản ({filteredUsers.length} / {usersList.length})</h3>
                <Badge variant="secondary" className="text-[10px]">Đầy đủ Thêm / Sửa / Xoá</Badge>
              </div>
              <p className="text-[11px] text-muted-foreground">Admin có thể tạo tài khoản, đổi quyền USER/ADMIN, khóa/mở và xoá tài khoản</p>
            </div>
            <div className="flex items-center gap-2">
              <div className="relative w-48 sm:w-64">
                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                <Input
                  placeholder="Tìm theo tên, email..."
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  className="pl-8 h-8 text-xs bg-background"
                />
              </div>
              <Button size="sm" onClick={() => setIsCreateUserOpen(true)} className="h-8 text-xs gap-1.5 cursor-pointer">
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
                {filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="p-8 text-center text-muted-foreground">
                      Không tìm thấy tài khoản nào phù hợp.
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map((u) => (
                    <tr key={u.id} className="hover:bg-muted/20 transition-colors">
                      <td className="p-3 font-mono font-medium text-muted-foreground">{u.id}</td>
                      <td className="p-3">
                        <div className="flex items-center gap-2.5">
                          <div className="h-8 w-8 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold shrink-0">
                            {u.name?.charAt(0) || "U"}
                          </div>
                          <div>
                            <div className="font-semibold text-foreground flex items-center gap-1.5">
                              {u.name}
                              {user?.id === u.id && (
                                <Badge className="text-[9px] px-1 py-0 bg-primary/20 text-primary">Bạn</Badge>
                              )}
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
                            <CheckCircle2 className="h-3.5 w-3.5" /> Hoạt động
                          </span>
                        ) : (
                          <span className="text-destructive font-medium flex items-center gap-1">
                            <XCircle className="h-3.5 w-3.5" /> Tạm khóa
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
                            onClick={() => handleOpenEditUser(u)}
                            className="h-7 px-2.5 text-xs gap-1 border-primary/30 text-primary hover:bg-primary/10 cursor-pointer"
                            title="Sửa thông tin hoặc đổi mật khẩu"
                          >
                            <Pencil className="h-3 w-3" /> Sửa
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleDeleteUser(u)}
                            disabled={user?.id === u.id}
                            className="h-7 px-2.5 text-xs gap-1 border-destructive/30 text-destructive hover:bg-destructive/10 cursor-pointer disabled:opacity-40"
                            title={user?.id === u.id ? "Không thể xoá chính bạn" : "Xoá tài khoản"}
                          >
                            <Trash2 className="h-3 w-3" /> Xoá
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* TAB 2: TRIPS MANAGEMENT (CRUD) */}
      {activeTab === "trips" && (
        <Card className="overflow-hidden border-border/80 shadow-sm">
          <div className="p-4 border-b bg-muted/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-bold text-sm">Danh sách kế hoạch du lịch ({filteredTrips.length} / {tripsList.length})</h3>
                <Badge variant="secondary" className="text-[10px]">Đầy đủ Xem / Sửa / Xoá</Badge>
              </div>
              <p className="text-[11px] text-muted-foreground">Admin có thể tạo mới, cập nhật tên, giai đoạn, ngân sách hoặc xoá chuyến đi vi phạm</p>
            </div>
            <div className="flex items-center gap-2">
              <div className="relative w-48 sm:w-64">
                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                <Input
                  placeholder="Tìm theo tên, địa điểm, User ID..."
                  value={tripSearch}
                  onChange={(e) => setTripSearch(e.target.value)}
                  className="pl-8 h-8 text-xs bg-background"
                />
              </div>
              <Button size="sm" onClick={() => setIsCreateTripOpen(true)} className="h-8 text-xs gap-1.5 cursor-pointer">
                <Plus className="h-3.5 w-3.5" /> Tạo chuyến mới
              </Button>
            </div>
          </div>

          <div className="overflow-x-auto text-xs">
            <table className="w-full text-left">
              <thead className="bg-muted/50 text-muted-foreground font-semibold">
                <tr>
                  <th className="p-3">ID</th>
                  <th className="p-3">Tên chuyến đi</th>
                  <th className="p-3">Điểm đến</th>
                  <th className="p-3">User ID</th>
                  <th className="p-3">Thời gian</th>
                  <th className="p-3">Giai đoạn</th>
                  <th className="p-3">Hoạt động</th>
                  <th className="p-3 text-right">Thao tác Admin</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {filteredTrips.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="p-8 text-center text-muted-foreground">
                      Không tìm thấy chuyến đi nào phù hợp.
                    </td>
                  </tr>
                ) : (
                  filteredTrips.map((t) => (
                    <tr key={t.id} className="hover:bg-muted/20 transition-colors">
                      <td className="p-3 font-mono font-medium text-muted-foreground">{t.id}</td>
                      <td className="p-3">
                        <div className="font-semibold text-foreground">{t.name || `Chuyến #${t.id}`}</div>
                        <div className="text-[10px] text-muted-foreground">{t.travelers || 1} khách &bull; {t.currency || "VND"}</div>
                      </td>
                      <td className="p-3">
                        <span className="font-medium">{t.destinationName || t.destination || "--"}</span>
                        {t.country && <span className="text-muted-foreground text-[10px] block">{t.country}</span>}
                      </td>
                      <td className="p-3 font-mono text-muted-foreground">User #{t.userId}</td>
                      <td className="p-3 text-muted-foreground">
                        {t.startDate} &rarr; {t.endDate}
                      </td>
                      <td className="p-3">
                        <Badge
                          variant="outline"
                          className={`text-[10px] ${
                            t.phase === "DURING"
                              ? "border-emerald-500/40 text-emerald-500 bg-emerald-500/10 font-bold"
                              : t.phase === "AFTER"
                                ? "border-muted-foreground/30 text-muted-foreground"
                                : "border-primary/40 text-primary bg-primary/10"
                          }`}
                        >
                          {t.phase === "DURING" ? "ĐANG ĐI" : t.phase === "AFTER" ? "ĐÃ KẾT THÚC" : "SẮP TỚI"}
                        </Badge>
                      </td>
                      <td className="p-3 font-semibold">{t.activityCount || 0} mục</td>
                      <td className="p-3 text-right">
                        <div className="inline-flex items-center gap-1">
                          <Link to="/trips/$tripId" params={{ tripId: String(t.id) }}>
                            <Button size="sm" variant="outline" className="h-7 px-2 text-xs gap-1 border-primary/30 text-primary hover:bg-primary/10 cursor-pointer" title="Xem chi tiết">
                              <Eye className="h-3 w-3" /> Xem
                            </Button>
                          </Link>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleOpenEditTrip(t)}
                            className="h-7 px-2 text-xs gap-1 border-amber-500/30 text-amber-600 dark:text-amber-400 hover:bg-amber-500/10 cursor-pointer"
                            title="Sửa tên hoặc giai đoạn chuyến đi"
                          >
                            <Pencil className="h-3 w-3" /> Sửa
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleDeleteTrip(t)}
                            className="h-7 px-2 text-xs gap-1 border-destructive/30 text-destructive hover:bg-destructive/10 cursor-pointer"
                            title="Xoá chuyến đi"
                          >
                            <Trash2 className="h-3 w-3" /> Xoá
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* TAB 3: AI USAGE METRICS */}
      {activeTab === "metrics" && metrics && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card className="p-4 space-y-1">
              <span className="text-xs text-muted-foreground">Tổng lượt gọi AI (Turns)</span>
              <div className="text-2xl font-extrabold text-foreground">{metrics.totalCalls || 0}</div>
              <div className="text-[11px] text-muted-foreground">Lượt đối thoại trợ lý ReAct</div>
            </Card>

            <Card className="p-4 space-y-1">
              <span className="text-xs text-muted-foreground">Tổng Tokens tiêu thụ</span>
              <div className="text-2xl font-extrabold text-indigo-500">
                {((metrics.totalPromptTokens || 0) + (metrics.totalCompletionTokens || 0)).toLocaleString()}
              </div>
              <div className="text-[11px] text-muted-foreground">
                In: {(metrics.totalPromptTokens || 0).toLocaleString()} &bull; Out: {(metrics.totalCompletionTokens || 0).toLocaleString()}
              </div>
            </Card>

            <Card className="p-4 space-y-1">
              <span className="text-xs text-muted-foreground">Tỷ lệ lỗi (Error Rate)</span>
              <div className={`text-2xl font-extrabold ${metrics.errorRate > 0.1 ? "text-destructive" : "text-emerald-500"}`}>
                {Math.round((metrics.errorRate || 0) * 100)}%
              </div>
              <div className="text-[11px] text-muted-foreground">Các lần gọi công cụ thất bại</div>
            </Card>

            <Card className="p-4 space-y-1">
              <span className="text-xs text-muted-foreground">Mô hình AI mặc định</span>
              <div className="text-lg font-bold text-foreground truncate">Gemini 3.8 Flash</div>
              <div className="text-[11px] text-muted-foreground">Google AI Studio API</div>
            </Card>
          </div>

          <Card className="p-5">
            <CardHeader className="p-0 pb-4">
              <CardTitle className="text-sm font-bold flex items-center gap-2">
                <Cpu className="h-4 w-4 text-primary" /> Top công cụ AI được gọi nhiều nhất
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              {metrics.topTools && metrics.topTools.length > 0 ? (
                <div className="space-y-2">
                  {metrics.topTools.map((t, idx) => (
                    <div key={idx} className="flex items-center justify-between p-2.5 rounded-lg bg-muted/40 border border-border/60 text-xs">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-muted-foreground w-5">#{idx + 1}</span>
                        <span className="font-mono font-medium text-foreground">{t.toolName}</span>
                      </div>
                      <Badge variant="secondary">{t.count} lượt</Badge>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-muted-foreground py-4 text-center">Chưa có dữ liệu thống kê công cụ.</p>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* TAB 4: TOOL EXECUTIONS AUDIT */}
      {activeTab === "tools" && (
        <Card className="overflow-hidden">
          <div className="p-4 border-b bg-muted/30 flex items-center justify-between">
            <h3 className="font-bold text-sm">Nhật ký thực thi công cụ ({tools.length} bản ghi gần nhất)</h3>
            <span className="text-xs text-muted-foreground">Ghi lại toàn bộ tham số &amp; thời gian chạy</span>
          </div>

          {tools.length === 0 ? (
            <div className="py-12 text-center text-xs text-muted-foreground">Chưa có nhật ký thực thi công cụ nào.</div>
          ) : (
            <div className="divide-y divide-border/60 overflow-x-auto text-xs">
              <table className="w-full text-left">
                <thead className="bg-muted/50 text-muted-foreground font-semibold">
                  <tr>
                    <th className="p-3">Mã #</th>
                    <th className="p-3">Công cụ</th>
                    <th className="p-3">Trạng thái</th>
                    <th className="p-3">Thời gian</th>
                    <th className="p-3">User ID</th>
                    <th className="p-3">Thời điểm</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/40">
                  {tools.map((item) => (
                    <tr key={item.id} className="hover:bg-muted/20">
                      <td className="p-3 font-mono">{item.id}</td>
                      <td className="p-3 font-semibold text-foreground flex items-center gap-1.5">
                        <Wrench className="h-3.5 w-3.5 text-primary" /> {item.toolName}
                      </td>
                      <td className="p-3">
                        {item.status === "OK" ? (
                          <Badge variant="outline" className="border-emerald-500/30 text-emerald-500 bg-emerald-500/10 text-[10px]">
                            OK
                          </Badge>
                        ) : (
                          <Badge variant="outline" className="border-destructive/30 text-destructive bg-destructive/10 text-[10px]">
                            {item.status}
                          </Badge>
                        )}
                      </td>
                      <td className="p-3 font-mono">{item.executionTimeMs} ms</td>
                      <td className="p-3 font-mono">User #{item.userId}</td>
                      <td className="p-3 text-muted-foreground">{new Date(item.createdAt).toLocaleString("vi-VN")}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}

      {/* MODAL: CREATE USER */}
      {isCreateUserOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 overflow-y-auto">
          <Card className="w-full max-w-md shadow-2xl border-border animate-in fade-in zoom-in-95">
            <CardHeader className="flex flex-row items-center justify-between border-b pb-4">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-primary/10 text-primary">
                  <UserIcon className="h-5 w-5" />
                </div>
                <div>
                  <CardTitle className="text-base font-bold">Thêm Tài Khoản Mới</CardTitle>
                  <p className="text-xs text-muted-foreground">Tạo tài khoản người dùng hoặc quản trị viên</p>
                </div>
              </div>
              <Button variant="ghost" size="icon" onClick={() => setIsCreateUserOpen(false)} className="rounded-full">
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>

            <form onSubmit={handleCreateUser} className="p-6 space-y-4 text-xs">
              <div className="space-y-1.5">
                <label className="text-xs font-medium">Họ và tên</label>
                <Input
                  required
                  placeholder="VD: Nguyễn Văn B"
                  value={createName}
                  onChange={(e) => setCreateName(e.target.value)}
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-medium">Địa chỉ Email</label>
                <Input
                  type="email"
                  required
                  placeholder="user@example.com"
                  value={createEmail}
                  onChange={(e) => setCreateEmail(e.target.value)}
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-medium">Mật khẩu ban đầu</label>
                <Input
                  type="password"
                  required
                  placeholder="Tối thiểu 6 ký tự"
                  value={createPassword}
                  onChange={(e) => setCreatePassword(e.target.value)}
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-medium">Vai trò (Role)</label>
                <select
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs outline-none"
                  value={createRole}
                  onChange={(e) => setCreateRole(e.target.value as any)}
                >
                  <option value="USER">Người dùng thường (USER)</option>
                  <option value="ADMIN">Quản trị viên (ADMIN)</option>
                </select>
              </div>

              <div className="pt-2 flex justify-end gap-2 border-t">
                <Button type="button" variant="outline" size="sm" onClick={() => setIsCreateUserOpen(false)}>
                  Hủy
                </Button>
                <Button type="submit" size="sm" disabled={isSubmitting}>
                  {isSubmitting ? "Đang tạo..." : "Xác nhận tạo tài khoản"}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* MODAL: EDIT USER */}
      {editingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 overflow-y-auto">
          <Card className="w-full max-w-md shadow-2xl border-border animate-in fade-in zoom-in-95">
            <CardHeader className="flex flex-row items-center justify-between border-b pb-4">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-primary/10 text-primary">
                  <Pencil className="h-5 w-5" />
                </div>
                <div>
                  <CardTitle className="text-base font-bold">Chỉnh Sửa Tài Khoản</CardTitle>
                  <p className="text-xs text-muted-foreground">{editingUser.email}</p>
                </div>
              </div>
              <Button variant="ghost" size="icon" onClick={() => setEditingUser(null)} className="rounded-full">
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>

            <form onSubmit={handleSaveEditUser} className="p-6 space-y-4 text-xs">
              <div className="space-y-1.5">
                <label className="text-xs font-medium">Họ và tên</label>
                <Input
                  required
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-medium">Vai trò (Role)</label>
                <select
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs outline-none"
                  value={editRole}
                  onChange={(e) => setEditRole(e.target.value as any)}
                >
                  <option value="USER">USER (Người dùng thường)</option>
                  <option value="ADMIN">ADMIN (Quản trị viên)</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-medium">Trạng thái hoạt động</label>
                <div className="flex items-center gap-4 pt-1">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="activeStatus"
                      checked={editActive}
                      onChange={() => setEditActive(true)}
                    />
                    <span>Kích hoạt</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="activeStatus"
                      checked={!editActive}
                      onChange={() => setEditActive(false)}
                    />
                    <span className="text-muted-foreground">Tạm khóa</span>
                  </label>
                </div>
              </div>

              <div className="space-y-1.5 pt-2 border-t">
                <label className="text-xs font-medium">Đặt lại mật khẩu mới (bỏ trống nếu giữ nguyên)</label>
                <Input
                  type="password"
                  placeholder="Để trống nếu không đổi mật khẩu"
                  value={editPassword}
                  onChange={(e) => setEditPassword(e.target.value)}
                />
              </div>

              <div className="pt-2 flex justify-end gap-2 border-t">
                <Button type="button" variant="outline" size="sm" onClick={() => setEditingUser(null)}>
                  Hủy
                </Button>
                <Button type="submit" size="sm" disabled={isSubmitting}>
                  {isSubmitting ? "Đang lưu..." : "Lưu thay đổi"}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* MODAL: EDIT TRIP */}
      {editingTrip && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 overflow-y-auto">
          <Card className="w-full max-w-md shadow-2xl border-border animate-in fade-in zoom-in-95">
            <CardHeader className="flex flex-row items-center justify-between border-b pb-4">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-amber-500/10 text-amber-500">
                  <Pencil className="h-5 w-5" />
                </div>
                <div>
                  <CardTitle className="text-base font-bold">Chỉnh Sửa Kế Hoạch #{editingTrip.id}</CardTitle>
                  <p className="text-xs text-muted-foreground">{editingTrip.destinationName || editingTrip.destination}</p>
                </div>
              </div>
              <Button variant="ghost" size="icon" onClick={() => setEditingTrip(null)} className="rounded-full">
                <X className="h-4 w-4" />
              </Button>
            </CardHeader>

            <form onSubmit={handleSaveEditTrip} className="p-6 space-y-4 text-xs">
              <div className="space-y-1.5">
                <label className="text-xs font-medium">Tên chuyến đi</label>
                <Input
                  required
                  placeholder="VD: Du lịch Đà Nẵng 4N3Đ"
                  value={editTripName}
                  onChange={(e) => setEditTripName(e.target.value)}
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-medium">Giai đoạn chuyến đi (Phase)</label>
                <select
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs outline-none"
                  value={editTripPhase}
                  onChange={(e) => setEditTripPhase(e.target.value)}
                >
                  <option value="BEFORE">SẮP TỚI (BEFORE)</option>
                  <option value="DURING">ĐANG DIỄN RA (DURING)</option>
                  <option value="AFTER">ĐÃ HOÀN THÀNH (AFTER)</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-medium">Ngân sách dự kiến (VND)</label>
                <Input
                  type="number"
                  placeholder="VD: 5000000"
                  value={editTripBudget}
                  onChange={(e) => setEditTripBudget(e.target.value)}
                />
              </div>

              <div className="pt-2 flex justify-end gap-2 border-t">
                <Button type="button" variant="outline" size="sm" onClick={() => setEditingTrip(null)}>
                  Hủy
                </Button>
                <Button type="submit" size="sm" disabled={isSubmitting}>
                  {isSubmitting ? "Đang lưu..." : "Lưu cập nhật"}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* MODAL: CREATE TRIP (USES CreateTripModal) */}
      <CreateTripModal
        isOpen={isCreateTripOpen}
        onClose={() => setIsCreateTripOpen(false)}
        onSuccess={() => {
          toast.success("Đã tạo kế hoạch mới thành công!")
          loadData()
        }}
      />
    </div>
  )
}
