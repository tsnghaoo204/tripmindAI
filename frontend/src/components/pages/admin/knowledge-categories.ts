import type { KnowledgeCategory } from "@/client/types"
import { AlertOctagon, Bed, Bus, Camera, Lightbulb, type LucideIcon, Utensils } from "lucide-react"

export const KNOWLEDGE_CATEGORIES: Record<KnowledgeCategory, { label: string; icon: LucideIcon; badgeClass: string }> =
  {
    FOOD: {
      label: "Ăn uống",
      icon: Utensils,
      badgeClass: "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20",
    },
    ACCOMMODATION: {
      label: "Lưu trú",
      icon: Bed,
      badgeClass: "bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20",
    },
    TRANSPORT: {
      label: "Đi lại",
      icon: Bus,
      badgeClass: "bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20",
    },
    SIGHTSEEING: {
      label: "Tham quan",
      icon: Camera,
      badgeClass: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20",
    },
    TIP: {
      label: "Mẹo hay",
      icon: Lightbulb,
      badgeClass: "bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 border-yellow-500/20",
    },
    WARNING: {
      label: "Cảnh báo / Tránh bẫy",
      icon: AlertOctagon,
      badgeClass: "bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20",
    },
  }

export const KNOWLEDGE_CATEGORY_IDS = Object.keys(KNOWLEDGE_CATEGORIES) as KnowledgeCategory[]
