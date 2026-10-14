import { adminApi } from "@/client/api/admin-api"
import { Badge } from "@/components/ui/badge"
import { Card } from "@/components/ui/card"
import { queryKeys } from "@/hooks/query-keys"
import { useQuery } from "@tanstack/react-query"
import { Wrench } from "lucide-react"
import { AdminQueryError } from "./AdminQueryError"

export const adminToolsQuery = {
  queryKey: queryKeys.admin.tools,
  queryFn: () => adminApi.getToolExecutions({ page: 0, size: 40 }),
}

export function ToolsTab() {
  const { data, isPending, isError, error } = useQuery(adminToolsQuery)
  if (isError) return <AdminQueryError error={error} />
  const tools = data?.content ?? []

  return (
    <Card className="overflow-hidden">
      <div className="p-4 border-b bg-muted/30 flex items-center justify-between">
        <h3 className="font-bold text-sm">Nhật ký thực thi công cụ ({tools.length} bản ghi gần nhất)</h3>
        <span className="text-xs text-muted-foreground">Ghi lại toàn bộ tham số &amp; thời gian chạy</span>
      </div>

      {isPending ? (
        <div className="py-12 text-center text-xs text-muted-foreground">Đang tải nhật ký...</div>
      ) : tools.length === 0 ? (
        <div className="py-12 text-center text-xs text-muted-foreground">Chưa có nhật ký thực thi công cụ nào.</div>
      ) : (
        <div className="overflow-x-auto text-xs">
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
                  <td className="p-3 font-semibold text-foreground">
                    <span className="flex items-center gap-1.5">
                      <Wrench className="h-3.5 w-3.5 text-primary" aria-hidden /> {item.toolName}
                    </span>
                  </td>
                  <td className="p-3">
                    <Badge
                      variant="outline"
                      title={item.errorMessage ?? undefined}
                      className={`text-[10px] ${
                        item.status === "OK"
                          ? "border-emerald-500/30 text-emerald-500 bg-emerald-500/10"
                          : "border-destructive/30 text-destructive bg-destructive/10"
                      }`}
                    >
                      {item.status}
                    </Badge>
                  </td>
                  <td className="p-3 font-mono">{item.executionTimeMs ?? "--"} ms</td>
                  <td className="p-3 font-mono">{item.userId != null ? `User #${item.userId}` : "--"}</td>
                  <td className="p-3 text-muted-foreground">{new Date(item.createdAt).toLocaleString("vi-VN")}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  )
}
