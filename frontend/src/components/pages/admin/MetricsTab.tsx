import { adminApi } from "@/client/api/admin-api"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { queryKeys } from "@/hooks/query-keys"
import { useQuery } from "@tanstack/react-query"
import { Cpu } from "lucide-react"
import { AdminQueryError } from "./AdminQueryError"

const num = (v: number | null | undefined) => Number(v ?? 0).toLocaleString("vi-VN")

export function MetricsTab() {
  const {
    data: metrics,
    isPending,
    isError,
    error,
  } = useQuery({
    queryKey: queryKeys.admin.usage,
    queryFn: adminApi.getAiUsage,
  })

  if (isError) return <AdminQueryError error={error} />
  if (isPending) return <div className="py-12 text-center text-xs text-muted-foreground">Đang tải thống kê AI...</div>

  const errorRate = metrics.toolErrorRate || 0

  return (
    <div className="space-y-6">
      <p className="text-xs text-muted-foreground">
        Khoảng thời gian: {metrics.from} → {metrics.to}
      </p>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Tổng lượt gọi AI (Turns)</span>
          <div className="text-2xl font-extrabold text-foreground">{num(metrics.assistantTurns)}</div>
          <div className="text-[11px] text-muted-foreground">Lượt đối thoại trợ lý ReAct</div>
        </Card>

        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Tổng Tokens tiêu thụ</span>
          <div className="text-2xl font-extrabold text-indigo-500">{num(metrics.totalTokens)}</div>
          <div className="text-[11px] text-muted-foreground">
            In: {num(metrics.promptTokens)} &bull; Out: {num(metrics.completionTokens)}
          </div>
        </Card>

        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Tỷ lệ lỗi công cụ</span>
          <div className={`text-2xl font-extrabold ${errorRate > 0.1 ? "text-destructive" : "text-emerald-500"}`}>
            {Math.round(errorRate * 100)}%
          </div>
          <div className="text-[11px] text-muted-foreground">Trên {num(metrics.toolCalls)} lần gọi công cụ</div>
        </Card>

        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Thời gian chạy công cụ TB</span>
          <div className="text-2xl font-extrabold text-foreground">{num(metrics.avgToolMs)} ms</div>
          <div className="text-[11px] text-muted-foreground">Trung bình mỗi lần gọi</div>
        </Card>
      </div>

      <Card className="p-5">
        <CardHeader className="p-0 pb-4">
          <CardTitle className="text-sm font-bold flex items-center gap-2">
            <Cpu className="h-4 w-4 text-primary" aria-hidden /> Top công cụ AI được gọi nhiều nhất
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {metrics.topTools?.length ? (
            <ol className="space-y-2">
              {metrics.topTools.map((t, idx) => (
                <li
                  key={t.tool}
                  className="flex items-center justify-between p-2.5 rounded-lg bg-muted/40 border border-border/60 text-xs"
                >
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-muted-foreground w-5">#{idx + 1}</span>
                    <span className="font-mono font-medium text-foreground">{t.tool}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    {t.failures > 0 && (
                      <Badge variant="outline" className="text-destructive border-destructive/30">
                        {t.failures} lỗi
                      </Badge>
                    )}
                    <Badge variant="secondary">{t.calls} lượt</Badge>
                  </div>
                </li>
              ))}
            </ol>
          ) : (
            <p className="text-xs text-muted-foreground py-4 text-center">Chưa có dữ liệu thống kê công cụ.</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
