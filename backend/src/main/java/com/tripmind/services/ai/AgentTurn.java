package com.tripmind.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.time.Instant;
import java.util.*;

/**
 * Trạng thái của một lượt hỏi, truyền vào công cụ qua {@code ToolContext}.
 *
 * <p>{@code userId} và {@code tripId} lấy từ phiên đăng nhập và đường dẫn, <b>không</b> nằm trong
 * tham số công cụ mà mô hình nhìn thấy (QĐ-03, BR-501): mô hình có "xin" đọc chuyến số 42 thì
 * công cụ vẫn chỉ đọc chuyến của người đang hỏi.
 */
@Getter
public class AgentTurn {

    public static final String CONTEXT_KEY = "turn";

    private final Long userId;
    private final Long tripId;
    private final Long conversationId;
    private final Instant deadline;
    private final AgentEventSink sink;

    /** Mã các dòng {@code ai_tool_executions} của lượt này, theo thứ tự chạy. */
    private final List<Long> executionIds = new ArrayList<>();
    /** Kết quả đã có theo (tên công cụ, tham số) để chặn gọi lặp (BR-503). */
    private final Map<String, String> resultsByCall = new HashMap<>();
    private final List<Long> proposalIds = new ArrayList<>();
    /** Tệp đính kèm theo loại: "places", "checklist"... */
    private final Map<String, JsonNode> attachments = new LinkedHashMap<>();
    /** Mã ngoài của các địa điểm công cụ tìm kiếm đã trả trong lượt này. */
    private final Set<String> seenPlaceIds = new HashSet<>();

    public AgentTurn(Long userId, Long tripId, Long conversationId, Instant deadline, AgentEventSink sink) {
        this.userId = userId;
        this.tripId = tripId;
        this.conversationId = conversationId;
        this.deadline = deadline;
        this.sink = sink;
    }

    public void emit(String event, Object data) {
        sink.send(event, data);
    }
}
