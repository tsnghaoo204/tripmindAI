package com.tripmind.domains.proposals;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.CostSource;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Một thao tác trong đề xuất, lưu ở {@code ai_proposals.changes_json}. Khai báo bằng sealed
 * interface để trình biên dịch bắt nhánh thiếu khi áp dụng, hoàn tác hay hiển thị.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "op")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ProposalChange.Add.class, name = "ADD"),
        @JsonSubTypes.Type(value = ProposalChange.Remove.class, name = "REMOVE"),
        @JsonSubTypes.Type(value = ProposalChange.Update.class, name = "UPDATE"),
        @JsonSubTypes.Type(value = ProposalChange.Reorder.class, name = "REORDER")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface ProposalChange {

    /** Lý do của riêng thao tác này, hiện ở thẻ so sánh. */
    String reason();

    /**
     * Ghi danh sách thao tác ra JSON kèm trường {@code op}. Phải khai kiểu phần tử: serialize
     * một {@code List} trơn thì Jackson không biết phần tử là {@code ProposalChange} và bỏ mất {@code op}.
     */
    static com.fasterxml.jackson.databind.JsonNode toJson(com.fasterxml.jackson.databind.ObjectMapper mapper,
                                                          java.util.List<ProposalChange> changes) {
        try {
            return mapper.readTree(mapper.writerFor(new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ProposalChange>>() {
            }).writeValueAsString(changes));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize proposal changes", e);
        }
    }

    /** Ảnh chụp gọn một hoạt động để hiện "trước / sau" và để dựng lại khi hoàn tác. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ActivitySnapshot(Long id, Integer dayNumber, String title, ActivityType activityType, LocalTime startTime,
                            LocalTime endTime, Long estimatedCost, String notes, String placeName) {
    }

    /** Địa điểm đã xác minh là có thật lúc dựng đề xuất (QĐ-08). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PlaceRef(String provider, String externalId, String name, BigDecimal lat, BigDecimal lng,
                    Integer priceLevel) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Add(int dayNumber, String title, ActivityType activityType, PlaceRef place, LocalTime startTime,
               LocalTime endTime, Long estimatedCost, CostSource costSource, String notes, String reason)
            implements ProposalChange {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Remove(long activityId, ActivitySnapshot before, String reason) implements ProposalChange {
    }

    /** Trường null thì giữ nguyên; {@code dayNumber} khác ngày hiện tại là chuyển hoạt động sang ngày đó. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Update(long activityId, Integer dayNumber, String title, LocalTime startTime, LocalTime endTime,
                  Long estimatedCost, String notes, ActivitySnapshot before, String reason) implements ProposalChange {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Reorder(int dayNumber, List<Long> activityIds, List<Long> beforeOrder, String reason)
            implements ProposalChange {
    }
}
