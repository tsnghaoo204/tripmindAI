import { blogApi } from "@/client/api/blog-api"
import { tripApi } from "@/client/api/trip-api"
import { getErrorMessage } from "@/client/errors"
import type { TripPhase, TripRecapResponse } from "@/client/types"
import { AiChatDrawer } from "@/components/pages/ai/AiChatDrawer"
import { BudgetView } from "@/components/pages/budget/BudgetView"
import { ChecklistView } from "@/components/pages/checklist/ChecklistView"
import { CreatePostDialog } from "@/components/pages/community/CreatePostDialog"
import { DayTimeline } from "@/components/pages/itinerary/DayTimeline"
import { PlaceReviewModal } from "@/components/pages/itinerary/PlaceReviewModal"
import { DuplicateTripModal } from "@/components/pages/trips/DuplicateTripModal"
import { PHASE_LABEL, TripHero } from "@/components/pages/trips/TripHero"
import { Card } from "@/components/ui/card"
import { ConfirmDialog } from "@/components/ui/confirm-dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useItineraryGeneration } from "@/hooks/use-itinerary-generation"
import { useInvalidateTrip, useItinerary, useTrip, useTripWeather } from "@/hooks/use-trips"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { Calendar, DollarSign, Luggage, RotateCw, Sparkles } from "lucide-react"
import { useState } from "react"
import { toast } from "sonner"

export const Route = createFileRoute("/_layout/trips/$tripId")({
  component: TripDetailPage,
})

function defaultDuplicateDate() {
  return new Date(Date.now() + 7 * 86400000).toISOString().split("T")[0]
}

