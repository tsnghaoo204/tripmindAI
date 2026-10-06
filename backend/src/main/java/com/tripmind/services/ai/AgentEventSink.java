package com.tripmind.services.ai;

/** Nơi nhận sự kiện của một lượt hỏi: luồng SSE khi chạy thật, danh sách khi test. */
@FunctionalInterface
public interface AgentEventSink {

    void send(String event, Object data);
}
