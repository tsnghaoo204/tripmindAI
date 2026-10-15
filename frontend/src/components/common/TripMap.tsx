import L from "leaflet"
import { MapPin } from "lucide-react"
import { useEffect, useRef } from "react"

export interface MapPoint {
  id: number | string
  name: string
  latitude: number
  longitude: number
  address?: string | null
  category?: string | null
  time?: string | null
  orderIndex?: number
  rating?: number | null
}

export interface TripMapProps {
  points: MapPoint[]
  showRoute?: boolean
  height?: string
  className?: string
  selectedPointId?: number | string | null
  onPointClick?: (point: MapPoint) => void
  onSelectPoint?: (point: MapPoint) => void
}

export function TripMap({
  points,
  showRoute = true,
  height = "h-[420px]",
  className = "",
  selectedPointId,
  onPointClick,
  onSelectPoint,
}: TripMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<L.Map | null>(null)
  const layerGroupRef = useRef<L.LayerGroup | null>(null)
  const handlePointSelect = onPointClick || onSelectPoint
  const onSelectRef = useRef(handlePointSelect)
  onSelectRef.current = handlePointSelect

  // Initialize map once
  useEffect(() => {
    if (!containerRef.current || mapInstanceRef.current) return

    // Default center in Vietnam (Da Nang coordinates approx)
    const initialCenter: [number, number] = [16.0544, 108.2022]
    const map = L.map(containerRef.current, {
      center: initialCenter,
      zoom: 13,
      zoomControl: true,
      attributionControl: false,
    })

    // CartoDB Voyager tiles (clean, modern, light & dark friendly)
    L.tileLayer("https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png", {
      maxZoom: 19,
      subdomains: "abcd",
    }).addTo(map)

    const layerGroup = L.layerGroup().addTo(map)
    layerGroupRef.current = layerGroup
    mapInstanceRef.current = map

    return () => {
      map.remove()
      mapInstanceRef.current = null
      layerGroupRef.current = null
    }
  }, [])

  // Update markers, polyline and bounds when points change
  useEffect(() => {
    const map = mapInstanceRef.current
    const layerGroup = layerGroupRef.current
    if (!map || !layerGroup) return

    layerGroup.clearLayers()

    // Filter points with valid coordinates
    const validPoints = points.filter(
      (p) =>
        typeof p.latitude === "number" &&
        typeof p.longitude === "number" &&
        !Number.isNaN(p.latitude) &&
        !Number.isNaN(p.longitude),
    )

    if (validPoints.length === 0) return

    const latLngs: L.LatLngTuple[] = []

    validPoints.forEach((point, index) => {
      const latLng: L.LatLngTuple = [point.latitude, point.longitude]
      latLngs.push(latLng)

      const isSelected = selectedPointId != null && String(selectedPointId) === String(point.id)
      const num = point.orderIndex != null ? point.orderIndex + 1 : index + 1

      // Custom div marker
      const markerHtml = `
        <div class="trip-map-marker ${isSelected ? "selected" : ""}" style="
          display: flex;
          align-items: center;
          justify-content: center;
          width: 32px;
          height: 32px;
          border-radius: 9999px;
          background: ${isSelected ? "#f59e0b" : "#2563eb"};
          color: white;
          font-weight: 800;
          font-size: 13px;
          box-shadow: 0 4px 12px rgba(0,0,0,0.3);
          border: 2.5px solid white;
          cursor: pointer;
          transition: transform 0.2s;
        ">
          ${num}
        </div>
      `

      const customIcon = L.divIcon({
        className: "custom-div-icon",
        html: markerHtml,
        iconSize: [32, 32],
        iconAnchor: [16, 16],
        popupAnchor: [0, -18],
      })

      const marker = L.marker(latLng, { icon: customIcon })

      const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
        `${point.name} ${point.address || ""}`.trim(),
      )}`

      const popupContent = `
        <div style="font-family: inherit; font-size: 12px; line-height: 1.4; min-width: 180px; max-width: 240px; padding: 4px;">
          <div style="font-weight: 700; font-size: 14px; margin-bottom: 4px; color: #0f172a;">
            ${point.name}
          </div>
          ${point.time ? `<div style="color: #2563eb; font-weight: 600; font-size: 11px; margin-bottom: 4px;">🕒 ${point.time}</div>` : ""}
          ${point.category ? `<div style="font-size: 11px; color: #64748b; margin-bottom: 4px;">🏷️ ${point.category}</div>` : ""}
          ${point.rating ? `<div style="font-size: 11px; color: #eab308; font-weight: 600; margin-bottom: 4px;">★ ${point.rating}</div>` : ""}
          ${point.address ? `<div style="font-size: 11px; color: #475569; margin-bottom: 6px;">📍 ${point.address}</div>` : ""}
          <div style="margin-top: 8px; border-top: 1px solid #e2e8f0; padding-top: 6px;">
            <a href="${googleMapsUrl}" target="_blank" rel="noreferrer" style="
              display: inline-flex;
              align-items: center;
              gap: 4px;
              color: #2563eb;
              font-weight: 600;
              text-decoration: none;
              font-size: 11px;
            ">
              Mở trên Google Maps ↗
            </a>
          </div>
        </div>
      `

      marker.bindPopup(popupContent)

      marker.on("click", () => {
        onSelectRef.current?.(point)
      })

      marker.addTo(layerGroup)
    })

    // Draw route connecting sequential points if requested
    if (showRoute && latLngs.length > 1) {
      const polyline = L.polyline(latLngs, {
        color: "#3b82f6",
        weight: 3.5,
        opacity: 0.85,
        dashArray: "6, 8",
        lineCap: "round",
      })
      polyline.addTo(layerGroup)
    }

    // Auto fit bounds
    if (latLngs.length === 1) {
      map.setView(latLngs[0], 14)
    } else if (latLngs.length > 1) {
      const bounds = L.latLngBounds(latLngs)
      map.fitBounds(bounds, { padding: [45, 45], maxZoom: 16 })
    }
  }, [points, showRoute, selectedPointId])

  return (
    <div className={`relative overflow-hidden rounded-2xl border border-border shadow-sm ${height} ${className}`}>
      <div ref={containerRef} className="w-full h-full z-0" />
      {points.length === 0 && (
        <div className="absolute inset-0 bg-muted/60 backdrop-blur-xs flex flex-col items-center justify-center p-4 text-center z-10">
          <div className="p-3 rounded-full bg-primary/10 text-primary mb-2">
            <MapPin className="h-6 w-6" />
          </div>
          <p className="text-sm font-semibold text-foreground">Chưa có toạ độ địa điểm</p>
          <p className="text-xs text-muted-foreground mt-0.5">Các địa điểm trong ngày chưa có thông tin toạ độ GPS.</p>
        </div>
      )}
    </div>
  )
}
