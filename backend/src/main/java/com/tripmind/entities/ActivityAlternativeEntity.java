package com.tripmind.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "activity_alternatives")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAlternativeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private ActivityEntity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private PlaceEntity place;

    @Column(name = "place_name", length = 200)
    private String placeName;

    @Column(nullable = false, length = 200)
    private String title;

    /** BUDGET, BALANCED, PREMIUM */
    @Column(nullable = false, length = 20)
    private String tier;

    @Column(name = "tier_label", length = 60)
    private String tierLabel;

    @Column(name = "estimated_cost")
    private Long estimatedCost;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "user_ratings_total")
    private Integer userRatingsTotal;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "photo_url", columnDefinition = "text")
    private String photoUrl;

    @Column(name = "is_selected", nullable = false)
    @Builder.Default
    private boolean isSelected = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
