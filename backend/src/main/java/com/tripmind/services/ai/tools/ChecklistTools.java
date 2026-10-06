package com.tripmind.services.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.domains.responses.ChecklistItemResponse;
import com.tripmind.domains.responses.ChecklistSuggestionsResponse;
import com.tripmind.enums.ChecklistCategory;
import com.tripmind.enums.ChecklistKind;
import com.tripmind.enums.ChecklistSource;
import com.tripmind.services.ChecklistService;
import com.tripmind.services.ai.AgentToolSet;
import com.tripmind.services.ai.AgentTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Gợi ý checklist trong hội thoại. Phần lõi là bộ luật bằng mã; mô hình được thêm tối đa vài
 * mục <b>có lý do</b> dựa trên lịch trình. Ứng viên đính vào tin nhắn, chỉ ghi vào checklist
 * khi người dùng chọn (D9).
 */
@Component
@RequiredArgsConstructor
public class ChecklistTools implements AgentToolSet {

    private static final int MAX_EXTRA = 5;

    private final ChecklistService checklistService;
    private final ObjectMapper objectMapper;

    public record ExtraItem(
            @ToolParam(description = "PACK (things to bring) or TODO (things to do before the trip)") String kind,
            @ToolParam(description = "Short item in Vietnamese") String title,
            @ToolParam(description = "Why it is needed, citing the itinerary or weather, in Vietnamese") String reason) {
    }

    @Tool(name = "suggest_checklist", description = """
            Suggest what to pack and what to do before the trip. Rule-based suggestions (weather, beach activities,
            flights, destination abroad, children/seniors) are included automatically; you may add up to 5 extra
            items, each with a concrete reason. Items are shown to the user to pick; nothing is saved.""")
    public String suggestChecklist(
            @ToolParam(description = "Extra items you recommend, each with a reason", required = false)
            List<ExtraItem> extras,
            ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        ChecklistSuggestionsResponse suggestions = checklistService.suggestions(turn.getUserId(), turn.getTripId());
        List<ChecklistItemResponse> items = new ArrayList<>(suggestions.getItems());
        Set<String> titles = new HashSet<>();
        items.forEach(i -> titles.add(i.getTitle().toLowerCase(Locale.ROOT)));
        int added = 0;
        for (ExtraItem extra : extras == null ? List.<ExtraItem>of() : extras) {
            if (added >= MAX_EXTRA || extra.title() == null || extra.title().isBlank() || extra.title().length() > 120
                    || extra.reason() == null || extra.reason().isBlank()
                    || !titles.add(extra.title().strip().toLowerCase(Locale.ROOT))) {
                continue;
            }
            ChecklistKind kind = "TODO".equalsIgnoreCase(extra.kind()) ? ChecklistKind.TODO : ChecklistKind.PACK;
            items.add(ChecklistItemResponse.builder()
                    .kind(kind)
                    .title(extra.title().strip())
                    .category(ChecklistCategory.OTHER)
                    .source(ChecklistSource.AI)
                    .reason(extra.reason().length() > 200 ? extra.reason().substring(0, 200) : extra.reason())
                    .build());
            added++;
        }
        if (!items.isEmpty()) {
            turn.getAttachments().put("checklist", objectMapper.valueToTree(items));
            turn.emit("checklist", Map.of("items", items));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("shown", items.size());
        out.put("items", items.stream().map(ChecklistItemResponse::getTitle).toList());
        if (suggestions.getSkippedRules() != null) {
            out.put("skippedRules", suggestions.getSkippedRules());
        }
        return ToolSupport.json(objectMapper, out);
    }
}
