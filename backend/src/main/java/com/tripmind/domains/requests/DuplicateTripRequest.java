package com.tripmind.domains.requests;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateTripRequest {

    /** Bỏ trống thì lấy tên chuyến cũ kèm "(bản sao)". */
    @Size(max = 160)
    private String name;

    /** Ngày đi của chuyến mới; số ngày giữ nguyên như chuyến cũ. */
    @NotNull(message = "startDate is required")
    @FutureOrPresent(message = "Start date must not be in the past")
    private LocalDate startDate;
}
