package com.tripmind.services.ai;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Danh mục công cụ của trợ lý: mọi bean đánh dấu {@link AgentToolSet} được quét lấy các
 * phương thức {@code @Tool}. Mỗi công cụ có một nhãn tiếng Việt để giao diện hiện tiến trình.
 */
@Component
public class ToolRegistry {

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("get_current_trip", "Đang đọc thông tin chuyến đi..."),
            Map.entry("get_itinerary", "Đang đọc lịch trình..."),
            Map.entry("get_user_preferences", "Đang xem sở thích của bạn..."),
            Map.entry("get_saved_places", "Đang xem địa điểm đã lưu..."),
            Map.entry("calculate_trip_budget", "Đang tính ngân sách..."),
            Map.entry("get_weather", "Đang kiểm tra thời tiết..."),
            Map.entry("search_places", "Đang tìm địa điểm..."),
            Map.entry("calculate_distance", "Đang tính quãng đường..."),
            Map.entry("optimize_day_order", "Đang tối ưu thứ tự trong ngày..."),
            Map.entry("propose_itinerary_changes", "Đang dựng đề xuất..."),
            Map.entry("propose_places", "Đang chuẩn bị gợi ý địa điểm..."),
            Map.entry("suggest_checklist", "Đang chuẩn bị danh sách cần mang..."));

    private final Map<String, ToolCallback> callbacks = new LinkedHashMap<>();

    public ToolRegistry(List<AgentToolSet> toolSets) {
        ToolCallback[] all = MethodToolCallbackProvider.builder()
                .toolObjects(toolSets.toArray())
                .build()
                .getToolCallbacks();
        for (ToolCallback callback : all) {
            callbacks.put(callback.getToolDefinition().name(), callback);
        }
    }

    public List<ToolCallback> all() {
        return List.copyOf(callbacks.values());
    }

    public ToolCallback find(String name) {
        return callbacks.get(name);
    }

    public String label(String name) {
        return LABELS.getOrDefault(name, "Đang xử lý...");
    }
}
