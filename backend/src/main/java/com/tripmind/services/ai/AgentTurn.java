package com.tripmind.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Trạng thái của một lượt hỏi, truyền vào công cụ qua {@code ToolContext}.
 *
 * <p>{@code userId} và {@code tripId} lấy từ phiên đăng nhập và đường dẫn, <b>không</b> nằm trong
 * tham số công cụ mà mô hình nhìn thấy (QĐ-03, BR-501): mô hình có "xin" đọc chuyến số 42 thì
 * công cụ vẫn chỉ đọc chuyến của người đang hỏi.
 *
 * <p>Công cụ chạy trên luồng khác và có thể còn chạy sau khi quá hạn, nên mọi tập hợp đều an toàn
 * đa luồng. Thao tác ghi (dựng đề xuất) phải qua {@link #beginWrite()}: lượt đã huỷ thì không ghi.
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
    private final List<Long> executionIds = new CopyOnWriteArrayList<>();
    /** Kết quả đã có theo (tên công cụ, tham số) để chặn gọi lặp (BR-503). */
    private final Map<String, String> resultsByCall = new ConcurrentHashMap<>();
    private final List<Long> proposalIds = new CopyOnWriteArrayList<>();
    /** Tệp đính kèm theo loại: "places", "checklist"... */
    private final Map<String, JsonNode> attachments = Collections.synchronizedMap(new LinkedHashMap<>());
    /** Mã ngoài của các địa điểm công cụ tìm kiếm đã trả trong lượt này. */
    private final Set<String> seenPlaceIds = ConcurrentHashMap.newKeySet();

    private boolean cancelled;
    private int writers;

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

    /** Bản sao tệp đính kèm theo thứ tự thêm vào. */
    public Map<String, JsonNode> attachmentsSnapshot() {
        synchronized (attachments) {
            return new LinkedHashMap<>(attachments);
        }
    }

    /** Bắt đầu một thao tác ghi; {@code false} nếu lượt đã bị huỷ (quá hạn). */
    public synchronized boolean beginWrite() {
        if (cancelled) {
            return false;
        }
        writers++;
        return true;
    }

    /** Gọi sau khi giao dịch của thao tác ghi kết thúc (commit hoặc rollback). */
    public synchronized void endWrite() {
        writers = Math.max(0, writers - 1);
    }

    /** Huỷ lượt nếu không có thao tác ghi đang dở; trả {@code false} khi đang ghi (không huỷ được). */
    public synchronized boolean cancel() {
        if (writers > 0) {
            return false;
        }
        cancelled = true;
        return true;
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }
}
