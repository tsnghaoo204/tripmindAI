// Mirrors backend DTOs in backend/src/main/java/com/tripmind/domains and enums

export interface ApiResponse<T> {
  success: boolean
  code?: string
  message?: string
  data?: T
  details?: unknown
  timestamp?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
}

export type UserRole = "USER" | "ADMIN"

export interface User {
  id: number
  email: string
  name: string
  avatarUrl?: string | null
  role: UserRole
  active?: boolean
  createdAt?: string
}

/** Public author info embedded in blog posts and comments. */
export interface BlogUser {
  id: number
  name: string
  avatarUrl?: string | null
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInMs: number
  user: User
}

export type TravelStyle = "RELAXED" | "BALANCED" | "FAST_PACED"
export type BudgetPreference = "BUDGET" | "MODERATE" | "LUXURY"
export type TripPhase = "BEFORE" | "DURING" | "AFTER"
export type DietaryRestriction = "VEGETARIAN" | "VEGAN" | "HALAL" | "NO_SEAFOOD" | "NO_PORK"

export interface GroupProfile {
  children?: number | null
  seniors?: number | null
  dietary?: DietaryRestriction[] | null
  limitedMobility?: boolean | null
  note?: string | null
  childAges?: number[] | null
  motionSickness?: boolean | null
  allergies?: string | null
}

export interface TravelPlan {
  origin?: string | null
  arrivalTime?: string | null
  departureTime?: string | null
  transportMode?: "AUTO" | "BUS" | "TRAIN" | "FLIGHT" | "CAR" | "MOTORBIKE" | null
  localTransport?: "TAXI" | "MOTORBIKE_RENTAL" | "OWN_VEHICLE" | "WALK" | null
  lodgingType?: "HOMESTAY" | "HOTEL" | "RESORT" | "BOOKED" | null
  lodgingName?: string | null
  budgetIncludesTransport?: boolean | null
  avoid?: string | null
}

export interface Destination {
  id: number
  provider?: string
  externalId?: string | null
  name: string
  country: string
  latitude?: number | null
  longitude?: number | null
  timezone?: string | null
  formattedAddress?: string | null
  saved?: boolean
}

export interface TripDaySummary {
  id: number
  dayNumber: number
  date: string
  note?: string | null
  activityCount: number
}

export interface Trip {
  id: number
  name: string
  destination: Destination
  startDate: string
  endDate: string
  totalDays?: number
  travelers: number
  budget?: number | null
  currency: string
  travelStyle?: TravelStyle | null
  budgetPreference?: BudgetPreference | null
  preferences?: string[] | null
  groupProfile?: GroupProfile | null
  travelPlan?: TravelPlan | null
  phase: TripPhase
  phaseSource?: string
  today?: string
  planningProgress: number
  days?: TripDaySummary[]
  createdAt?: string
  updatedAt?: string
}

export interface DuplicateTripResponse {
  trip: Trip
  activitiesCopied: number
  checklistItemsCopied: number
  previouslySkipped: Array<{ activityId: number; dayNumber: number; title: string; skipReason: string }>
}

export type ActivityType = "SIGHTSEEING" | "FOOD" | "TRANSPORT" | "ACCOMMODATION" | "REST" | "OTHER"
export type ActivityStatus = "PLANNED" | "DOING" | "DONE" | "SKIPPED"
export type ActivityCreator = "USER" | "AI"
export type CostSource = "USER" | "PRICE_LEVEL" | "AI"
export type SkipReason = "RAIN" | "TIRED" | "CLOSED" | "NO_TIME" | "OTHER"

export interface Place {
  id: number
  provider: string
  externalId: string
  name: string
  category?: string | null
  primaryType?: string | null
  primaryTypeDisplayName?: string | null
  types?: string[] | null
  latitude?: number | null
  longitude?: number | null
  rating?: number | null
  userRatingsTotal?: number | null
  priceLevel?: number | null
  address?: string | null
  phoneNumber?: string | null
  websiteUrl?: string | null
  openingHours?: unknown
  reviews?: unknown
  photoUrls?: unknown
}

