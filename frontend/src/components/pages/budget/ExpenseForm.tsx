import { budgetApi } from "@/client/api/budget-api"
import { getErrorMessage } from "@/client/errors"
import type { ExpenseCategory } from "@/client/types"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useMutation } from "@tanstack/react-query"
import { Plus } from "lucide-react"
import { useState } from "react"
import { toast } from "sonner"
import { EXPENSE_CATEGORIES } from "./budget-categories"

interface ExpenseFormProps {
  tripId: number
  currency: string
  onCreated: () => void
}

function today() {
  return new Date().toISOString().split("T")[0]
}

export function ExpenseForm({ tripId, currency, onCreated }: ExpenseFormProps) {
  const [amount, setAmount] = useState("")
  const [category, setCategory] = useState<ExpenseCategory>("FOOD")
  const [description, setDescription] = useState("")
  const [expenseDate, setExpenseDate] = useState(today)

  const createMutation = useMutation({
    mutationFn: () =>
      budgetApi.createExpense(tripId, {
        amount: Number(amount),
        currency,
        category,
        description: description.trim() || undefined,
        expenseDate,
      }),
    onSuccess: () => {
      toast.success("Đã ghi nhận khoản chi!")
      setAmount("")
      setDescription("")
      onCreated()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi thêm chi tiêu")),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (amount) createMutation.mutate()
  }

  return (
    <Card className="p-5 space-y-4">
      <div className="flex items-center gap-2 border-b pb-3">
        <Plus className="h-4 w-4 text-primary" aria-hidden />
        <h4 className="font-bold text-sm">Ghi nhận chi tiêu thực tế</h4>
      </div>

      <form onSubmit={handleSubmit} className="space-y-3 text-xs">
        <div className="space-y-1">
          <label htmlFor="expense-amount" className="font-medium text-foreground">
            Số tiền ({currency})
          </label>
          <Input
            id="expense-amount"
            type="number"
            min={0}
            required
            placeholder="VD: 120000"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
        </div>

        <div className="space-y-1">
          <label htmlFor="expense-category" className="font-medium text-foreground">
            Hạng mục
          </label>
          <select
            id="expense-category"
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-xs focus:ring-2 focus:ring-ring outline-none"
            value={category}
            onChange={(e) => setCategory(e.target.value as ExpenseCategory)}
          >
            {EXPENSE_CATEGORIES.map((c) => (
              <option key={c.id} value={c.id}>
                {c.label}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1">
          <label htmlFor="expense-description" className="font-medium text-foreground">
            Mô tả khoản chi
          </label>
          <Input
            id="expense-description"
            maxLength={255}
            placeholder="VD: Ăn tối hải sản Bé Mặn"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        <div className="space-y-1">
          <label htmlFor="expense-date" className="font-medium text-foreground">
            Ngày chi
          </label>
          <Input
            id="expense-date"
            type="date"
            required
            value={expenseDate}
            onChange={(e) => setExpenseDate(e.target.value)}
          />
        </div>

        <Button type="submit" className="w-full mt-2" size="sm" disabled={createMutation.isPending}>
          {createMutation.isPending ? "Đang ghi..." : "Lưu khoản chi"}
        </Button>
      </form>
    </Card>
  )
}