function TripDetailPage() {
  const { tripId: tripIdParam } = Route.useParams()
  const tripId = Number(tripIdParam)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const tripQuery = useTrip(tripId)
  const itineraryQuery = useItinerary(tripId)
  const weatherQuery = useTripWeather(tripId)
  const refreshTrip = useInvalidateTrip(tripId)
  const generation = useItineraryGeneration(tripId, refreshTrip)

  const [activeTab, setActiveTab] = useState("itinerary")
  const [isAiOpen, setIsAiOpen] = useState(false)
  const [isReviewOpen, setIsReviewOpen] = useState(false)
  const [isDuplicateOpen, setIsDuplicateOpen] = useState(false)
  const [isDeleteOpen, setIsDeleteOpen] = useState(false)
  const [recapData, setRecapData] = useState<TripRecapResponse | null>(null)
  const [isRecapModalOpen, setIsRecapModalOpen] = useState(false)

  const recapMutation = useMutation({
    mutationFn: () => blogApi.generateTripRecap(tripId),
    onSuccess: (data) => {
      setRecapData(data)
      setIsRecapModalOpen(true)
      toast.success("AI đã hoàn thành bản thảo review chuyến đi!")
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể tạo bài review tổng kết")),
  })

  const trip = tripQuery.data
  const itinerary = itineraryQuery.data

  const phaseMutation = useMutation({
    mutationFn: (phase: TripPhase | null) => tripApi.updateTripPhase(tripId, phase),
    onSuccess: (_data, phase) => {
      toast.success(`Đã cập nhật giai đoạn chuyến đi: ${phase ? PHASE_LABEL[phase] : "Tự động"}`)
      refreshTrip()
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể cập nhật giai đoạn")),
  })

  const duplicateMutation = useMutation({
    mutationFn: (startDate: string) =>
      tripApi.duplicateTrip(tripId, { name: trip ? `${trip.name} (Bản sao)` : undefined, startDate }),
    onSuccess: (res) => {
      toast.success(`Đã nhân bản chuyến đi! Sao chép ${res.activitiesCopied || 0} hoạt động.`)
      setIsDuplicateOpen(false)
      queryClient.invalidateQueries({ queryKey: ["trips"] })
      navigate({ to: "/trips/$tripId", params: { tripId: String(res.trip.id) } })
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể nhân bản chuyến đi")),
  })

  const deleteMutation = useMutation({
    mutationFn: () => tripApi.deleteTrip(tripId),
    onSuccess: () => {
      toast.success("Đã xoá chuyến đi")
      setIsDeleteOpen(false)
      queryClient.removeQueries({ queryKey: ["trip", tripId] })
      queryClient.invalidateQueries({ queryKey: ["trips"] })
      navigate({ to: "/" })
    },
    onError: (err) => toast.error(getErrorMessage(err, "Không thể xoá chuyến đi")),
  })

  const handleDuplicateTrip = () => {
    setIsDuplicateOpen(true)
  }

  const handleDeleteTrip = () => {
    setIsDeleteOpen(true)
  }

  if (tripQuery.isPending) {
    return <div className="py-20 text-center text-sm text-muted-foreground">Đang tải chi tiết chuyến đi...</div>
  }

  if (!trip) {
    return (
      <div className="py-20 text-center text-sm text-muted-foreground">
        {tripQuery.error ? getErrorMessage(tripQuery.error) : "Không tìm thấy chuyến đi."}
      </div>
    )
  }

  const itineraryEmpty = !itinerary || itinerary.days.every((d) => d.activities.length === 0)
  const job = generation.job

  return (
    <div className="container mx-auto px-4 sm:px-6 py-6 space-y-6 pb-24">
      <TripHero
        trip={trip}
        canGenerate={itineraryQuery.isSuccess && itineraryEmpty}
        isGenerating={generation.isGenerating}
        onGenerate={generation.start}
        onChangePhase={(p) => phaseMutation.mutate(p)}
        onReview={() => setIsReviewOpen(true)}
        onDuplicate={handleDuplicateTrip}
        onDelete={handleDeleteTrip}
        onRecap={() => recapMutation.mutate()}
        isGeneratingRecap={recapMutation.isPending}
      />

      {generation.isGenerating && job && (
        <Card className="border-primary/40 bg-primary/5 p-4 animate-pulse" aria-live="polite">
          <div className="flex items-center justify-between text-xs font-semibold text-primary mb-2">
            <span className="flex items-center gap-2">
              <RotateCw className="h-4 w-4 animate-spin" aria-hidden />
              {job.step}
            </span>
            <span>{Math.round(job.progress * 100)}%</span>
          </div>
          <div className="w-full bg-muted rounded-full h-2 overflow-hidden">
            <div
              className="bg-primary h-full transition-all duration-300"
              style={{ width: `${Math.round(job.progress * 100)}%` }}
            />
          </div>
        </Card>
      )}

      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <TabsList aria-label="Nội dung chuyến đi">
          <TabsTrigger value="itinerary">
            <Calendar className="h-4 w-4" aria-hidden /> Lịch Trình Chi Tiết
          </TabsTrigger>
          <TabsTrigger value="budget">
            <DollarSign className="h-4 w-4" aria-hidden /> Ngân Sách & Chi Tiêu
          </TabsTrigger>
          <TabsTrigger value="checklist">
            <Luggage className="h-4 w-4" aria-hidden /> Checklist Chuẩn Bị
          </TabsTrigger>
        </TabsList>

        <TabsContent value="itinerary">
          {itineraryQuery.isError ? (
            <div className="py-12 text-center text-xs text-muted-foreground">
              {getErrorMessage(itineraryQuery.error, "Không thể tải lịch trình")}
            </div>
          ) : (
            <DayTimeline
              tripId={tripId}
              currency={trip.currency}
              days={itinerary?.days || []}
              weather={weatherQuery.data}
              onRefresh={refreshTrip}
              onOpenAiChat={() => setIsAiOpen(true)}
            />
          )}
        </TabsContent>
        <TabsContent value="budget">
          <BudgetView tripId={tripId} />
        </TabsContent>
        <TabsContent value="checklist">
          <ChecklistView tripId={tripId} />
        </TabsContent>
      </Tabs>

      <button
        type="button"
        onClick={() => setIsAiOpen(true)}
        className="fixed bottom-5 right-5 sm:bottom-6 sm:right-6 z-40 flex items-center gap-2 sm:gap-2.5 px-3.5 py-2.5 sm:px-4 sm:py-3 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-semibold text-xs sm:text-sm shadow-xl shadow-blue-500/30 hover:scale-105 transition-all cursor-pointer group"
        title="Trợ lý AI TripMind"
        aria-label="Mở Trợ lý AI TripMind"
      >
        <div className="p-1 rounded-full bg-white/20">
          <Sparkles className="h-3.5 w-3.5 sm:h-4 sm:w-4 text-amber-300 animate-pulse" aria-hidden />
        </div>
        <span className="hidden sm:inline">Trợ lý AI TripMind</span>
        <span className="sm:hidden font-bold">Trợ lý AI</span>
      </button>

      <AiChatDrawer
        tripId={tripId}
        isOpen={isAiOpen}
        onClose={() => setIsAiOpen(false)}
        onProposalApplied={refreshTrip}
      />

      <PlaceReviewModal tripId={tripId} isOpen={isReviewOpen} onClose={() => setIsReviewOpen(false)} />

      <CreatePostDialog
        isOpen={isRecapModalOpen}
        onClose={() => setIsRecapModalOpen(false)}
        initialData={recapData}
        onCreated={() => {
          queryClient.invalidateQueries({ queryKey: ["blog"] })
          toast.success("Đã đăng bài review tổng kết lên Cộng đồng!")
        }}
      />

      <DuplicateTripModal
        open={isDuplicateOpen}
        onOpenChange={setIsDuplicateOpen}
        defaultDate={defaultDuplicateDate()}
        isLoading={duplicateMutation.isPending}
        onDuplicate={(date) => duplicateMutation.mutate(date)}
      />

      <ConfirmDialog
        open={isDeleteOpen}
        onOpenChange={setIsDeleteOpen}
        title="Xoá chuyến đi"
        description="Bạn có chắc chắn muốn xoá toàn bộ chuyến đi này? Thao tác này không thể hoàn tác."
        confirmText="Xoá chuyến đi"
        variant="destructive"
        isLoading={deleteMutation.isPending}
        onConfirm={() => deleteMutation.mutate()}
      />
    </div>
  )
}
