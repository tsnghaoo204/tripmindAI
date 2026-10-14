import { request } from "../client"
import type { BudgetSummary, Expense, ExpenseCategory } from "../types"

export interface ExpenseInput {
  amount: number
  currency?: string
  category: ExpenseCategory
  description?: string
  expenseDate: string
  activityId?: number
}

export const budgetApi = {
  getBudgetSummary: (tripId: number) => request<BudgetSummary>(`/api/trips/${tripId}/budget`),

  getExpenses: (tripId: number) => request<Expense[]>(`/api/trips/${tripId}/expenses`),

  createExpense: (tripId: number, data: ExpenseInput) =>
    request<Expense>(`/api/trips/${tripId}/expenses`, { method: "POST", data }),

  updateExpense: (expenseId: number, data: ExpenseInput) =>
    request<Expense>(`/api/expenses/${expenseId}`, { method: "PUT", data }),

  deleteExpense: (expenseId: number) => request<void>(`/api/expenses/${expenseId}`, { method: "DELETE" }),
}
