import { adminApi } from "@/client/api/admin-api"
import { getErrorMessage } from "@/client/errors"
import type { AdminTripRow } from "@/client/types"
import { CreateTripModal } from "@/components/pages/trips/CreateTripModal"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { ConfirmDialog } from "@/components/ui/confirm-dialog"
import { Input } from "@/components/ui/input"
import { queryKeys } from "@/hooks/query-keys"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { Eye, Pencil, Plus, Search, Trash2 } from "lucide-react"
import { useMemo, useState } from "react"
import { toast } from "sonner"
import { AdminQueryError } from "./AdminQueryError"
import { EditTripDialog } from "./EditTripDialog"

export const adminTripsQuery = {
  queryKey: queryKeys.admin.trips,
  queryFn: () => adminApi.getTrips(0, 50),
}

const PHASE_BADGE = {
  DURING: { label: "ĐANG ĐI", className: "border-emerald-500/40 text-emerald-500 bg-emerald-500/10 font-bold" },
  AFTER: { label: "ĐÃ KẾT THÚC", className: "border-muted-foreground/30 text-muted-foreground" },
  BEFORE: { label: "SẮP TỚI", className: "border-primary/40 text-primary bg-primary/10" },
}

export function TripsTab() {
  const queryClient = useQueryClient()
  const { data, isPending, isError, error } = useQuery(adminTripsQuery)
  const [search, setSearch] = useState("")
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [editingTrip, setEditingTrip] = useState<AdminTripRow | null>(null)
  const [deletingTrip, setDeletingTrip] = useState<AdminTripRow | null>(null)

  const trips = data?.content ?? []
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return trips
    return trips.filter(
      (t) =>
        t.name?.toLowerCase().includes(q) ||
        t.destinationName?.toLowerCase().includes(q) ||
        String(t.userId).includes(q) ||
        String(t.id).includes(q),
    )
  }, [trips, search])

  const deleteMutation = useMutation({
    mutationFn: (tripId: number) => adminApi.deleteTrip(tripId),
    onSuccess: () => {
      setDeletingTrip(null)
      toast.success("Đã xoá chuyến đi thành công!")
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.trips })
    },
    onError: (err) => toast.error(getErrorMessage(err, "Lỗi khi xoá chuyến đi")),
  })

  const handleDelete = (t: AdminTripRow) => {
    setDeletingTrip(t)
  }

  if (isError) return <AdminQueryError error={error} />

  return (
    <Card className="overflow-hidden border-border/80 shadow-sm">
      <div className="p-4 border-b bg-muted/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="font-bold text-sm">
              Danh sách kế hoạch du lịch ({filtered.length} / {data?.totalElements ?? trips.length})
            </h3>
            <Badge variant="secondary" className="text-[10px]">
              Đầy đủ Xem / Sửa / Xoá
            </Badge>
          </div>
          <p className="text-[11px] text-muted-foreground">
            Admin có thể tạo mới, cập nhật tên, giai đoạn, ngân sách hoặc xoá chuyến đi vi phạm
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative w-48 sm:w-64">
            <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" aria-hidden />
            <Input
              aria-label="Tìm chuyến đi"
              placeholder="Tìm theo tên, địa điểm, User ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-8 h-8 text-xs bg-background"
            />
          </div>
          <Button size="sm" onClick={() => setIsCreateOpen(true)} className="h-8 text-xs gap-1.5 cursor-pointer">
            <Plus className="h-3.5 w-3.5" /> Tạo chuyến mới
          </Button>
        </div>
      </div>

      <div className="overflow-x-auto text-xs">
        <table className="w-full text-left">
          <thead className="bg-muted/50 text-muted-foreground font-semibold">
            <tr>
              <th className="p-3">ID</th>
              <th className="p-3">Tên chuyến đi</th>
              <th className="p-3">Điểm đến</th>
              <th className="p-3">User ID</th>
              <th className="p-3">Thời gian</th>
              <th className="p-3">Giai đoạn</th>
              <th className="p-3">Hoạt động</th>
              <th className="p-3 text-right">Thao tác Admin</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border/40">
            {isPending ? (
              <tr>
                <td colSpan={8} className="p-8 text-center text-muted-foreground">
                  Đang tải danh sách chuyến đi...
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={8} className="p-8 text-center text-muted-foreground">
                  Không tìm thấy chuyến đi nào phù hợp.
                </td>
              </tr>
            ) : (
              filtered.map((t) => {
                const phase = PHASE_BADGE[t.phase ?? "BEFORE"]
                return (
                  <tr key={t.id} className="hover:bg-muted/20 transition-colors">
                    <td className="p-3 font-mono font-medium text-muted-foreground">{t.id}</td>
                    <td className="p-3">
                      <div className="font-semibold text-foreground">{t.name || `Chuyến #${t.id}`}</div>
                      <div className="text-[10px] text-muted-foreground">
                        {t.travelers || 1} khách &bull; {t.currency || "VND"}
                      </div>
                    </td>
                    <td className="p-3">
                      <span className="font-medium">{t.destinationName || "--"}</span>
                      {t.country && <span className="text-muted-foreground text-[10px] block">{t.country}</span>}
                    </td>
                    <td className="p-3 font-mono text-muted-foreground">User #{t.userId}</td>
                    <td className="p-3 text-muted-foreground">
                      {t.startDate} &rarr; {t.endDate}
                    </td>
                    <td className="p-3">
                      <Badge variant="outline" className={`text-[10px] ${phase.className}`}>
                        {t.phase ? phase.label : "TỰ ĐỘNG"}
                      </Badge>
                    </td>
                    <td className="p-3 font-semibold">{t.activityCount || 0} mục</td>
                    <td className="p-3 text-right">
                      <div className="inline-flex items-center gap-1">
                        <Button
                          asChild
                          size="sm"
                          variant="outline"
                          className="h-7 px-2 text-xs gap-1 border-primary/30 text-primary hover:bg-primary/10 cursor-pointer"
                        >
                          <Link
                            to="/trips/$tripId"
                            params={{ tripId: String(t.id) }}
                            aria-label={`Xem chuyến ${t.name}`}
                          >
                            <Eye className="h-3 w-3" /> Xem
                          </Link>
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setEditingTrip(t)}
                          className="h-7 px-2 text-xs gap-1 border-amber-500/30 text-amber-600 dark:text-amber-400 hover:bg-amber-500/10 cursor-pointer"
                          aria-label={`Sửa chuyến ${t.name}`}
                        >
                          <Pencil className="h-3 w-3" /> Sửa
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={deleteMutation.isPending}
                          onClick={() => handleDelete(t)}
                          className="h-7 px-2 text-xs gap-1 border-destructive/30 text-destructive hover:bg-destructive/10 cursor-pointer"
                          aria-label={`Xoá chuyến ${t.name}`}
                        >
                          <Trash2 className="h-3 w-3" /> Xoá
                        </Button>
                      </div>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      <EditTripDialog trip={editingTrip} onClose={() => setEditingTrip(null)} />
      <CreateTripModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={() => {
          queryClient.invalidateQueries({ queryKey: queryKeys.admin.trips })
          queryClient.invalidateQueries({ queryKey: ["trips"] })
        }}
      />
      <ConfirmDialog
        open={deletingTrip !== null}
        onOpenChange={(open) => !open && setDeletingTrip(null)}
        title="Xoá chuyến đi khỏi hệ thống"
        description={`Bạn có chắc muốn xoá chuyến đi '${deletingTrip?.name || deletingTrip?.id}'? Mọi hoạt động, chi tiêu và checklist liên quan sẽ bị xoá vĩnh viễn.`}
        confirmText="Xoá vĩnh viễn"
        variant="destructive"
        isLoading={deleteMutation.isPending}
        onConfirm={() => {
          if (deletingTrip) deleteMutation.mutate(deletingTrip.id)
        }}
      />
    </Card>
  )
}
