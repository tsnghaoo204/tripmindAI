package com.tripmind.entities;

import com.tripmind.enums.ActivityCreator;
import com.tripmind.enums.ActivityStatus;
import com.tripmind.enums.ActivityType;
import com.tripmind.enums.CostSource;
import com.tripmind.enums.SkipReason;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "activities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    private ItineraryDayEntity itineraryDay;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private PlaceEntity place;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 16)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by", nullable = false, length = 8)
    @Builder.Default
    private ActivityCreator createdBy = ActivityCreator.USER;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /** NULL = chưa biết chi phí; khác với 0 = miễn phí. */
    @Column(name = "estimated_cost")
    private Long estimatedCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "estimated_cost_source", length = 12)
    private CostSource estimatedCostSource;

    @Column(name = "transportation_mode", length = 16)
    private String transportationMode;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "order_index", nullable = false)
    private short orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private ActivityStatus status = ActivityStatus.PLANNED;

    @Column(name = "actual_start")
    private LocalTime actualStart;

    @Column(name = "actual_end")
    private LocalTime actualEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "skip_reason", length = 12)
    private SkipReason skipReason;

    @Column(name = "from_proposal_id")
    private Long fromProposalId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
