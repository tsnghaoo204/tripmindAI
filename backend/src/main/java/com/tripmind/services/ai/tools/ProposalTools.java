package com.tripmind.services.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.services.DayOrderService;
import com.tripmind.services.ItineraryOptimizer;
import com.tripmind.services.ai.AgentToolSet;
import com.tripmind.services.ai.AgentTurn;
import com.tripmind.services.ai.ProposalService;
import com.tripmind.services.ai.ProposalService.ChangeInput;
import com.tripmind.services.ai.ProposalService.CreateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Công cụ ghi duy nhất cho lịch trình: dựng đề xuất. Không công cụ nào chạm được vào
 * {@code activities} (QĐ-01) — người dùng duyệt thì hệ thống mới ghi.
 */
@Component
@RequiredArgsConstructor
public class ProposalTools implements AgentToolSet {

    private final ProposalService proposalService;
    private final DayOrderService dayOrderService;
    private final ObjectMapper objectMapper;

    @Value("${tripmind.ai.optimize-min-improvement:0.05}")
    private double minImprovement;

    @Tool(name = "propose_itinerary_changes", description = """
            Create a proposal to change the itinerary (ADD, REMOVE, UPDATE, REORDER activities). It does NOT change
            anything: the user reviews the proposal and applies or rejects it. Places must come from search_places.
            Invalid changes are rejected with a reason; fix them and call again if needed.""")
    public String proposeItineraryChanges(
            @ToolParam(description = "One-line summary in Vietnamese, shown on the proposal card") String summary,
            @ToolParam(description = "Overall reason in Vietnamese: distance saved, best time, weather, rating...") String reason,
            @ToolParam(description = "The changes") List<ChangeInput> changes,
            ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        CreateResult result = proposalService.create(turn, summary, reason, changes);
        Map<String, Object> out = new LinkedHashMap<>();
        if (result.proposalId() == null) {
            out.put("error", "No valid change; nothing was proposed");
        } else {
            out.put("proposalId", result.proposalId());
            out.put("acceptedChanges", result.accepted());
            out.put("estimatedCostDelta", result.costDelta());
            out.put("travelTimeDeltaMinutes", result.travelTimeDelta());
            out.put("note", "Tell the user to review and apply the proposal; it is not applied yet.");
        }
        out.put("rejected", result.rejected());
        return ToolSupport.json(objectMapper, out);
    }

    @Tool(name = "optimize_day_order", description = """
            Compute (in code) a shorter visiting order for one day. Activities with a fixed start time, without
            coordinates, and the first activity of the day keep their position. Returns the suggested order;
            call propose_itinerary_changes with op=REORDER to propose it.""")
    public String optimizeDayOrder(@ToolParam(description = "Day number (1-based)") Integer dayNumber,
                                   ToolContext context) {
        AgentTurn turn = ToolSupport.turn(context);
        DayOrderService.DayOrder order = dayOrderService.optimize(turn.getUserId(), turn.getTripId(), dayNumber);
        ItineraryOptimizer.Result result = order.result();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dayNumber", dayNumber);
        out.put("currentOrder", order.currentOrder());
        out.put("currentMeters", result.currentMeters());
        out.put("optimizedMeters", result.optimizedMeters());
        out.put("improvementPercent", Math.round(result.improvement() * 1000) / 10.0);
        if (result.improvement() >= minImprovement) {
            out.put("suggestedOrder", result.order());
        } else {
            out.put("note", "Improvement below " + Math.round(minImprovement * 100) + "%: keep the current order");
        }
        return ToolSupport.json(objectMapper, out);
    }
}
