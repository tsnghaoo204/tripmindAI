package com.tripmind.domains.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    private Long conversationId;

    @NotBlank(message = "Prompt message must not be blank")
    @Size(max = 4000, message = "Prompt message cannot exceed 4000 characters")
    private String message;
}
