package com.tripmind.entities;

import com.tripmind.enums.ExpenseCategory;
import com.tripmind.enums.ExpenseSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Một khoản chi thực tế. Tách rời khỏi chi phí ước tính của hoạt động (BR-402). */
@Entity
@Table(name = "expenses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    /** Khoản chi gắn với một hoạt động (tuỳ chọn). Xoá hoạt động thì cột về NULL. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private ActivityEntity activity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExpenseCategory category;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    /** Người trả trong {@code trip_participants}; NULL khi chưa dùng tính năng chia tiền. */
    @Column(name = "paid_by")
    private Long paidBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "share_with", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<Long> shareWith = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    @Builder.Default
    private ExpenseSource source = ExpenseSource.FORM;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
