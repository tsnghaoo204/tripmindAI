package com.tripmind.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.configurations.properties.WeatherProperties;
import com.tripmind.domains.responses.TripWeatherResponse;
import com.tripmind.domains.responses.TripWeatherResponse.DayWeather;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.enums.WeatherSource;
import com.tripmind.exceptions.AppException;
import com.tripmind.services.clients.OpenMeteoClient;
import com.tripmind.services.clients.OpenMeteoClient.RawDay;
import com.tripmind.utils.WeatherCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Thời tiết từng ngày của chuyến (FR-501 → FR-505). Mỗi ngày rơi vào đúng một trong ba nguồn:
 *
 * <ul>
 *   <li>trong cửa sổ của API dự báo (hôm nay − 90 … hôm nay + 15) → {@code FORECAST}, hoặc
 *       {@code OBSERVED} với ngày đã qua;</li>
 *   <li>xa hơn trong tương lai → {@code CLIMATE_NORMAL}: trung bình cùng ngày của vài năm trước;</li>
 *   <li>xa hơn trong quá khứ → {@code OBSERVED} lấy từ API lưu trữ.</li>
 * </ul>
 *
 * Một nhóm lỗi thì các ngày của nhóm đó vắng mặt, các nhóm còn lại vẫn trả về.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    public static final String WEATHER_UNAVAILABLE = "WEATHER_UNAVAILABLE";
    public static final String PARTIAL = "PARTIAL";

    private static final double RAINY_DAY_MM = 1.0;

    private final TripService tripService;
    private final OpenMeteoClient openMeteoClient;
    private final WeatherProperties properties;
    private final TripClock tripClock;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /** Không bọc giao dịch: không giữ kết nối CSDL trong lúc chờ Open-Meteo. */
    public TripWeatherResponse getTripWeather(Long userId, Long tripId) {
        return forTrip(tripService.getOwnedTrip(userId, tripId));
    }

    /** Cho các dịch vụ đã kiểm sở hữu chuyến (công cụ AI, checklist). */
    public TripWeatherResponse forTrip(TripEntity trip) {
        DestinationEntity destination = trip.getDestination();
        ZoneId zone = tripClock.zoneOf(trip);
        LocalDate today = tripClock.today(trip);
        int expected = trip.lengthInDays();

        String cacheKey = "weather:%s:%s:%s:%s:%s".formatted(destination.getLatitude().toPlainString(),
                destination.getLongitude().toPlainString(), trip.getStartDate(), trip.getEndDate(), today);
        List<DayWeather> days = readCache(cacheKey)
                .orElseGet(() -> fetch(destination.getLatitude(), destination.getLongitude(), zone, today,
                        trip.getStartDate(), trip.getEndDate()));
        if (days.size() == expected) {
            writeCache(cacheKey, days);
        }
        for (DayWeather day : days) {
            day.setDayNumber((int) ChronoUnit.DAYS.between(trip.getStartDate(), day.getDate()) + 1);
        }

        return TripWeatherResponse.builder()
                .tripId(trip.getId())
                .timezone(zone.getId())
                .days(days)
                .reason(days.isEmpty() ? WEATHER_UNAVAILABLE : days.size() < expected ? PARTIAL : null)
                .build();
    }

    List<DayWeather> fetch(BigDecimal lat, BigDecimal lng, ZoneId zone, LocalDate today, LocalDate start, LocalDate end) {
        LocalDate forecastFrom = today.minusDays(properties.pastDays());
        LocalDate forecastTo = today.plusDays(properties.forecastDays() - 1L);
        List<DayWeather> result = new ArrayList<>();

        // 1. Quá khứ xa: số liệu đã ghi nhận từ API lưu trữ.
        LocalDate oldEnd = min(end, forecastFrom.minusDays(1));
        if (!start.isAfter(oldEnd)) {
            result.addAll(safely("archive", () -> openMeteoClient.archive(lat, lng, zone, start, oldEnd).stream()
                    .map(raw -> toDay(raw, WeatherSource.OBSERVED))
                    .toList()));
        }

        // 2. Cửa sổ dự báo.
        LocalDate fcStart = max(start, forecastFrom);
        LocalDate fcEnd = min(end, forecastTo);
        if (!fcStart.isAfter(fcEnd)) {
            result.addAll(safely("forecast", () -> openMeteoClient.forecast(lat, lng, zone, fcStart, fcEnd).stream()
                    .map(raw -> toDay(raw, raw.date().isBefore(today) ? WeatherSource.OBSERVED : WeatherSource.FORECAST))
                    .toList()));
        }

        // 3. Tương lai xa: trung bình khí hậu.
        LocalDate climateStart = max(start, forecastTo.plusDays(1));
        if (!climateStart.isAfter(end)) {
            result.addAll(safely("climate", () -> climateNormals(lat, lng, zone, today, climateStart, end)));
        }

        result.sort(Comparator.comparing(DayWeather::getDate));
        return result;
    }

    /**
     * Trung bình cùng ngày của {@code climateYears} năm gần nhất đã có số liệu lưu trữ.
     * Khả năng mưa = tỉ lệ số năm có mưa ≥ 1 mm vào ngày đó.
     */
    private List<DayWeather> climateNormals(BigDecimal lat, BigDecimal lng, ZoneId zone, LocalDate today,
                                            LocalDate from, LocalDate to) {
        Map<LocalDate, List<RawDay>> samples = new TreeMap<>();
        LocalDate archiveLimit = today.minusDays(properties.pastDays());
        int collected = 0;
        for (int yearsBack = 1; collected < properties.climateYears() && yearsBack <= properties.climateYears() + 2; yearsBack++) {
            LocalDate shiftedFrom = from.minusYears(yearsBack);
            LocalDate shiftedTo = to.minusYears(yearsBack);
            if (shiftedTo.isAfter(archiveLimit)) {
                continue;
            }
            int offset = yearsBack;
            for (RawDay raw : openMeteoClient.archive(lat, lng, zone, shiftedFrom, shiftedTo)) {
                samples.computeIfAbsent(raw.date().plusYears(offset), d -> new ArrayList<>()).add(raw);
            }
            collected++;
        }

        List<DayWeather> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<RawDay>> entry : samples.entrySet()) {
            if (entry.getKey().isBefore(from) || entry.getKey().isAfter(to)) {
                continue;
            }
            List<RawDay> raws = entry.getValue();
            Integer code = raws.stream().map(RawDay::weatherCode).filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            long rainy = raws.stream().filter(r -> r.precipitationMm() != null && r.precipitationMm() >= RAINY_DAY_MM).count();
            days.add(DayWeather.builder()
                    .date(entry.getKey())
                    .source(WeatherSource.CLIMATE_NORMAL)
                    .tempMin(average(raws, RawDay::tempMin))
                    .tempMax(average(raws, RawDay::tempMax))
                    .precipitationMm(average(raws, RawDay::precipitationMm))
                    .precipitationProbability((int) Math.round(rainy * 100.0 / raws.size()))
                    .weatherCode(code)
                    .summary(WeatherCodes.describe(code))
                    .build());
        }
        return days;
    }

    private DayWeather toDay(RawDay raw, WeatherSource source) {
        return DayWeather.builder()
                .date(raw.date())
                .source(source)
                .tempMin(raw.tempMin())
                .tempMax(raw.tempMax())
                .precipitationProbability(raw.precipitationProbability())
                .precipitationMm(raw.precipitationMm())
                .weatherCode(raw.weatherCode())
                .summary(WeatherCodes.describe(raw.weatherCode()))
                .build();
    }

    private List<DayWeather> safely(String part, java.util.function.Supplier<List<DayWeather>> call) {
        try {
            return call.get();
        } catch (AppException e) {
            log.warn("Bo qua phan thoi tiet '{}': {}", part, e.getMessage());
            return List.of();
        }
    }

    private Double average(List<RawDay> raws, Function<RawDay, Double> field) {
        OptionalDouble avg = raws.stream().map(field).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average();
        return avg.isPresent() ? BigDecimal.valueOf(avg.getAsDouble()).setScale(1, RoundingMode.HALF_UP).doubleValue() : null;
    }

    private Optional<List<DayWeather>> readCache(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, new TypeReference<>() {
            }));
        } catch (Exception e) {
            log.debug("Khong doc duoc bo dem thoi tiet: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void writeCache(String key, List<DayWeather> days) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(days), properties.cacheTtl());
        } catch (Exception e) {
            log.debug("Khong ghi duoc bo dem thoi tiet: {}", e.getMessage());
        }
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
