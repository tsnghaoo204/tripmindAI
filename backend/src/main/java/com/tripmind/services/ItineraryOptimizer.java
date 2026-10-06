package com.tripmind.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tối ưu thứ tự hoạt động trong một ngày để giảm quãng đường (FR-613). Tính bằng mã, không
 * nhờ mô hình (QĐ-09): láng giềng gần nhất rồi cải thiện bằng 2-opt.
 *
 * <p>Hoạt động <b>cố định</b> giữ nguyên vị trí: hoạt động đầu ngày (điểm xuất phát), hoạt
 * động có giờ bắt đầu, và hoạt động không có toạ độ. Chúng chia ngày thành các đoạn; mỗi đoạn
 * được tối ưu riêng với hai đầu mút giữ nguyên.
 */
@Component
@RequiredArgsConstructor
public class ItineraryOptimizer {

    private final DistanceService distanceService;

    public record Stop(long id, Double lat, Double lng, boolean fixed) {
        boolean hasCoordinates() {
            return lat != null && lng != null;
        }
    }

    public record Result(List<Long> order, long currentMeters, long optimizedMeters) {
        public double improvement() {
            return currentMeters == 0 ? 0 : (currentMeters - optimizedMeters) / (double) currentMeters;
        }
    }

    public Result optimize(List<Stop> stops) {
        List<Stop> current = List.copyOf(stops);
        if (current.size() < 3) {
            return new Result(ids(current), length(current), length(current));
        }
        List<Stop> result = new ArrayList<>(current);
        int segmentStart = 0;
        for (int i = 1; i <= current.size(); i++) {
            boolean boundary = i == current.size() || isAnchor(current.get(i), i);
            if (!boundary) {
                continue;
            }
            // Đoạn [segmentStart .. i): phần tử đầu là mỏ neo, các phần tử sau tự do.
            int end = i < current.size() ? i : -1;
            optimizeSegment(result, segmentStart, i, end);
            segmentStart = i;
        }
        return new Result(ids(result), length(current), length(result));
    }

    private boolean isAnchor(Stop stop, int index) {
        return index == 0 || stop.fixed() || !stop.hasCoordinates();
    }

    /**
     * Sắp lại {@code list[from+1 .. to)} giữa mỏ neo {@code list[from]} và (nếu có) {@code list[end]}.
     */
    private void optimizeSegment(List<Stop> list, int from, int to, int end) {
        if (to - from <= 2 || !list.get(from).hasCoordinates()) {
            return;
        }
        List<Stop> free = new ArrayList<>(list.subList(from + 1, to));
        List<Stop> route = new ArrayList<>();
        Stop cursor = list.get(from);
        while (!free.isEmpty()) {
            Stop nearest = free.get(0);
            for (Stop candidate : free) {
                if (distance(cursor, candidate) < distance(cursor, nearest)) {
                    nearest = candidate;
                }
            }
            route.add(nearest);
            free.remove(nearest);
            cursor = nearest;
        }

        List<Stop> path = new ArrayList<>();
        path.add(list.get(from));
        path.addAll(route);
        if (end >= 0) {
            path.add(list.get(end));
        }
        twoOpt(path, end >= 0);
        for (int k = 0; k < route.size(); k++) {
            list.set(from + 1 + k, path.get(1 + k));
        }
    }

    /** Đảo đoạn giữa nếu làm tổng quãng đường ngắn lại; hai đầu mút không đổi. */
    private void twoOpt(List<Stop> path, boolean fixedEnd) {
        boolean improved = true;
        int last = fixedEnd ? path.size() - 2 : path.size() - 1;
        while (improved) {
            improved = false;
            for (int i = 1; i < last; i++) {
                for (int j = i + 1; j <= last; j++) {
                    long before = length(path);
                    reverse(path, i, j);
                    if (length(path) < before) {
                        improved = true;
                    } else {
                        reverse(path, i, j);
                    }
                }
            }
        }
    }

    private void reverse(List<Stop> path, int i, int j) {
        while (i < j) {
            Stop tmp = path.get(i);
            path.set(i, path.get(j));
            path.set(j, tmp);
            i++;
            j--;
        }
    }

    /** Tổng quãng đường giữa các cặp liền kề đều có toạ độ. */
    public long length(List<Stop> stops) {
        long total = 0;
        for (int i = 0; i + 1 < stops.size(); i++) {
            total += distance(stops.get(i), stops.get(i + 1));
        }
        return total;
    }

    private long distance(Stop a, Stop b) {
        if (!a.hasCoordinates() || !b.hasCoordinates()) {
            return 0;
        }
        return distanceService.calculateDistanceMeters(a.lat(), a.lng(), b.lat(), b.lng());
    }

    private List<Long> ids(List<Stop> stops) {
        return stops.stream().map(Stop::id).toList();
    }
}
