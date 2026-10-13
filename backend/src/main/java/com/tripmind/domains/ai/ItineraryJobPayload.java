package com.tripmind.domains.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItineraryJobPayload {

    private String id;
    private Long userId;
    private Long tripId;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private String state = "QUEUED";

    @Builder.Default
    private String step = "Đang xếp hàng trong hệ thống...";

    private double progress;
    private Map<String, Object> result;
    private String error;

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("jobId", id);
        view.put("state", state);
        view.put("step", step);
        view.put("progress", progress);
        if (result != null) {
            view.put("result", result);
        }
        if (error != null) {
            view.put("error", error);
        }
        return view;
    }
}
