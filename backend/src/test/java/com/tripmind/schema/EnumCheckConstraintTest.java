package com.tripmind.schema;

import com.tripmind.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code ddl-auto: validate} chỉ kiểm tên cột và kiểu, không kiểm ràng buộc CHECK. Enum Java
 * lệch với CHECK trong migration thì chỉ vỡ lúc ghi thật, dưới dạng lỗi 500. Test này đọc
 * toàn bộ migration theo thứ tự và so tập giá trị của từng CHECK với enum tương ứng.
 */
class EnumCheckConstraintTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    private static final Pattern CHECK_IN = Pattern.compile(
            "CONSTRAINT\\s+(chk_\\w+)\\s+CHECK\\s*\\(\\s*(?:\\w+\\s+IS\\s+NULL\\s+OR\\s+)?\\w+\\s+IN\\s*\\(([^)]*)\\)\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    private static final Map<String, Class<? extends Enum<?>>> CONSTRAINT_TO_ENUM = Map.ofEntries(
            Map.entry("chk_users_role", UserRole.class),
            Map.entry("chk_user_preferences_travel_style", TravelStyle.class),
            Map.entry("chk_user_preferences_budget_pref", BudgetPreference.class),
            Map.entry("chk_places_adopted_via", PlaceAdoption.class),
            Map.entry("chk_activities_status", ActivityStatus.class),
            Map.entry("chk_activities_skip_reason", SkipReason.class),
            Map.entry("chk_activities_type", ActivityType.class),
            Map.entry("chk_activities_created_by", ActivityCreator.class),
            Map.entry("chk_expenses_source", ExpenseSource.class),
            Map.entry("chk_expenses_category", ExpenseCategory.class),
            Map.entry("chk_messages_role", MessageRole.class),
            Map.entry("chk_ai_tool_executions_status", ToolExecutionStatus.class),
            Map.entry("chk_ai_proposals_status", ProposalStatus.class),
            Map.entry("chk_ai_proposals_kind", ProposalKind.class),
            Map.entry("chk_trips_phase_override", TripPhase.class),
            Map.entry("chk_trips_travel_style", TravelStyle.class),
            Map.entry("chk_trips_budget_pref", BudgetPreference.class),
            Map.entry("chk_activities_cost_source", CostSource.class),
            Map.entry("chk_checklist_kind", ChecklistKind.class),
            Map.entry("chk_checklist_source", ChecklistSource.class),
            Map.entry("chk_checklist_category", ChecklistCategory.class),
            Map.entry("chk_place_ratings_verdict", PlaceVerdict.class));

    @Test
    @DisplayName("Mọi enum ánh xạ xuống cột có CHECK phải có đúng tập giá trị của CHECK đó")
    void enumsMatchCheckConstraints() throws IOException {
        Map<String, Set<String>> checks = readChecks();

        for (Map.Entry<String, Class<? extends Enum<?>>> entry : CONSTRAINT_TO_ENUM.entrySet()) {
            assertThat(checks).as("Khong tim thay rang buoc %s trong migration", entry.getKey())
                    .containsKey(entry.getKey());
            Set<String> enumValues = Arrays.stream(entry.getValue().getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(enumValues).as("%s lech voi %s", entry.getValue().getSimpleName(), entry.getKey())
                    .isEqualTo(checks.get(entry.getKey()));
        }
    }

    /** Ràng buộc bị DROP rồi ADD lại ở migration sau thì lấy bản cuối cùng. */
    private Map<String, Set<String>> readChecks() throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.list(MIGRATIONS)) {
            files = stream.filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
                    .sorted(Comparator.comparingInt(EnumCheckConstraintTest::version))
                    .toList();
        }
        Map<String, Set<String>> checks = new HashMap<>();
        for (Path file : files) {
            Matcher m = CHECK_IN.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (m.find()) {
                Set<String> values = Arrays.stream(m.group(2).split(","))
                        .map(v -> v.trim().replace("'", ""))
                        .collect(Collectors.toSet());
                checks.put(m.group(1).toLowerCase(), values);
            }
        }
        return checks;
    }

    private static int version(Path file) {
        String name = file.getFileName().toString();
        return Integer.parseInt(name.substring(1, name.indexOf("__")));
    }
}
