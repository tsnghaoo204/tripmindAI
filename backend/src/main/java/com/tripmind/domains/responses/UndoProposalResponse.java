package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ADS-30 §10a.2. Bản xem trước ({@code preview = true}) liệt kê <b>đúng</b> những việc sắp xảy
 * ra để hộp xác nhận nói thật (FR-1205); bản thật trả thêm lịch trình sau khi lùi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UndoProposalResponse {

    private Long proposalId;
    private boolean preview;
    private int reverted;
    /** Hoạt động AI thêm vào sẽ bị xoá. */
    private List<String> willDelete;
    /** Hoạt động AI đã xoá sẽ được dựng lại. */
    private List<String> willRestore;
    /** Hoạt động AI đã sửa / sắp lại sẽ quay về như trước. */
    private List<String> willRevert;
    /** Phần không lùi được và vì sao — ví dụ người dùng đã sửa tay sau khi áp dụng (FR-1203, FR-1204). */
    private List<Skipped> undoSkipped;
    /** Địa điểm đã ghi vào CSDL lúc áp dụng; hoàn tác lịch trình không rút lại (FR-1206). */
    private List<String> placesKept;
    private ItineraryResponse itinerary;

    public record Skipped(String title, String reason) {
    }
}
