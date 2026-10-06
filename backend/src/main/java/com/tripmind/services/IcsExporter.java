package com.tripmind.services;

import com.tripmind.domains.responses.ActivityResponse;
import com.tripmind.domains.responses.ItineraryDayResponse;
import com.tripmind.domains.responses.ItineraryResponse;
import com.tripmind.utils.MoneyFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Xuất lịch trình ra iCalendar (RFC 5545) để mở trong Google Calendar, Apple Calendar…
 *
 * <ul>
 *   <li>Hoạt động có giờ → một {@code VEVENT}, giờ địa phương quy đổi sang UTC theo múi giờ điểm
 *       đến nên không cần khối {@code VTIMEZONE}. Không có giờ kết thúc thì mặc định 1 giờ.</li>
 *   <li>Hoạt động không có giờ của cùng một ngày gom thành <b>một</b> sự kiện cả ngày.</li>
 *   <li>{@code UID} cố định theo mã hoạt động / ngày: nhập lại thì cập nhật, không nhân đôi.</li>
 *   <li>Dòng gập ở 75 byte UTF-8, không cắt giữa ký tự nhiều byte; thoát {@code \ ; ,} và xuống dòng.</li>
 * </ul>
 */
@Component
public class IcsExporter {

    private static final DateTimeFormatter UTC_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_LINE_BYTES = 75;
    private static final Duration DEFAULT_DURATION = Duration.ofHours(1);

    private final String uidDomain;
    private final Clock clock;

    public IcsExporter(@Value("${tripmind.export.ics-uid-domain:tripmind}") String uidDomain, Clock clock) {
        this.uidDomain = uidDomain;
        this.clock = clock;
    }

    public String export(ItineraryResponse itinerary, ZoneId zone) {
        String stamp = UTC_STAMP.format(clock.instant().atZone(ZoneOffset.UTC));
        List<String> lines = new ArrayList<>();
        lines.add("BEGIN:VCALENDAR");
        lines.add("VERSION:2.0");
        lines.add("PRODID:-//TripMind//Itinerary//VI");
        lines.add("CALSCALE:GREGORIAN");
        lines.add("METHOD:PUBLISH");
        lines.add("X-WR-CALNAME:" + escape(itinerary.getTripName()));

        for (ItineraryDayResponse day : itinerary.getDays()) {
            List<ActivityResponse> untimed = new ArrayList<>();
            for (ActivityResponse a : day.getActivities()) {
                if (a.getStartTime() == null) {
                    untimed.add(a);
                    continue;
                }
                ZonedDateTime start = ZonedDateTime.of(day.getDate(), a.getStartTime(), zone);
                ZonedDateTime end = a.getEndTime() != null && !a.getEndTime().isBefore(a.getStartTime())
                        ? ZonedDateTime.of(day.getDate(), a.getEndTime(), zone)
                        : start.plus(DEFAULT_DURATION);
                lines.add("BEGIN:VEVENT");
                lines.add("UID:activity-" + a.getId() + "@" + uidDomain);
                lines.add("DTSTAMP:" + stamp);
                lines.add("DTSTART:" + UTC_STAMP.format(start.withZoneSameInstant(ZoneOffset.UTC)));
                lines.add("DTEND:" + UTC_STAMP.format(end.withZoneSameInstant(ZoneOffset.UTC)));
                lines.add("SUMMARY:" + escape(a.getTitle()));
                addPlace(lines, a);
                String description = description(a, itinerary.getCurrency());
                if (!description.isEmpty()) {
                    lines.add("DESCRIPTION:" + escape(description));
                }
                lines.add("END:VEVENT");
            }
            if (!untimed.isEmpty()) {
                lines.add("BEGIN:VEVENT");
                lines.add("UID:day-" + day.getId() + "@" + uidDomain);
                lines.add("DTSTAMP:" + stamp);
                lines.add("DTSTART;VALUE=DATE:" + DATE.format(day.getDate()));
                lines.add("DTEND;VALUE=DATE:" + DATE.format(day.getDate().plusDays(1)));
                lines.add("SUMMARY:" + escape("Ngày " + day.getDayNumber() + " · "
                        + untimed.stream().map(ActivityResponse::getTitle).collect(Collectors.joining(" → "))));
                lines.add("DESCRIPTION:" + escape(untimed.stream()
                        .map(a -> "• " + a.getTitle() + (a.getPlace() == null ? "" : " — " + a.getPlace().getName()))
                        .collect(Collectors.joining("\n"))));
                lines.add("END:VEVENT");
            }
        }
        lines.add("END:VCALENDAR");
        return lines.stream().map(IcsExporter::fold).collect(Collectors.joining("\r\n")) + "\r\n";
    }

    private void addPlace(List<String> lines, ActivityResponse a) {
        if (a.getPlace() == null) {
            return;
        }
        String location = a.getPlace().getAddress() != null ? a.getPlace().getName() + ", " + a.getPlace().getAddress()
                : a.getPlace().getName();
        lines.add("LOCATION:" + escape(location));
        if (a.getPlace().getLatitude() != null && a.getPlace().getLongitude() != null) {
            lines.add("GEO:" + a.getPlace().getLatitude().toPlainString() + ";" + a.getPlace().getLongitude().toPlainString());
        }
    }

    private String description(ActivityResponse a, String currency) {
        List<String> parts = new ArrayList<>();
        if (a.getNotes() != null && !a.getNotes().isBlank()) {
            parts.add(a.getNotes().strip());
        }
        if (a.getEstimatedCost() != null) {
            parts.add("Chi phí ước tính: " + MoneyFormat.compact(a.getEstimatedCost(), currency));
        }
        return String.join("\n", parts);
    }

    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    /** RFC 5545 §3.1: dòng dài hơn 75 byte gập thành nhiều dòng, dòng tiếp bắt đầu bằng một dấu cách. */
    static String fold(String line) {
        if (line.getBytes(StandardCharsets.UTF_8).length <= MAX_LINE_BYTES) {
            return line;
        }
        StringBuilder out = new StringBuilder();
        int bytes = 0;
        int limit = MAX_LINE_BYTES;
        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            int size = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + size > limit) {
                out.append("\r\n ");
                bytes = 0;
                limit = MAX_LINE_BYTES - 1;
            }
            out.appendCodePoint(codePoint);
            bytes += size;
            i += Character.charCount(codePoint);
        }
        return out.toString();
    }
}
