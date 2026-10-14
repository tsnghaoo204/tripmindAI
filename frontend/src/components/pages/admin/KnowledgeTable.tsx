import type { DestinationKnowledge } from "@/client/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { BookOpen, CheckCircle2, ExternalLink, MapPin, Pencil, Star, Trash2, XCircle } from "lucide-react"
import { KNOWLEDGE_CATEGORIES } from "./knowledge-categories"

interface KnowledgeTableProps {
  items: DestinationKnowledge[]
  busy: boolean
  onEdit: (item: DestinationKnowledge) => void
  onToggle: (item: DestinationKnowledge) => void
  onDelete: (item: DestinationKnowledge) => void
}

const PRIORITY_LABEL: Record<number, string> = { 1: "Thường", 2: "Khuyến nghị", 3: "Rất cao" }

export function KnowledgeTable({ items, busy, onEdit, onToggle, onDelete }: KnowledgeTableProps) {
  return (
    <div className="overflow-x-auto text-xs">
      <table className="w-full text-left">
        <thead className="bg-muted/50 text-muted-foreground font-semibold">
          <tr>
            <th className="p-3 w-12 text-center">ID</th>
            <th className="p-3 w-32">Điểm đến</th>
            <th className="p-3 w-36">Chuyên mục</th>
            <th className="p-3 min-w-[280px]">Nội dung mẹo &amp; Kinh nghiệm</th>
            <th className="p-3 w-40">Nguồn tham khảo</th>
            <th className="p-3 w-28 text-center">Ưu tiên AI</th>
            <th className="p-3 w-28 text-center">Bật cho AI</th>
            <th className="p-3 w-24 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border/40">
          {items.length === 0 ? (
            <tr>
              <td colSpan={8} className="p-12 text-center text-muted-foreground">
                <BookOpen className="h-8 w-8 mx-auto mb-2 opacity-30" aria-hidden />
                <p className="font-medium">Chưa có tri thức nào phù hợp với bộ lọc.</p>
                <p className="text-[11px] mt-1">Bấm "Thêm mẹo du lịch mới" để nạp dữ liệu đầu tiên!</p>
              </td>
            </tr>
          ) : (
            items.map((item) => {
              const cat = KNOWLEDGE_CATEGORIES[item.category] ?? KNOWLEDGE_CATEGORIES.TIP
              const Icon = cat.icon
              return (
                <tr key={item.id} className="hover:bg-muted/20 transition-colors">
                  <td className="p-3 font-mono font-medium text-muted-foreground text-center">{item.id}</td>
                  <td className="p-3 font-semibold text-foreground whitespace-nowrap">
                    <div className="flex items-center gap-1.5">
                      <MapPin className="h-3.5 w-3.5 text-primary shrink-0" aria-hidden />
                      <span>{item.destinationName}</span>
                    </div>
                  </td>
                  <td className="p-3 whitespace-nowrap">
                    <Badge
                      variant="outline"
                      className={`text-[10px] px-2 py-0.5 gap-1 font-semibold ${cat.badgeClass}`}
                    >
                      <Icon className="h-3 w-3" aria-hidden />
                      {cat.label}
                    </Badge>
                  </td>
                  <td className="p-3">
                    <div className="font-semibold text-foreground text-xs mb-1">{item.title}</div>
                    <p className="text-muted-foreground text-[11px] line-clamp-2 leading-relaxed">{item.content}</p>
                    <div className="flex flex-wrap items-center gap-1.5 mt-1.5">
                      {item.placeName && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-medium text-primary bg-primary/10 px-1.5 py-0.5 rounded">
                          📍 {item.placeName}
                        </span>
                      )}
                      {item.tags?.map((t) => (
                        <span
                          key={t}
                          className="inline-flex items-center gap-0.5 text-[10px] text-muted-foreground bg-muted px-1.5 py-0.5 rounded"
                        >
                          #{t}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="p-3 text-[11px] text-muted-foreground">
                    {item.sourceName ? (
                      <div className="font-medium text-foreground flex items-center gap-1">
                        <span>{item.sourceName}</span>
                        {item.sourceUrl && (
                          <a
                            href={item.sourceUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="text-primary hover:underline inline-block shrink-0"
                            aria-label="Xem bài viết gốc"
                          >
                            <ExternalLink className="h-3 w-3" />
                          </a>
                        )}
                      </div>
                    ) : (
                      <span className="italic text-muted-foreground/60">Không ghi rõ</span>
                    )}
                  </td>
                  <td className="p-3 text-center">
                    <div
                      className="inline-flex items-center gap-0.5 text-amber-500 font-semibold text-[11px]"
                      aria-label={`Ưu tiên ${item.priority}`}
                    >
                      {Array.from({ length: item.priority }, (_, i) => (
                        <Star key={`star-${i + 1}`} className="h-3 w-3 fill-amber-500 text-amber-500" aria-hidden />
                      ))}
                    </div>
                    <div className="text-[10px] text-muted-foreground">{PRIORITY_LABEL[item.priority] ?? ""}</div>
                  </td>
                  <td className="p-3 text-center">
                    <button
                      type="button"
                      role="switch"
                      aria-checked={item.isActive}
                      aria-label={`Bật/tắt tri thức ${item.title} cho AI`}
                      disabled={busy}
                      onClick={() => onToggle(item)}
                      className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-[10px] font-semibold transition-all cursor-pointer ${
                        item.isActive
                          ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/25"
                          : "bg-muted text-muted-foreground hover:bg-muted/80"
                      }`}
                    >
                      {item.isActive ? (
                        <>
                          <CheckCircle2 className="h-3 w-3" aria-hidden /> Bật
                        </>
                      ) : (
                        <>
                          <XCircle className="h-3 w-3" aria-hidden /> Tắt
                        </>
                      )}
                    </button>
                  </td>
                  <td className="p-3 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => onEdit(item)}
                        className="h-7 w-7 text-muted-foreground hover:text-foreground"
                        aria-label={`Chỉnh sửa tri thức ${item.title}`}
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </Button>
                      <Button
                        size="icon"
                        variant="ghost"
                        disabled={busy}
                        onClick={() => onDelete(item)}
                        className="h-7 w-7 text-destructive hover:bg-destructive/10"
                        aria-label={`Xóa tri thức ${item.title}`}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
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
  )
}
