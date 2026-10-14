import type { Place } from "@/client/types"

export interface PlaceTag {
  label: string
  icon: string
  color: string
}

const NEUTRAL = "bg-slate-500/10 text-slate-700 dark:text-slate-400 border-slate-500/25"
const CAFE_TYPES = ["cafe", "coffee_shop", "tea_house", "bar", "pub", "wine_bar"]
const LODGING_TYPES = ["lodging", "hotel", "resort_hotel", "guest_house", "hostel"]
const SIGHT_TYPES = ["tourist_attraction", "historical_landmark", "museum", "park", "natural_feature"]

/** Derives a display badge from a place's Google category/types. */
export function getPlaceTag(place?: Place | null): PlaceTag | null {
  if (!place) return null
  const rawType = (place.category || "").toLowerCase()
  const types = (place.types || []).map((t) => t.toLowerCase())
  const displayName = place.primaryTypeDisplayName || undefined

  if (
    place.provider === "COMMUNITY" ||
    rawType === "community" ||
    rawType.includes("cộng đồng") ||
    types.includes("community")
  ) {
    return {
      label: "Gợi ý cộng đồng",
      icon: "✨",
      color: "bg-purple-500/10 text-purple-700 dark:text-purple-400 border-purple-500/25",
    }
  }
  if (CAFE_TYPES.includes(rawType) || types.some((t) => CAFE_TYPES.includes(t))) {
    return {
      label: displayName || "Quán cà phê / Đồ uống",
      icon: "☕",
      color: "bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/25",
    }
  }
  if (
    rawType.includes("restaurant") ||
    types.some((t) => t.includes("restaurant")) ||
    types.some((t) => ["food_court", "diner", "meal_takeaway"].includes(t))
  ) {
    return {
      label: displayName || "Nhà hàng / Quán ăn",
      icon: "🍽️",
      color: "bg-orange-500/10 text-orange-700 dark:text-orange-400 border-orange-500/25",
    }
  }
  if (["hotel", "lodging", "resort"].some((k) => rawType.includes(k)) || types.some((t) => LODGING_TYPES.includes(t))) {
    return {
      label: displayName || "Khách sạn / Lưu trú",
      icon: "🏨",
      color: "bg-blue-500/10 text-blue-700 dark:text-blue-400 border-blue-500/25",
    }
  }
  if (["attraction", "museum", "park"].some((k) => rawType.includes(k)) || types.some((t) => SIGHT_TYPES.includes(t))) {
    return {
      label: displayName || "Điểm tham quan",
      icon: "📸",
      color: "bg-purple-500/10 text-purple-700 dark:text-purple-400 border-purple-500/25",
    }
  }
  if (displayName) return { label: displayName, icon: "🏷️", color: NEUTRAL }
  if (place.category && place.category !== "GENERAL") return { label: place.category, icon: "🏷️", color: NEUTRAL }
  return null
}

const COMMUNITY_HINTS = ["cộng đồng", "Facebook", "TikTok", "Review", "mẹo", "Lala"]

export function isCommunityTip(place?: Place | null, notes?: string | null): boolean {
  if (place?.provider === "COMMUNITY") return true
  return !!notes && COMMUNITY_HINTS.some((h) => notes.includes(h))
}
