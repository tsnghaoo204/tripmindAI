package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyProposalRequest {

    @NotNull(message = "Proposal ID is required")
    private Long proposalId;
}
