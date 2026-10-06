package com.tripmind.services.ai;

import com.tripmind.domains.models.GroupProfile;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.DietaryRestriction;
import com.tripmind.services.TripClock;
import com.tripmind.services.ai.tools.PlaceTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GroupAwareToolsTest {

    private final GroupProfile vegetarianFamily =
            new GroupProfile(1, 0, Set.of(DietaryRestriction.VEGETARIAN), null, null);

    @Test
    @DisplayName("Tìm quán ăn khi nhóm có người ăn chay: thêm 'chay' vào truy vấn; tìm bảo tàng thì giữ nguyên")
    void dietaryKeywords() {
        assertThat(PlaceTools.withDietaryKeywords("quán ăn gần biển", vegetarianFamily)).isEqualTo("quán ăn gần biển chay");
        assertThat(PlaceTools.withDietaryKeywords("bảo tàng", vegetarianFamily)).isEqualTo("bảo tàng");
        assertThat(PlaceTools.withDietaryKeywords("quán chay", vegetarianFamily)).isEqualTo("quán chay");
        assertThat(PlaceTools.withDietaryKeywords("quán ăn", GroupProfile.empty())).isEqualTo("quán ăn");
    }

    @Test
    @DisplayName("Lời nhắc hệ thống tóm tắt nhóm đi và nhắc tránh lịch dày khi có trẻ nhỏ")
    void groupSummaryInPrompt() {
        SystemPromptBuilder builder = new SystemPromptBuilder(
                new ByteArrayResource("{groupSummary}".getBytes()), new TripClock(Clock.systemUTC()));
        TripEntity trip = TripEntity.builder().travelers((short) 4).groupProfile(vegetarianFamily)
                .destination(DestinationEntity.builder().timezone("Asia/Ho_Chi_Minh").build())
                .startDate(LocalDate.now()).endDate(LocalDate.now()).build();

        assertThat(builder.groupSummary(trip))
                .isEqualTo("Nhóm 4 người, 1 trẻ nhỏ, ăn chay. Tránh lịch dày và chặng đi bộ dài.");
    }
}
