package com.tripmind.entities;

import com.tripmind.enums.ChecklistCategory;
import com.tripmind.enums.ChecklistKind;
import com.tripmind.enums.ChecklistSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "trip_checklist_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private ChecklistKind kind;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ChecklistCategory category;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private ChecklistSource source = ChecklistSource.USER;

    @Column(length = 200)
    private String reason;

    @Column(name = "order_index", nullable = false)
    private short orderIndex;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