export interface ActivityAlternative {
  id: number
  placeId?: number | null
  placeName?: string | null
  title: string
  tier: "BUDGET" | "BALANCED" | "PREMIUM"
  tierLabel?: string | null
  estimatedCost?: number | null
  rating?: number | null
  userRatingsTotal?: number | null
  address?: string | null
  photoUrl?: string | null
  isSelected: boolean
}

export interface Activity {
  id: number
  dayId: number
  dayNumber: number
  orderIndex: number
  title: string
  activityType: ActivityType
  createdBy?: ActivityCreator
  fromProposalId?: number | null
  startTime?: string | null
  endTime?: string | null
  estimatedCost?: number | null
  estimatedCostSource?: CostSource | null
  transportationMode?: string | null
  notes?: string | null
  status: ActivityStatus
  skipReason?: SkipReason | null
  actualStart?: string | null
  actualEnd?: string | null
  place?: Place | null
  placeName?: string | null
  distanceToNextMeters?: number | null
  formattedDistanceToNext?: string | null
  travelTimeToNextMinutes?: number | null
  formattedTravelTimeToNext?: string | null
  idealTimingTip?: string | null
  alternatives?: ActivityAlternative[] | null
}

export interface ItineraryDay {
  id: number
  tripId: number
  dayNumber: number
  date: string
  note?: string | null
  activities: Activity[]
  totalDayDistanceMeters?: number | null
  formattedTotalDayDistance?: string | null
  totalDayTravelMinutes?: number | null
  formattedTotalDayTravelTime?: string | null
}

export interface Itinerary {
  tripId: number
  tripName?: string
  destinationName?: string
  startDate?: string
  endDate?: string
  totalDays?: number
  totalActivities?: number
  totalEstimatedCost?: number
  currency?: string
  days: ItineraryDay[]
  totalTripDistanceMeters?: number | null
  formattedTotalTripDistance?: string | null
}

export type ProposalStatus = "PENDING" | "APPLIED" | "REJECTED" | "EXPIRED" | "REVERTED"

export interface Proposal {
  id: number
  tripId: number
  conversationId?: number | null
  kind: "ITINERARY" | "EXPENSE"
  status: ProposalStatus
  summary: string
  reason?: string | null
  changes?: unknown
  rejected?: unknown
  estimatedCostDelta?: number | null
  travelTimeDelta?: number | null
  expiresAt?: string | null
  appliedAt?: string | null
  revertedAt?: string | null
  undoAvailableUntil?: string | null
}

export interface ApplyProposalResponse {
  proposalId: number
  applied: number
  skipped?: Array<{ op: string; activityId?: number | null; title?: string | null; reason: string }>
  undoAvailableUntil?: string | null
  itinerary?: Itinerary
}

export interface UndoProposalResponse {
  proposalId: number
  preview: boolean
  reverted: number
  willDelete?: string[]
  willRestore?: string[]
  willRevert?: string[]
  undoSkipped?: Array<{ title: string; reason: string }>
  placesKept?: string[]
  itinerary?: Itinerary
}

export type ExpenseCategory = "ACCOMMODATION" | "FOOD" | "TRANSPORTATION" | "ACTIVITIES" | "SHOPPING" | "OTHER"

export interface DailyAllowance {
  mode: "PLAN" | "TODAY"
  date?: string
  simulated?: boolean
  dayNumber?: number | null
  remainingDays: number
  allowance: number
  spentToday?: number | null
  todayLeft?: number | null
  status: "ON_TRACK" | "NEAR" | "OVER"
  averageSpentPerDay?: number | null
  overBy?: number | null
}

export interface BudgetSummary {
  tripId: number
  budget?: number | null
  currency: string
  estimatedTotal: number
  actualTotal: number
  remaining?: number | null
  usedRatio?: number | null
  warningLevel: "NONE" | "NEAR_LIMIT" | "OVER"
  byCategory?: Partial<Record<ExpenseCategory, { estimated: number; actual: number }>>
  daily?: DailyAllowance | null
  dailyReason?: string | null
}

export interface Expense {
  id: number
  tripId: number
  activityId?: number | null
  activityTitle?: string | null
  category: ExpenseCategory
  amount: number
  currency: string
  description?: string | null
  expenseDate: string
  source?: "FORM" | "CHAT"
  createdAt?: string
}

