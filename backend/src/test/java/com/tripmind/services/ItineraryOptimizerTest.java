package com.tripmind.services;

import com.tripmind.services.ItineraryOptimizer.Stop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItineraryOptimizerTest {

    private final ItineraryOptimizer optimizer = new ItineraryOptimizer(new DistanceService());

    @Test
    @DisplayName("Lộ trình chạy lòng vòng được sắp lại ngắn hơn, điểm đầu ngày giữ nguyên")
    void shortensZigZag() {
        // Bốn điểm trên một đường thẳng, thứ tự hiện tại 0 → 3 → 1 → 2.
        List<Stop> stops = List.of(
                new Stop(1, 16.00, 108.00, false),
                new Stop(4, 16.03, 108.00, false),
                new Stop(2, 16.01, 108.00, false),
                new Stop(3, 16.02, 108.00, false));

        ItineraryOptimizer.Result result = optimizer.optimize(stops);

        assertThat(result.order()).containsExactly(1L, 2L, 3L, 4L);
        assertThat(result.optimizedMeters()).isLessThan(result.currentMeters());
        assertThat(result.improvement()).isGreaterThan(0.3);
    }

    @Test
    @DisplayName("Hoạt động có giờ cố định và hoạt động không có toạ độ giữ nguyên vị trí")
    void anchorsStayInPlace() {
        List<Stop> stops = List.of(
                new Stop(1, 16.00, 108.00, false),
                new Stop(2, 16.05, 108.00, false),
                new Stop(3, 16.01, 108.00, false),
                new Stop(9, 16.10, 108.00, true),
                new Stop(8, null, null, false),
                new Stop(5, 16.20, 108.00, false));

        List<Long> order = optimizer.optimize(stops).order();

        assertThat(order).containsExactly(1L, 3L, 2L, 9L, 8L, 5L);
    }
}
