package com.tripmind.entities;

import com.tripmind.enums.BudgetPreference;
import com.tripmind.enums.TravelStyle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Sở thích mặc định của tài khoản, dùng để điền sẵn khi tạo chuyến mới (FR-007). */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_style", length = 16)
    private TravelStyle travelStyle;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_preference", length = 16)
    private BudgetPreference budgetPreference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences_json", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> preferences = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
