import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(amount?: number | null, currency = "VND"): string {
  if (amount == null) return "Chưa rõ"
  if (currency === "VND") {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
      maximumFractionDigits: 0,
    }).format(amount)
  }
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: currency,
  }).format(amount)
}

export function formatDateRange(startDate: string, endDate: string): string {
  try {
    const s = new Date(startDate)
    const e = new Date(endDate)
    return `${s.toLocaleDateString("vi-VN")} - ${e.toLocaleDateString("vi-VN")}`
  } catch {
    return `${startDate} - ${endDate}`
  }
}
