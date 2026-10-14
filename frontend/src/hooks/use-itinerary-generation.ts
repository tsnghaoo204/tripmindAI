import { aiApi } from "@/client/api/ai-api"
import { getErrorMessage } from "@/client/errors"
import type { GenerateJobStatus } from "@/client/types"
import { useCallback, useEffect, useRef, useState } from "react"
import { toast } from "sonner"

const POLL_INTERVAL_MS = 2000

/** Starts itinerary generation and follows it via SSE, falling back to polling if the stream drops. */
export function useItineraryGeneration(tripId: number, onFinished: () => void) {
  const [job, setJob] = useState<GenerateJobStatus | null>(null)
  const [isGenerating, setIsGenerating] = useState(false)
  const abortRef = useRef<(() => void) | null>(null)
  const pollTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const runRef = useRef(0)
  const onFinishedRef = useRef(onFinished)

  useEffect(() => {
    onFinishedRef.current = onFinished
  }, [onFinished])

  const stop = useCallback(() => {
    runRef.current += 1
    abortRef.current?.()
    abortRef.current = null
    if (pollTimerRef.current) clearTimeout(pollTimerRef.current)
    pollTimerRef.current = null
  }, [])

  // Cancel any run when the trip changes or the page unmounts
  // biome-ignore lint/correctness/useExhaustiveDependencies: tripId is the reset trigger
  useEffect(() => {
    setJob(null)
    setIsGenerating(false)
    return stop
  }, [tripId, stop])

  const start = useCallback(async () => {
    stop()
    const run = runRef.current
    const isStale = () => run !== runRef.current
    setIsGenerating(true)

    let started: GenerateJobStatus
    try {
      started = await aiApi.startGenerateItinerary(tripId)
    } catch (err) {
      if (isStale()) return
      setIsGenerating(false)
      toast.error(getErrorMessage(err, "Lỗi khi kích hoạt AI sinh lịch trình"))
      return
    }
    if (isStale()) return
    setJob(started)
    toast.info("Đã bắt đầu tác vụ sinh lịch trình...")

    const finish = (status: GenerateJobStatus) => {
      if (isStale()) return
      stop()
      setJob(status)
      setIsGenerating(false)
      if (status.state === "DONE") {
        toast.success("Sinh lịch trình thành công!")
        onFinishedRef.current()
      } else {
        toast.error(status.error || "Không thể sinh lịch trình")
      }
    }

    const poll = async () => {
      if (isStale()) return
      try {
        const status = await aiApi.getGenerateStatus(tripId, started.jobId)
        if (isStale()) return
        if (status.state === "DONE" || status.state === "FAILED") return finish(status)
        setJob(status)
        pollTimerRef.current = setTimeout(poll, POLL_INTERVAL_MS)
      } catch (err) {
        if (isStale()) return
        stop()
        setIsGenerating(false)
        toast.error(getErrorMessage(err, "Không thể kiểm tra tiến độ sinh lịch trình"))
      }
    }

    abortRef.current = aiApi.streamGenerateStatus(tripId, started.jobId, {
      onProgress: (status) => !isStale() && setJob(status),
      onDone: finish,
      // Both cases are confirmed through the status endpoint
      onError: () => void poll(),
      onInterrupted: () => void poll(),
    })
  }, [tripId, stop])

  return { job, isGenerating, start }
}
