import { KnowledgeManagementTab } from "@/components/pages/admin/KnowledgeManagementTab"
import { MetricsTab } from "@/components/pages/admin/MetricsTab"
import { ToolsTab, adminToolsQuery } from "@/components/pages/admin/ToolsTab"
import { TripsTab, adminTripsQuery } from "@/components/pages/admin/TripsTab"
import { UsersTab, adminUsersQuery } from "@/components/pages/admin/UsersTab"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { queryKeys } from "@/hooks/query-keys"
import { getStoredUser } from "@/lib/auth-storage"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, redirect } from "@tanstack/react-router"
import { BarChart3, BookOpen, Compass, RotateCw, ShieldCheck, Users, Wrench } from "lucide-react"
import { useState } from "react"

export const Route = createFileRoute("/_layout/admin")({
  beforeLoad: () => {
    if (getStoredUser()?.role !== "ADMIN") throw redirect({ to: "/" })
  },
  component: AdminPage,
})

function AdminPage() {
  const queryClient = useQueryClient()
  const [activeTab, setActiveTab] = useState("users")

  // Counts for tab labels; shares cache with the tabs themselves
  const users = useQuery(adminUsersQuery)
  const trips = useQuery(adminTripsQuery)
  const tools = useQuery(adminToolsQuery)

  return (
    <div className="container mx-auto px-4 sm:px-6 py-8 space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-2xl bg-gradient-to-tr from-amber-500/20 to-primary/20 text-amber-500 shadow-sm border border-amber-500/30">
            <ShieldCheck className="h-7 w-7" aria-hidden />
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

        <Button
          size="sm"
          variant="outline"
          onClick={() => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all })}
          className="gap-1.5 cursor-pointer self-start sm:self-auto"
        >
          <RotateCw className="h-3.5 w-3.5" /> Làm mới
        </Button>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <TabsList aria-label="Khu vực quản trị">
          <TabsTrigger value="users">
            <Users className="h-4 w-4" aria-hidden /> Quản Lý Tài Khoản ({users.data?.totalElements ?? "…"})
          </TabsTrigger>
          <TabsTrigger value="trips">
            <Compass className="h-4 w-4" aria-hidden /> Quản Lý Kế Hoạch ({trips.data?.totalElements ?? "…"})
          </TabsTrigger>
          <TabsTrigger value="knowledge">
            <BookOpen className="h-4 w-4" aria-hidden /> Dataset &amp; Tri Thức AI
          </TabsTrigger>
          <TabsTrigger value="metrics">
            <BarChart3 className="h-4 w-4" aria-hidden /> Thống Kê AI
          </TabsTrigger>
          <TabsTrigger value="tools">
            <Wrench className="h-4 w-4" aria-hidden /> Nhật Ký Công Cụ ({tools.data?.content.length ?? "…"})
          </TabsTrigger>
        </TabsList>

        <TabsContent value="users">
          <UsersTab />
        </TabsContent>
        <TabsContent value="trips">
          <TripsTab />
        </TabsContent>
        <TabsContent value="knowledge">
          <KnowledgeManagementTab />
        </TabsContent>
        <TabsContent value="metrics">
          <MetricsTab />
        </TabsContent>
        <TabsContent value="tools">
          <ToolsTab />
        </TabsContent>
      </Tabs>
    </div>
  )
}
