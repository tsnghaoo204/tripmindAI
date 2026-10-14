import type { ExpenseCategory } from "@/client/types"
import { Car, Coffee, DollarSign, Hotel, type LucideIcon, ShoppingBag, Sparkles } from "lucide-react"

export const EXPENSE_CATEGORIES: Array<{ id: ExpenseCategory; label: string; icon: LucideIcon }> = [
  { id: "FOOD", label: "Ăn uống", icon: Coffee },
  { id: "ACCOMMODATION", label: "Khách sạn", icon: Hotel },
  { id: "TRANSPORTATION", label: "Di chuyển", icon: Car },
  { id: "ACTIVITIES", label: "Vui chơi", icon: Sparkles },
  { id: "SHOPPING", label: "Mua sắm", icon: ShoppingBag },
  { id: "OTHER", label: "Khác", icon: DollarSign },
]

export function categoryLabel(id: ExpenseCategory): string {
  return EXPENSE_CATEGORIES.find((c) => c.id === id)?.label ?? id
}
