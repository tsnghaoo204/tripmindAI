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
  children?: number
  seniors?: number
  dietary?: Array<"VEGETARIAN" | "VEGAN" | "HALAL" | "NO_SEAFOOD" | "NO_PORK">
  limitedMobility?: boolean
  note?: string
  childAges?: number[]
  motionSickness?: boolean
  allergies?: string
}

export interface TravelPlan {
  origin?: string
  arrivalTime?: string
  departureTime?: string
  transportMode?: "AUTO" | "BUS" | "TRAIN" | "FLIGHT" | "CAR" | "MOTORBIKE"
  localTransport?: "TAXI" | "MOTORBIKE_RENTAL" | "OWN_VEHICLE" | "WALK"
  lodgingType?: "HOMESTAY" | "HOTEL" | "RESORT" | "BOOKED"
  lodgingName?: string
  budgetIncludesTransport?: boolean
  avoid?: string
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
  travelPlan?: TravelPlan
  preferences?: string[]
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
  alternatives?: ActivityAlternative[]
}

export interface ActivityAlternative {
  id: number
  placeId?: number
  placeName?: string
  title: string
  tier: "BUDGET" | "BALANCED" | "PREMIUM"
  tierLabel?: string
  estimatedCost?: number
  rating?: number
  userRatingsTotal?: number
  address?: string
  photoUrl?: string
  isSelected: boolean
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

export type KnowledgeCategory = "FOOD" | "ACCOMMODATION" | "TRANSPORT" | "SIGHTSEEING" | "TIP" | "WARNING"

export interface DestinationKnowledge {
  id: number
  destinationId?: number | null
  destinationName: string
  category: KnowledgeCategory
  categoryLabel?: string
  title: string
  content: string
  placeName?: string | null
  sourceName?: string | null
  sourceUrl?: string | null
  tags: string[]
  priority: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface KnowledgeRequest {
  destinationId?: number | null
  destinationName: string
  category: KnowledgeCategory
  title: string
  content: string
  placeName?: string | null
  sourceName?: string | null
  sourceUrl?: string | null
  tags?: string[]
  priority?: number
  active?: boolean
}

export interface BlogPost {
  id: number
  user: User
  tripId?: number | null
  destinationName: string
  title: string
  content: string
  photoUrls: string[]
  upvoteCount: number
  commentCount: number
  isUpvoted?: boolean
  upvoted?: boolean
  isConvertedToKnowledge?: boolean
  convertedToKnowledge?: boolean
  convertedKnowledgeId?: number | null
  createdAt: string
  updatedAt: string
}

export interface BlogComment {
  id: number
  postId: number
  user: User
  content: string
  upvoteCount: number
  isUpvoted?: boolean
  upvoted?: boolean
  createdAt: string
  updatedAt: string
}

export interface BlogPostRequest {
  destinationName: string
  title: string
  content: string
  tripId?: number | null
  photoUrls?: string[]
}

export interface ExtractedKnowledge {
  destinationName: string
  category: KnowledgeCategory
  categoryLabel?: string
  title: string
  content: string
  placeName?: string
  sourceName?: string
  sourceUrl?: string
  tags: string[]
  priority: number
  active: boolean
  reasoning?: string
}


