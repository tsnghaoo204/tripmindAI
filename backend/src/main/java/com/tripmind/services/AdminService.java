package com.tripmind.services;

import com.tripmind.domains.responses.PageResponse;
import com.tripmind.domains.responses.ToolExecutionResponse;
import com.tripmind.entities.AiToolExecutionEntity;
import com.tripmind.enums.ToolExecutionStatus;
import com.tripmind.enums.TripPhase;
import com.tripmind.repositories.AiToolExecutionRepository;
import com.tripmind.domains.requests.AdminCreateUserRequest;
import com.tripmind.domains.requests.AdminUpdateUserRequest;
import com.tripmind.entities.UserEntity;
import com.tripmind.entities.TripEntity;
import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import com.tripmind.repositories.UserRepository;
import com.tripmind.repositories.TripRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * Màn quản trị — Giám sát và quản trị tài khoản, kế hoạch hệ thống.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AiToolExecutionRepository executionRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final PasswordEncoder passwordEncoder;
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
                SELECT t.id, t.name, t.user_id AS "userId", d.name AS "destinationName", d.country, t.start_date AS "startDate",
                       t.end_date AS "endDate", t.travelers, t.currency, t.phase_override AS "phase",
                       (SELECT COUNT(*) FROM activities a JOIN itinerary_days i ON i.id = a.itinerary_day_id
                         WHERE i.trip_id = t.id) AS "activityCount",
                       (SELECT COUNT(*) FROM conversations c WHERE c.trip_id = t.id) AS "conversationCount",
                       t.created_at AS "createdAt"
                FROM trips t JOIN destinations d ON d.id = t.destination_id
                ORDER BY t.id DESC LIMIT ? OFFSET ?
                """, size, (long) page * size);
        return new PageResponse<>(rows, page, size, total);
    }

    @Transactional
    public Map<String, Object> createUser(AdminCreateUserRequest req) {
        if (userRepository.existsByEmail(req.getEmail().trim().toLowerCase())) {
            throw new AppException(ErrorCode.CONFLICT, "Email already in use: " + req.getEmail());
        }
        UserEntity user = UserEntity.builder()
                .email(req.getEmail().trim().toLowerCase())
                .name(req.getName().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .isActive(true)
                .build();
        user = userRepository.save(user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", user.getId());
        res.put("email", user.getEmail());
        res.put("name", user.getName());
        res.put("role", user.getRole().name());
        res.put("active", user.isActive());
        res.put("createdAt", user.getCreatedAt());
        return res;
    }

    @Transactional
    public Map<String, Object> updateUser(Long userId, AdminUpdateUserRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found: " + userId));

        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName().trim());
        }
        if (req.getRole() != null) {
            user.setRole(req.getRole());
        }
        if (req.getActive() != null) {
            user.setActive(req.getActive());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        user = userRepository.save(user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", user.getId());
        res.put("email", user.getEmail());
        res.put("name", user.getName());
        res.put("role", user.getRole().name());
        res.put("active", user.isActive());
        res.put("updatedAt", user.getUpdatedAt());
        return res;
    }

    @Transactional
    public void deleteUser(Long currentAdminId, Long targetUserId) {
        if (currentAdminId != null && currentAdminId.equals(targetUserId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Cannot delete your own admin account");
        }
        UserEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found: " + targetUserId));
        userRepository.delete(user);
    }

    @Transactional
    public void deleteTrip(Long tripId) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Trip not found: " + tripId));
        tripRepository.delete(trip);
    }

    @Transactional
    public Map<String, Object> updateTrip(Long tripId, Map<String, Object> req) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Trip not found: " + tripId));
        if (req.containsKey("name") && req.get("name") != null && !req.get("name").toString().isBlank()) {
            trip.setName(req.get("name").toString().trim());
        }
        if (req.containsKey("phase")) {
            String p = (String) req.get("phase");
            trip.setPhaseOverride(p == null || p.isBlank() ? null : TripPhase.valueOf(p));
        }
        if (req.containsKey("budget") && req.get("budget") != null) {
            trip.setBudget(((Number) req.get("budget")).longValue());
        }
        trip = tripRepository.save(trip);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", trip.getId());
        res.put("name", trip.getName());
        res.put("phase", trip.getPhaseOverride() != null ? trip.getPhaseOverride().name() : null);
        return res;
    }

    private static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
