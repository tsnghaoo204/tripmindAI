package com.tripmind.services.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameMatchTest {

    @Test
    @DisplayName("So tên bỏ dấu, không phân biệt hoa thường; tên bịa không khớp")
    void nameMatch() {
        assertThat(ItineraryGenerationService.nameMatch("Bảo tàng Điêu khắc Chăm", "Bao tang Dieu khac Cham Da Nang"))
                .isEqualTo(1.0);
        assertThat(ItineraryGenerationService.nameMatch("Ngũ Hành Sơn", "Ngu Hanh Son - Marble Mountains")).isEqualTo(1.0);
        assertThat(ItineraryGenerationService.nameMatch("Lâu đài Pha Lê", "Bãi biển Mỹ Khê")).isZero();
    }
}
