package com.tripmind.services.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.services.ai.AgentTurn;
import org.springframework.ai.chat.model.ToolContext;

/** Tiện ích chung của các bộ công cụ: lấy lượt hỏi hiện tại và viết kết quả ra JSON. */
final class ToolSupport {

    private ToolSupport() {
    }

    static AgentTurn turn(ToolContext context) {
        Object turn = context == null ? null : context.getContext().get(AgentTurn.CONTEXT_KEY);
        if (!(turn instanceof AgentTurn agentTurn)) {
            throw new IllegalStateException("Tool called outside of an agent turn");
        }
        return agentTurn;
    }

    static String json(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize tool result", e);
        }
    }
}
