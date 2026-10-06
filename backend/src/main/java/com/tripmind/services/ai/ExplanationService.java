package com.tripmind.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.domains.proposals.ProposalChange;
import com.tripmind.domains.responses.ExplanationResponse;
import com.tripmind.domains.responses.ToolExecutionResponse;
import com.tripmind.entities.AiProposalEntity;
import com.tripmind.entities.ActivityEntity;
import com.tripmind.enums.ActivityCreator;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.ActivityRepository;
import com.tripmind.repositories.AiProposalRepository;
import com.tripmind.repositories.AiToolExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Truy một hoạt động do AI tạo về đề xuất, công cụ đã chạy và ứng viên bị loại. */
@Service
@RequiredArgsConstructor
public class ExplanationService {

    private final ActivityRepository activityRepository;
    private final AiProposalRepository proposalRepository;
    private final AiToolExecutionRepository executionRepository;
    private final ProposalService proposalService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ExplanationResponse explain(Long userId, Long activityId) {
        ActivityEntity activity = activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Activity not found or access denied: " + activityId));
        ExplanationResponse.ExplanationResponseBuilder response = ExplanationResponse.builder()
                .activityId(activityId)
                .createdBy(activity.getCreatedBy());

        if (activity.getFromProposalId() == null) {
            return response.known(false)
                    .unknownReason(activity.getCreatedBy() == ActivityCreator.USER ? "CREATED_BY_USER" : "NO_SOURCE_RECORDED")
                    .build();
        }
        Optional<AiProposalEntity> found = proposalRepository.findById(activity.getFromProposalId());
        if (found.isEmpty()) {
            return response.known(false).unknownReason("PROPOSAL_DELETED").build();
        }
        AiProposalEntity proposal = found.get();
        JsonNode evidence = proposal.getEvidence();
        List<Long> executionIds = new ArrayList<>();
        if (evidence != null && evidence.has("toolExecutionIds")) {
            evidence.get("toolExecutionIds").forEach(id -> executionIds.add(id.asLong()));
        }
        List<ToolExecutionResponse> tools = executionIds.isEmpty() ? List.of()
                : executionRepository.findByIdInOrderByIdAsc(executionIds).stream()
                .map(e -> ToolExecutionResponse.fromEntity(e, true))
                .toList();

        return response.known(true)
                .proposalId(proposal.getId())
                .proposalSummary(proposal.getSummary())
                .proposalReason(evidence == null || !evidence.hasNonNull("reason") ? null : evidence.get("reason").asText())
                .changeReason(changeReason(proposal, activity))
                .appliedAt(proposal.getAppliedAt())
                .tools(tools)
                .rejected(evidence == null ? null : evidence.get("rejected"))
                .build();
    }

    /** Thao tác ADD nào đã sinh ra hoạt động này: tra trong nhật ký hoàn tác (mã hoạt động → chỉ số thao tác). */
    private String changeReason(AiProposalEntity proposal, ActivityEntity activity) {
        if (proposal.getUndo() == null) {
            return null;
        }
        ProposalApplyService.UndoLog undo = objectMapper.convertValue(proposal.getUndo(), ProposalApplyService.UndoLog.class);
        List<ProposalChange> changes = proposalService.changesOf(proposal);
        for (ProposalApplyService.UndoOp op : undo.ops()) {
            if (op instanceof ProposalApplyService.UndoOp.Created created && created.activityId() == activity.getId()
                    && created.changeIndex() < changes.size()) {
                return changes.get(created.changeIndex()).reason();
            }
        }
        return null;
    }
}