export type ChecklistCategory =
  | "CLOTHING"
  | "TOILETRIES"
  | "HEALTH"
  | "DOCUMENTS"
  | "ELECTRONICS"
  | "MONEY"
  | "BOOKING"
  | "KIDS"
  | "OTHER"
export type ChecklistKind = "PACK" | "TODO"

export interface ChecklistItem {
  id: number
  kind: ChecklistKind
  title: string
  category?: ChecklistCategory | null
  dueDate?: string | null
  done: boolean
  source?: "USER" | "SUGGESTED" | "AI"
  reason?: string | null
}

export interface DayWeather {
  date: string
  dayNumber: number
  source: "FORECAST" | "CLIMATE_NORMAL" | "OBSERVED"
  tempMin?: number | null
  tempMax?: number | null
  precipitationProbability?: number | null
  precipitationMm?: number | null
  weatherCode?: number | null
  summary?: string | null
}

export interface TripWeather {
  tripId: number
  timezone: string
  days: DayWeather[]
  reason?: string | null
}

export type PlaceVerdict = "LIKE" | "DISLIKE"

export interface PlaceRating {
  place: Place
  verdict?: PlaceVerdict | null
  note?: string | null
  tripId: number
  dayNumbers?: number[]
  updatedAt?: string | null
}

export interface SavedPlace {
  id: number
  place: Place
  createdAt: string
}

export interface Conversation {
  id: number
  tripId: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  role: "SYSTEM" | "USER" | "ASSISTANT" | "TOOL"
  content: string
  attachments?: unknown
  tokenUsage?: unknown
  createdAt: string
}

export type ToolExecutionStatus = "OK" | "ERROR" | "TIMEOUT"

export interface ToolExecution {
  id: number
  conversationId?: number | null
  messageId?: number | null
  tripId?: number | null
  userId?: number | null
  toolName: string
  arguments?: unknown
  result?: unknown
  status: ToolExecutionStatus
  errorMessage?: string | null
  executionTimeMs?: number | null
  createdAt: string
}

export interface ExplanationResponse {
  activityId: number
  createdBy: ActivityCreator
  known: boolean
  unknownReason?: string | null
  proposalId?: number | null
  proposalSummary?: string | null
  proposalReason?: string | null
  changeReason?: string | null
  appliedAt?: string | null
  tools?: ToolExecution[]
  rejected?: unknown
}

export interface GenerateJobStatus {
  jobId: string
  state: "QUEUED" | "RUNNING" | "DONE" | "FAILED"
  step: string
  progress: number
  result?: {
    proposalId: number
    activitiesCreated: number
    rejectedCount: number
    undoAvailableUntil?: string
  }
  error?: string
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
  tags: string[] | null
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
  user: BlogUser
  tripId?: number | null
  destinationName: string
  title: string
  content: string
  photoUrls: string[] | null
  upvoteCount: number
  commentCount: number
  isUpvoted: boolean
  isConvertedToKnowledge: boolean
  convertedKnowledgeId?: number | null
  createdAt: string
  updatedAt: string
}

export interface BlogComment {
  id: number
  postId: number
  user: BlogUser
  content: string
  upvoteCount: number
  isUpvoted: boolean
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
  placeName?: string | null
  sourceName?: string | null
  sourceUrl?: string | null
  tags: string[]
  priority: number
  active: boolean
  reasoning?: string | null
  /** Excerpt of the post/comments the knowledge was taken from. */
  sourceSnippet?: string | null
}

export interface AiUsageMetrics {
  from: string
  to: string
  assistantTurns: number
  totalTokens: number
  promptTokens: number
  completionTokens: number
  toolCalls: number
  toolErrorRate: number
  avgToolMs: number
  topTools: Array<{ tool: string; calls: number; failures: number; avg_ms: number | null }>
  perDay: Array<{ day: string; calls: number; failures: number }>
}

export interface AdminUserRow {
  id: number
  email: string
  name: string
  role: UserRole
  active: boolean
  createdAt: string
  tripCount: number
}

export interface AdminTripRow {
  id: number
  name: string
  userId: number
  destinationName: string
  country?: string | null
  startDate: string
  endDate: string
  travelers: number
  currency: string
  phase?: TripPhase | null
  activityCount: number
  conversationCount: number
  createdAt: string
}
