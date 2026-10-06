import { request } from "../client"
import type { BudgetSummary, Expense } from "../types"

export interface CreateExpenseInput {
  amount: number
  currency: string
  category: string
  description: string
  date: string
}

export const budgetApi = {
  getBudgetSummary: async (tripId: number): Promise<BudgetSummary> => {
    return request<BudgetSummary>(`/api/trips/${tripId}/budget`)
  },

  getExpenses: async (tripId: number): Promise<Expense[]> => {
    return request<Expense[]>(`/api/trips/${tripId}/expenses`)
  },

  createExpense: async (tripId: number, data: CreateExpenseInput): Promise<Expense> => {
    return request<Expense>(`/api/trips/${tripId}/expenses`, {
      method: "POST",
      data,
    })
  },

  updateExpense: async (expenseId: number, data: Partial<CreateExpenseInput>): Promise<Expense> => {
    return request<Expense>(`/api/expenses/${expenseId}`, {
      method: "PUT",
      data,
    })
  },

  deleteExpense: async (expenseId: number): Promise<void> => {
    return request<void>(`/api/expenses/${expenseId}`, {
      method: "DELETE",
    })
  },
}
