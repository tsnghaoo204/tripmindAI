import React, { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import {
  DollarSign,
  TrendingDown,
  TrendingUp,
  AlertTriangle,
  Plus,
  Trash2,
  Calendar,
  PieChart,
  Coffee,
  Hotel,
  Car,
  ShoppingBag,
  Sparkles,
} from "lucide-react"
import { budgetApi } from "@/client/api/budget-api"
import type { BudgetSummary, Expense } from "@/client/types"
import { formatCurrency } from "@/lib/utils"
import { toast } from "sonner"

interface BudgetViewProps {
  tripId: number
}

const CATEGORIES = [
  { id: "FOOD", label: "Ăn uống", icon: Coffee },
  { id: "ACCOMMODATION", label: "Khách sạn", icon: Hotel },
  { id: "TRANSPORT", label: "Di chuyển", icon: Car },
  { id: "ACTIVITIES", label: "Vui chơi", icon: Sparkles },
  { id: "SHOPPING", label: "Mua sắm", icon: ShoppingBag },
  { id: "OTHER", label: "Khác", icon: DollarSign },
]

export function BudgetView({ tripId }: BudgetViewProps) {
  const [summary, setSummary] = useState<BudgetSummary | null>(null)
  const [expenses, setExpenses] = useState<Expense[]>([])
  const [loading, setLoading] = useState(true)

  // Form thêm chi tiêu
  const [amount, setAmount] = useState("")
  const [category, setCategory] = useState("FOOD")
  const [description, setDescription] = useState("")
  const [date, setDate] = useState(new Date().toISOString().split("T")[0])
  const [isSubmitting, setIsSubmitting] = useState(false)

  const fetchData = async () => {
    try {
      setLoading(true)
      const [sum, exp] = await Promise.all([
        budgetApi.getBudgetSummary(tripId),
        budgetApi.getExpenses(tripId),
      ])
      setSummary(sum)
      setExpenses(exp)
    } catch {
      toast.error("Không thể tải thông tin ngân sách")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [tripId])

  const handleAddExpense = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!amount) return
    setIsSubmitting(true)
    try {
      await budgetApi.createExpense(tripId, {
        amount: Number(amount),
        currency: summary?.currency || "VND",
        category,
        description,
        date,
      })
      toast.success("Đã ghi nhận khoản chi!")
      setAmount("")
      setDescription("")
      fetchData()
    } catch (err: any) {
      toast.error(err.message || "Lỗi khi thêm chi tiêu")
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDeleteExpense = async (id: number) => {
    try {
      await budgetApi.deleteExpense(id)
      toast.success("Đã xoá khoản chi")
      fetchData()
    } catch {
      toast.error("Lỗi khi xoá khoản chi")
    }
  }

  if (loading && !summary) {
    return <div className="py-12 text-center text-xs text-muted-foreground">Đang tải ngân sách...</div>
  }

  const usedPercent = summary?.usedRatio ? Math.min(Math.round(summary.usedRatio * 100), 100) : 0

  return (
    <div className="space-y-6">
      {/* Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Tổng ngân sách</span>
          <div className="text-2xl font-bold text-foreground">
            {formatCurrency(summary?.budget, summary?.currency)}
          </div>
          <div className="text-[11px] text-muted-foreground">Mục tiêu ban đầu</div>
        </Card>

        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Ước tính lịch trình</span>
          <div className="text-2xl font-bold text-foreground">
            {formatCurrency(summary?.estimatedTotal, summary?.currency)}
          </div>
          <div className="text-[11px] text-muted-foreground">Từ các hoạt động đã lên lịch</div>
        </Card>

        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Thực tế đã chi</span>
          <div className="text-2xl font-bold text-rose-500">
            {formatCurrency(summary?.actualTotal, summary?.currency)}
          </div>
          <div className="text-[11px] text-muted-foreground">{expenses.length} khoản chi đã ghi</div>
        </Card>

        <Card className="p-4 space-y-1">
          <span className="text-xs text-muted-foreground">Còn lại</span>
          <div className={`text-2xl font-bold ${summary && summary.remaining != null && summary.remaining < 0 ? "text-destructive" : "text-emerald-500"}`}>
            {formatCurrency(summary?.remaining, summary?.currency)}
          </div>
          <div className="text-[11px] text-muted-foreground">
            {summary?.warningLevel === "OVER" && (
              <span className="text-destructive font-semibold flex items-center gap-1">
                <AlertTriangle className="h-3 w-3" /> Đã vượt ngân sách!
              </span>
            )}
            {summary?.warningLevel === "NEAR_LIMIT" && (
              <span className="text-amber-500 font-semibold flex items-center gap-1">
                <AlertTriangle className="h-3 w-3" /> Đã tiêu hơn 90%
              </span>
            )}
            {summary?.warningLevel === "NONE" && "Trong giới hạn an toàn"}
          </div>
        </Card>
      </div>

      {/* Daily Allowance Widget */}
      {summary?.dailyAllowance && (
        <Card className="bg-gradient-to-r from-blue-600/10 via-indigo-600/10 to-violet-600/10 border-blue-500/20 p-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <Sparkles className="h-4 w-4 text-primary" />
                <h4 className="font-bold text-sm text-foreground">Hôm nay còn tiêu được bao nhiêu?</h4>
              </div>
              <p className="text-xs text-muted-foreground">
                Được tính toán theo múi giờ điểm đến &bull; Còn {summary.dailyAllowance.daysLeft} ngày của chuyến đi
              </p>
            </div>
            <div className="text-right">
              <span className="text-2xl font-extrabold text-primary">
                {formatCurrency(summary.dailyAllowance.remainingToday, summary.currency)}
              </span>
              <div className="text-xs text-muted-foreground">
                Hạn mức hôm nay: {formatCurrency(summary.dailyAllowance.allowancePerDay, summary.currency)}
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Main Budget Grid: Add Expense & Expense List */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Form Add Expense */}
        <Card className="p-5 space-y-4">
          <div className="flex items-center gap-2 border-b pb-3">
            <Plus className="h-4 w-4 text-primary" />
            <h4 className="font-bold text-sm">Ghi nhận chi tiêu thực tế</h4>
          </div>

          <form onSubmit={handleAddExpense} className="space-y-3 text-xs">
            <div className="space-y-1">
              <label className="font-medium text-foreground">Số tiền (VND)</label>
              <Input
                type="number"
                required
                placeholder="VD: 120000"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>

            <div className="space-y-1">
              <label className="font-medium text-foreground">Hạng mục</label>
              <select
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs focus:ring-2 focus:ring-ring outline-none"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                {CATEGORIES.map((c) => (
                  <option key={c.id} value={c.id}>{c.label}</option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <label className="font-medium text-foreground">Mô tả khoản chi</label>
              <Input
                placeholder="VD: Ăn tối hải sản Bé Mặn"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            <div className="space-y-1">
              <label className="font-medium text-foreground">Ngày chi</label>
              <Input
                type="date"
                required
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            </div>

            <Button type="submit" className="w-full mt-2" size="sm" disabled={isSubmitting}>
              {isSubmitting ? "Đang ghi..." : "Lưu khoản chi"}
            </Button>
          </form>
        </Card>

        {/* Expenses List */}
        <Card className="lg:col-span-2 p-5 flex flex-col">
          <div className="flex items-center justify-between border-b pb-3 mb-4">
            <h4 className="font-bold text-sm">Lịch sử chi tiêu ({expenses.length})</h4>
            <span className="text-xs text-muted-foreground">Sắp xếp theo ngày mới nhất</span>
          </div>

          {expenses.length === 0 ? (
            <div className="py-12 text-center text-xs text-muted-foreground flex-1 flex flex-col items-center justify-center">
              <DollarSign className="h-8 w-8 text-muted-foreground/40 mb-2" />
              Chưa có khoản chi tiêu thực tế nào được ghi nhận.
            </div>
          ) : (
            <div className="divide-y divide-border/60 overflow-y-auto max-h-[420px]">
              {expenses.map((exp) => (
                <div key={exp.id} className="py-3 flex items-center justify-between gap-3 text-xs">
                  <div className="space-y-0.5">
                    <div className="font-semibold text-foreground flex items-center gap-2">
                      <span>{exp.description || "Chi tiêu khác"}</span>
                      <Badge variant="outline" className="text-[10px]">
                        {CATEGORIES.find((c) => c.id === exp.category)?.label || exp.category}
                      </Badge>
                    </div>
                    <div className="text-[11px] text-muted-foreground flex items-center gap-1.5">
                      <Calendar className="h-3 w-3" />
                      <span>{exp.date}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="font-bold text-sm text-foreground">
                      {formatCurrency(exp.amount, exp.currency)}
                    </span>
                    <Button
                      size="icon"
                      variant="ghost"
                      onClick={() => handleDeleteExpense(exp.id)}
                      className="h-7 w-7 text-muted-foreground hover:text-destructive"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}
