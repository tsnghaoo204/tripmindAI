package com.tripmind.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "places", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "external_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 24)
    private String provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(length = 64)
    private String category;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "user_ratings_total")
    @Builder.Default
    private Integer userRatingsTotal = 0;

    @Column(name = "price_level")
    private Short priceLevel;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "website_url", columnDefinition = "text")
    private String websiteUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours", columnDefinition = "jsonb")
    private JsonNode openingHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode reviews;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photo_urls", columnDefinition = "jsonb")
    private JsonNode photoUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "fetched_at", nullable = false)
    @Builder.Default
    private Instant fetchedAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
