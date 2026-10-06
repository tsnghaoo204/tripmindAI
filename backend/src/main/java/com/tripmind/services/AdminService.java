package com.tripmind.services;

import com.tripmind.domains.responses.PageResponse;
import com.tripmind.domains.responses.ToolExecutionResponse;
import com.tripmind.entities.AiToolExecutionEntity;
import com.tripmind.enums.ToolExecutionStatus;
import com.tripmind.repositories.AiToolExecutionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn quản trị — <b>chỉ đọc</b> (FR-903 → FR-906). Danh sách chuyến ở mức tổng hợp: không lộ
 * nội dung lịch trình, chi tiêu hay hội thoại của ai; quản trị không sửa được chuyến của người khác.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AiToolExecutionRepository executionRepository;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public PageResponse<ToolExecutionResponse> toolExecutions(Long userId, String tool, ToolExecutionStatus status,
                                                              LocalDate from, LocalDate to, int page, int size) {
        Specification<AiToolExecutionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (tool != null && !tool.isBlank()) {
                predicates.add(cb.equal(root.get("toolName"), tool.trim()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOf(from)));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), startOf(to.plusDays(1))));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.of(executionRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(e -> ToolExecutionResponse.fromEntity(e, true)));
    }

    /** Thống kê dùng AI trong khoảng ngày (mặc định 30 ngày gần nhất). */
    @Transactional(readOnly = true)
    public Map<String, Object> aiUsage(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(ZoneOffset.UTC) : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        Timestamp a = Timestamp.from(startOf(start));
        Timestamp b = Timestamp.from(startOf(end.plusDays(1)));

        Map<String, Object> turns = jdbc.queryForMap("""
                SELECT COUNT(*) AS turns,
                       COALESCE(SUM((token_usage->>'totalTokens')::bigint), 0) AS total_tokens,
                       COALESCE(SUM((token_usage->>'promptTokens')::bigint), 0) AS prompt_tokens,
                       COALESCE(SUM((token_usage->>'completionTokens')::bigint), 0) AS completion_tokens
                FROM messages WHERE role = 'ASSISTANT' AND created_at >= ? AND created_at < ?
                """, a, b);
        Map<String, Object> tools = jdbc.queryForMap("""
                SELECT COUNT(*) AS calls,
                       COUNT(*) FILTER (WHERE status <> 'OK') AS failures,
                       COALESCE(ROUND(AVG(execution_time_ms)), 0) AS avg_ms
                FROM ai_tool_executions WHERE created_at >= ? AND created_at < ?
                """, a, b);
        List<Map<String, Object>> topTools = jdbc.queryForList("""
                SELECT tool_name AS tool, COUNT(*) AS calls,
                       COUNT(*) FILTER (WHERE status <> 'OK') AS failures,
                       ROUND(AVG(execution_time_ms)) AS avg_ms
                FROM ai_tool_executions WHERE created_at >= ? AND created_at < ?
                GROUP BY tool_name ORDER BY calls DESC LIMIT 10
                """, a, b);
        List<Map<String, Object>> perDay = jdbc.queryForList("""
                SELECT CAST(created_at AT TIME ZONE 'UTC' AS date) AS day, COUNT(*) AS calls,
                       COUNT(*) FILTER (WHERE status <> 'OK') AS failures
                FROM ai_tool_executions WHERE created_at >= ? AND created_at < ?
                GROUP BY 1 ORDER BY 1
                """, a, b);

        long calls = ((Number) tools.get("calls")).longValue();
        long failures = ((Number) tools.get("failures")).longValue();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", start);
        result.put("to", end);
        result.put("assistantTurns", turns.get("turns"));
        result.put("totalTokens", turns.get("total_tokens"));
        result.put("promptTokens", turns.get("prompt_tokens"));
        result.put("completionTokens", turns.get("completion_tokens"));
        result.put("toolCalls", calls);
        result.put("toolErrorRate", calls == 0 ? 0.0 : (double) failures / calls);
        result.put("avgToolMs", tools.get("avg_ms"));
        result.put("topTools", topTools);
        result.put("perDay", perDay);
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> users(int page, int size) {
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.id, u.email, u.name, u.role, u.is_active AS active, u.created_at AS "createdAt",
                       (SELECT COUNT(*) FROM trips t WHERE t.user_id = u.id) AS "tripCount"
                FROM users u ORDER BY u.id DESC LIMIT ? OFFSET ?
                """, size, (long) page * size);
        return new PageResponse<>(rows, page, size, total);
    }

    /** Mức tổng hợp: số ngày, số hoạt động — không có tên hoạt động, chi tiêu hay hội thoại. */
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> trips(int page, int size) {
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM trips", Long.class);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT t.id, t.user_id AS "userId", d.name AS destination, d.country, t.start_date AS "startDate",
                       t.end_date AS "endDate", t.travelers, t.currency,
                       (SELECT COUNT(*) FROM activities a JOIN itinerary_days i ON i.id = a.itinerary_day_id
                         WHERE i.trip_id = t.id) AS "activityCount",
                       (SELECT COUNT(*) FROM conversations c WHERE c.trip_id = t.id) AS "conversationCount",
                       t.created_at AS "createdAt"
                FROM trips t JOIN destinations d ON d.id = t.destination_id
                ORDER BY t.id DESC LIMIT ? OFFSET ?
                """, size, (long) page * size);
        return new PageResponse<>(rows, page, size, total);
    }

    private static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
