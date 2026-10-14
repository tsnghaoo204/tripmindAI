import { budgetApi } from "@/client/api/budget-api"
import { getErrorMessage } from "@/client/errors"
import type { DailyAllowance } from "@/client/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { queryKeys } from "@/hooks/query-keys"
import { formatCurrency } from "@/lib/utils"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { AlertTriangle, Calendar, DollarSign, Sparkles, Trash2 } from "lucide-react"
import { toast } from "sonner"
import { ExpenseForm } from "./ExpenseForm"
import { categoryLabel } from "./budget-categories"

interface BudgetViewProps {
  tripId: number
}

function DailyAllowanceCard({ daily, currency }: { daily: DailyAllowance; currency: string }) {
  const isToday = daily.mode === "TODAY"
  const amount = isToday ? (daily.todayLeft ?? daily.allowance) : daily.allowance
  return (
    <Card className="bg-gradient-to-r from-blue-600/10 via-indigo-600/10 to-violet-600/10 border-blue-500/20 p-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Sparkles className="h-4 w-4 text-primary" aria-hidden />
            <h4 className="font-bold text-sm text-foreground">
              {isToday ? "Hôm nay còn tiêu được bao nhiêu?" : "Hạn mức chi tiêu mỗi ngày"}
            </h4>
          </div>
          <p className="text-xs text-muted-foreground">
            Được tính toán theo múi giờ điểm đến &bull; Còn {daily.remainingDays} ngày của chuyến đi
            {daily.status === "OVER" && daily.overBy ? ` • Vượt ${formatCurrency(daily.overBy, currency)}` : ""}
          </p>
        </div>
        <div className="text-right">
          <span className={`text-2xl font-extrabold ${daily.status === "OVER" ? "text-destructive" : "text-primary"}`}>
            {formatCurrency(amount, currency)}
          </span>
          {isToday && (
            <div className="text-xs text-muted-foreground">
              Hạn mức hôm nay: {formatCurrency(daily.allowance, currency)} &bull; Đã chi:{" "}
              {formatCurrency(daily.spentToday ?? 0, currency)}
            </div>
          )}
        </div>
      </div>
    </Card>
  )
}

export function BudgetView({ tripId }: BudgetViewProps) {
  const queryClient = useQueryClient()
  const summaryQuery = useQuery({
    queryKey: queryKeys.budget(tripId),
    queryFn: () => budgetApi.getBudgetSummary(tripId),
  })
  const expensesQuery = useQuery({
    queryKey: queryKeys.expenses(tripId),
    queryFn: () => budgetApi.getExpenses(tripId),
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.budget(tripId) })
    queryClient.invalidateQueries({ queryKey: queryKeys.expenses(tripId) })
  }

  const deleteMutation = useMutation({
    mutationFn: (expenseId: number) => budgetApi.deleteExpense(expenseId),
    onSuccess: () => {
      toast.success("Đã xoá khoản chi")
      refresh()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi xoá khoản chi")),
  })

  if (summaryQuery.isPending) {
    return <div className="py-12 text-center text-xs text-muted-foreground">Đang tải ngân sách...</div>
  }
  if (summaryQuery.isError) {
    return (
      <div className="py-12 text-center text-xs text-muted-foreground">
        {getErrorMessage(summaryQuery.error, "Không thể tải thông tin ngân sách")}
      </div>
    )
  }

  const summary = summaryQuery.data
  const expenses = expensesQuery.data ?? []
  const currency = summary.currency

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Tổng ngân sách</span>
          <div className="text-2xl font-bold text-foreground">{formatCurrency(summary.budget, currency)}</div>
          <div className="text-[11px] text-muted-foreground">Mục tiêu ban đầu</div>
        </Card>

        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Ước tính lịch trình</span>
          <div className="text-2xl font-bold text-foreground">{formatCurrency(summary.estimatedTotal, currency)}</div>
          <div className="text-[11px] text-muted-foreground">Từ các hoạt động đã lên lịch</div>
        </Card>

        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Thực tế đã chi</span>
          <div className="text-2xl font-bold text-rose-500">{formatCurrency(summary.actualTotal, currency)}</div>
          <div className="text-[11px] text-muted-foreground">{expenses.length} khoản chi đã ghi</div>
        </Card>

        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Còn lại</span>
          <div
            className={`text-2xl font-bold ${summary.remaining != null && summary.remaining < 0 ? "text-destructive" : "text-emerald-500"}`}
          >
            {formatCurrency(summary.remaining, currency)}
          </div>
          <div className="text-[11px] text-muted-foreground">
            {summary.warningLevel === "OVER" && (
              <span className="text-destructive font-semibold flex items-center gap-1">
                <AlertTriangle className="h-3 w-3" aria-hidden /> Đã vượt ngân sách!
              </span>
            )}
            {summary.warningLevel === "NEAR_LIMIT" && (
              <span className="text-amber-500 font-semibold flex items-center gap-1">
                <AlertTriangle className="h-3 w-3" aria-hidden /> Sắp chạm ngân sách
              </span>
            )}
            {summary.warningLevel === "NONE" && "Trong giới hạn an toàn"}
          </div>
        </Card>
      </div>

      {summary.daily ? (
        <DailyAllowanceCard daily={summary.daily} currency={currency} />
      ) : (
        summary.dailyReason && <p className="text-xs text-muted-foreground">{summary.dailyReason}</p>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <ExpenseForm tripId={tripId} currency={currency} onCreated={refresh} />

        <Card className="lg:col-span-2 p-5 flex flex-col">
          <div className="flex items-center justify-between border-b pb-3 mb-4">
            <h4 className="font-bold text-sm">Lịch sử chi tiêu ({expenses.length})</h4>
            <span className="text-xs text-muted-foreground">Sắp xếp theo ngày mới nhất</span>
          </div>

          {expenses.length === 0 ? (
            <div className="py-12 text-center text-xs text-muted-foreground flex-1 flex flex-col items-center justify-center">
              <DollarSign className="h-8 w-8 text-muted-foreground/40 mb-2" aria-hidden />
              {expensesQuery.isPending ? "Đang tải..." : "Chưa có khoản chi tiêu thực tế nào được ghi nhận."}
            </div>
          ) : (
            <ul className="divide-y divide-border/60 overflow-y-auto max-h-[420px]">
              {expenses.map((exp) => (
                <li key={exp.id} className="py-3 flex items-center justify-between gap-3 text-xs">
                  <div className="space-y-0.5">
                    <div className="font-semibold text-foreground flex items-center gap-2">
                      <span>{exp.description || exp.activityTitle || "Chi tiêu khác"}</span>
                      <Badge variant="outline" className="text-[10px]">
                        {categoryLabel(exp.category)}
                      </Badge>
                    </div>
                    <div className="text-[11px] text-muted-foreground flex items-center gap-1.5">
                      <Calendar className="h-3 w-3" aria-hidden />
                      <span>{exp.expenseDate}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="font-bold text-sm text-foreground">
                      {formatCurrency(exp.amount, exp.currency)}
                    </span>
                    <Button
                      size="icon"
                      variant="ghost"
                      disabled={deleteMutation.isPending}
                      onClick={() => deleteMutation.mutate(exp.id)}
                      className="h-7 w-7 text-muted-foreground hover:text-destructive"
                      aria-label={`Xoá khoản chi ${exp.description || categoryLabel(exp.category)}`}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  )
}
