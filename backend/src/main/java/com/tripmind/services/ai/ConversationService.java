package com.tripmind.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripmind.configurations.properties.AiProperties;
import com.tripmind.domains.responses.ConversationResponse;
import com.tripmind.domains.responses.MessageResponse;
import com.tripmind.domains.responses.ToolExecutionResponse;
import com.tripmind.entities.ConversationEntity;
import com.tripmind.entities.MessageEntity;
import com.tripmind.enums.MessageRole;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.AiToolExecutionRepository;
import com.tripmind.repositories.ConversationRepository;
import com.tripmind.repositories.MessageRepository;
import com.tripmind.services.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Lịch sử hội thoại (FR-607). Ngữ cảnh đưa lại cho mô hình chỉ gồm câu hỏi của người dùng và
 * câu trả lời cuối của trợ lý — không gồm các cặp tool call ↔ kết quả, nên không bao giờ cắt
 * lẻ một cặp. Kết quả công cụ đầy đủ vẫn nằm ở {@code ai_tool_executions}.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int TITLE_MAX = 60;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AiToolExecutionRepository executionRepository;
    private final TripService tripService;
    private final AiProperties properties;

    @Transactional
    public ConversationEntity openOrCreate(Long userId, Long tripId, Long conversationId, String firstMessage) {
        if (conversationId != null) {
            return requireConversation(userId, tripId, conversationId);
        }
        String title = firstMessage.strip();
        return conversationRepository.save(ConversationEntity.builder()
                .userId(userId)
                .tripId(tripId)
                .title(title.length() <= TITLE_MAX ? title : title.substring(0, TITLE_MAX) + "…")
                .build());
    }

    /** Hội thoại phải thuộc đúng người và đúng chuyến trong đường dẫn, ngược lại {@code 404}. */
    @Transactional(readOnly = true)
    public ConversationEntity requireConversation(Long userId, Long tripId, Long conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .filter(c -> tripId == null || c.getTripId().equals(tripId))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Conversation not found: " + conversationId));
    }

    @Transactional
    public MessageEntity saveUserMessage(Long conversationId, String content) {
        return messageRepository.save(MessageEntity.builder()
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(content)
                .build());
    }

    @Transactional
    public MessageEntity saveAssistantMessage(Long conversationId, String content, JsonNode usage, JsonNode attachments,
                                              List<Long> executionIds) {
        MessageEntity saved = messageRepository.save(MessageEntity.builder()
                .conversationId(conversationId)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .tokenUsage(usage)
                .attachments(attachments)
                .build());
        if (!executionIds.isEmpty()) {
            executionRepository.linkToMessage(executionIds, saved.getId());
        }
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.setUpdatedAt(java.time.Instant.now());
            conversationRepository.save(c);
        });
        return saved;
    }

    /** {@code historyMessages} tin gần nhất trước tin {@code beforeMessageId}, cũ trước, bắt đầu bằng câu hỏi. */
    @Transactional(readOnly = true)
    public List<Message> history(Long conversationId, Long beforeMessageId) {
        if (properties.historyMessages() == 0) {
            return List.of();
        }
        List<MessageEntity> recent = new ArrayList<>(messageRepository.findByConversationIdAndRoleInOrderByIdDesc(
                conversationId, List.of(MessageRole.USER, MessageRole.ASSISTANT),
                PageRequest.of(0, properties.historyMessages() + 1)));
        recent.removeIf(m -> Objects.equals(m.getId(), beforeMessageId) || m.getContent() == null);
        Collections.reverse(recent);
        while (!recent.isEmpty() && recent.get(0).getRole() != MessageRole.USER) {
            recent.remove(0);
        }
        List<Message> messages = new ArrayList<>();
        for (MessageEntity m : recent) {
            messages.add(m.getRole() == MessageRole.USER ? new UserMessage(m.getContent()) : new AssistantMessage(m.getContent()));
        }
        return messages;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(Long userId, Long tripId) {
        tripService.getOwnedTrip(userId, tripId);
        return conversationRepository.findByTripIdAndUserIdOrderByUpdatedAtDesc(tripId, userId).stream()
                .map(c -> ConversationResponse.builder()
                        .id(c.getId())
                        .tripId(c.getTripId())
                        .title(c.getTitle())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> messages(Long userId, Long conversationId) {
        requireConversation(userId, null, conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream()
                .filter(m -> m.getRole() == MessageRole.USER || m.getRole() == MessageRole.ASSISTANT)
                .map(m -> MessageResponse.builder()
                        .id(m.getId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .attachments(m.getAttachments())
                        .tokenUsage(m.getTokenUsage())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();
    }

    /** Nhật ký chạy công cụ của một chuyến, mới trước. */
    @Transactional(readOnly = true)
    public List<ToolExecutionResponse> activity(Long userId, Long tripId, int limit) {
        tripService.getOwnedTrip(userId, tripId);
        return executionRepository.findByTripIdAndUserIdOrderByIdDesc(tripId, userId, PageRequest.of(0, limit)).stream()
                .map(e -> ToolExecutionResponse.fromEntity(e, true))
                .toList();
    }
}
