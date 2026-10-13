package com.tripmind.services;

import com.tripmind.domains.requests.KnowledgeRequest;
import com.tripmind.domains.responses.KnowledgeResponse;
import com.tripmind.domains.responses.PageResponse;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.entities.DestinationKnowledgeEntity;
import com.tripmind.enums.KnowledgeCategory;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.DestinationKnowledgeRepository;
import com.tripmind.repositories.DestinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DestinationKnowledgeService {

    private final DestinationKnowledgeRepository knowledgeRepository;
    private final DestinationRepository destinationRepository;

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeResponse> findFiltered(String destination, KnowledgeCategory category, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DestinationKnowledgeEntity> result = knowledgeRepository.findFiltered(
                destination != null && !destination.isBlank() ? destination.trim() : null,
                category,
                search != null && !search.isBlank() ? search.trim() : null,
                pageable
        );
        return PageResponse.of(result.map(KnowledgeResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public KnowledgeResponse findById(Long id) {
        DestinationKnowledgeEntity entity = knowledgeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dữ liệu tri thức ID=" + id));
        return KnowledgeResponse.fromEntity(entity);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctDestinations() {
        return knowledgeRepository.findDistinctDestinationNames();
    }

    @Transactional
    public KnowledgeResponse create(KnowledgeRequest request) {
        DestinationEntity destination = null;
        if (request.getDestinationId() != null) {
            destination = destinationRepository.findById(request.getDestinationId()).orElse(null);
        }

        DestinationKnowledgeEntity entity = DestinationKnowledgeEntity.builder()
                .destination(destination)
                .destinationName(request.getDestinationName().trim())
                .category(request.getCategory())
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .placeName(request.getPlaceName() != null ? request.getPlaceName().trim() : null)
                .sourceName(request.getSourceName() != null ? request.getSourceName().trim() : null)
                .sourceUrl(request.getSourceUrl() != null ? request.getSourceUrl().trim() : null)
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .priority(request.getPriority())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        DestinationKnowledgeEntity saved = knowledgeRepository.save(entity);
        log.info("Admin created destination knowledge ID={} for destination='{}'", saved.getId(), saved.getDestinationName());
        return KnowledgeResponse.fromEntity(saved);
    }

    @Transactional
    public KnowledgeResponse update(Long id, KnowledgeRequest request) {
        DestinationKnowledgeEntity entity = knowledgeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dữ liệu tri thức ID=" + id));

        if (request.getDestinationId() != null) {
            DestinationEntity dest = destinationRepository.findById(request.getDestinationId()).orElse(null);
            entity.setDestination(dest);
        } else {
            entity.setDestination(null);
        }

        entity.setDestinationName(request.getDestinationName().trim());
        entity.setCategory(request.getCategory());
        entity.setTitle(request.getTitle().trim());
        entity.setContent(request.getContent().trim());
        entity.setPlaceName(request.getPlaceName() != null ? request.getPlaceName().trim() : null);
        entity.setSourceName(request.getSourceName() != null ? request.getSourceName().trim() : null);
        entity.setSourceUrl(request.getSourceUrl() != null ? request.getSourceUrl().trim() : null);
        if (request.getTags() != null) {
            entity.setTags(request.getTags());
        }
        entity.setPriority(request.getPriority());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }

        DestinationKnowledgeEntity saved = knowledgeRepository.save(entity);
        log.info("Admin updated destination knowledge ID={}", saved.getId());
        return KnowledgeResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!knowledgeRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dữ liệu tri thức ID=" + id);
        }
        knowledgeRepository.deleteById(id);
        log.info("Admin deleted destination knowledge ID={}", id);
    }

    @Transactional
    public KnowledgeResponse toggleActive(Long id) {
        DestinationKnowledgeEntity entity = knowledgeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dữ liệu tri thức ID=" + id));
        entity.setActive(!entity.isActive());
        DestinationKnowledgeEntity saved = knowledgeRepository.save(entity);
        log.info("Admin toggled active state for destination knowledge ID={} to {}", saved.getId(), saved.isActive());
        return KnowledgeResponse.fromEntity(saved);
    }

    /**
     * Lấy các mẹo/kinh nghiệm đã được chắt lọc (RAG) để nạp thẳng vào AI prompt.
     */
    @Transactional(readOnly = true)
    public String getFormattedKnowledgeForPrompt(String destinationName) {
        if (destinationName == null || destinationName.isBlank()) {
            return "";
        }

        List<DestinationKnowledgeEntity> items = knowledgeRepository.findActiveForAiPrompt(
                destinationName.trim(),
                PageRequest.of(0, 8)
        );

        if (items.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (DestinationKnowledgeEntity item : items) {
            sb.append("- [")
              .append(getCategoryDisplay(item.getCategory()))
              .append("] ")
              .append(item.getTitle())
              .append(": ")
              .append(item.getContent().replace("\n", " ").trim());

            if (item.getPlaceName() != null && !item.getPlaceName().isBlank()) {
                sb.append(" (Địa điểm: ").append(item.getPlaceName()).append(")");
            }
            if (item.getSourceName() != null && !item.getSourceName().isBlank()) {
                sb.append(" [Tham khảo: ").append(item.getSourceName()).append("]");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Phục vụ công cụ tra cứu tự động cho AI Agent trong lúc chat.
     */
    @Transactional(readOnly = true)
    public List<KnowledgeResponse> searchKnowledge(String destinationName, String keyword, int limit) {
        List<DestinationKnowledgeEntity> list = knowledgeRepository.searchActiveKnowledge(
                destinationName != null && !destinationName.isBlank() ? destinationName.trim() : null,
                keyword != null ? keyword.trim() : "",
                PageRequest.of(0, Math.min(limit, 10))
        );
        return list.stream().map(KnowledgeResponse::fromEntity).collect(Collectors.toList());
    }

    private String getCategoryDisplay(KnowledgeCategory category) {
        if (category == null) return "Mẹo";
        return switch (category) {
            case FOOD -> "Ăn uống";
            case ACCOMMODATION -> "Lưu trú";
            case TRANSPORT -> "Đi lại";
            case SIGHTSEEING -> "Tham quan";
            case TIP -> "Kinh nghiệm";
            case WARNING -> "Lưu ý quan trọng";
        };
    }
}
