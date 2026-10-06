export interface ApiResponse<T> {
  success: boolean
  code?: string
  message?: string
  data?: T
  details?: unknown
  timestamp?: string
}

export interface User {
  id: number
  email: string
  name: string
  avatarUrl?: string
  role: "USER" | "ADMIN"
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInMs: number
  user: User
}

export type TravelStyle = "RELAXED" | "BALANCED" | "FAST_PACED"
export type BudgetPreference = "BUDGET" | "MODERATE" | "LUXURY"
export type TripPhase = "BEFORE" | "DURING" | "AFTER"

export interface GroupProfile {
  childrenCount: number
  seniorsCount: number
  hasVegetarian: boolean
  hasHalal: boolean
  hasMobilityDifficulty: boolean
}

export interface Trip {
  id: number
  name: string
  title?: string
  destination: {
    id: number
    name: string
    country: string
    latitude: number
    longitude: number
  }
  startDate: string
  endDate: string
  travelers: number
  budget?: number
  currency: string
  travelStyle?: TravelStyle
  budgetPreference?: BudgetPreference
  groupProfile?: GroupProfile
  phase: TripPhase
  planningProgress: number
  isOwner?: boolean
}

export type ActivityType =
  | "ATTRACTION"
  | "RESTAURANT"
  | "HOTEL"
  | "FLIGHT"
  | "TRAIN"
  | "BUS"
  | "OTHER"

export interface Activity {
  id: number
  dayId: number
  dayNumber: number
  orderIndex: number
  title: string
  activityType: ActivityType
  placeId?: number
  placeName?: string
  place?: {
    id?: number
    provider?: string
    externalId?: string
    name?: string
    category?: string
    primaryType?: string
    primaryTypeDisplayName?: string
    types?: string[]
    address?: string
    formattedAddress?: string
    latitude?: number
    longitude?: number
    priceLevel?: number
    rating?: number
    userRatingsTotal?: number
  }
  latitude?: number
  longitude?: number
  startTime?: string
  endTime?: string
  estimatedCost?: number
  costSource?: "MANUAL" | "PRICE_LEVEL" | "AI" | "USER"
  estimatedCostSource?: "USER" | "PRICE_LEVEL" | "AI"
  notes?: string
  status: "PLANNED" | "COMPLETED" | "SKIPPED"
  fromProposalId?: number
}

export interface ItineraryDay {
  id: number
  tripId: number
  dayNumber: number
  date: string
  title?: string
  activities: Activity[]
}

export interface AiProposal {
  id: number
  tripId: number
  summary: string
  status: "PENDING" | "APPLIED" | "REJECTED" | "EXPIRED" | "REVERTED"
  estimatedCostDelta: number
  travelTimeDelta: number
  expiresAt: string
}

export interface BudgetSummary {
  tripId: number
  budget?: number
  currency: string
  estimatedTotal: number
  actualTotal: number
  remaining?: number
  usedRatio?: number
  warningLevel: "NONE" | "NEAR_LIMIT" | "OVER"
  dailyAllowance?: {
    allowancePerDay: number
    spentToday: number
    remainingToday: number
    daysLeft: number
  }
}

export interface Expense {
  id: number
  tripId: number
  amount: number
  currency: string
  category: "FOOD" | "ACCOMMODATION" | "TRANSPORT" | "ACTIVITIES" | "SHOPPING" | "OTHER"
  description: string
  date: string
}

export interface ChecklistItem {
  id: number
  tripId: number
  title: string
  category: "DOCUMENTS" | "CLOTHING" | "ELECTRONICS" | "HEALTH" | "MONEY" | "BOOKING" | "KIDS" | "OTHER"
  kind: "PACK" | "TODO"
  isCompleted: boolean
  dueDate?: string
  reason?: string
}

export interface DayWeather {
  date: string
  dayNumber: number
  tempMin: number
  tempMax: number
  precipitationProbability: number
  summary: string
  source: "FORECAST" | "CLIMATE_NORMAL" | "OBSERVED"
}

export interface TripWeather {
  tripId: number
  timezone: string
  days: DayWeather[]
  reason?: "WEATHER_UNAVAILABLE" | "PARTIAL"
}
