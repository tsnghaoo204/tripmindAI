package com.tripmind.repositories;

import com.tripmind.entities.DestinationKnowledgeEntity;
import com.tripmind.enums.KnowledgeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DestinationKnowledgeRepository extends JpaRepository<DestinationKnowledgeEntity, Long> {

    /**
     * Tri thức đang kích hoạt của một điểm đến: theo {@code destination_id}; dòng chưa gắn điểm đến
     * thì khớp đúng tên (không phân biệt hoa thường), không dùng {@code LIKE %...%}.
     */
    @Query("SELECT k FROM DestinationKnowledgeEntity k " +
           "WHERE k.active = true " +
           "  AND (k.destination.id = :destinationId " +
           "       OR (k.destination IS NULL AND LOWER(k.destinationName) = LOWER(:destinationName))) " +
           "ORDER BY k.priority DESC, k.createdAt DESC")
    List<DestinationKnowledgeEntity> findActiveForDestination(@Param("destinationId") Long destinationId,
                                                              @Param("destinationName") String destinationName,
                                                              Pageable pageable);

    /** Như trên khi chỉ biết tên điểm đến. */
    @Query("SELECT k FROM DestinationKnowledgeEntity k " +
           "WHERE k.active = true AND LOWER(k.destinationName) = LOWER(:destinationName) " +
           "ORDER BY k.priority DESC, k.createdAt DESC")
    List<DestinationKnowledgeEntity> findActiveByDestinationName(@Param("destinationName") String destinationName,
                                                                 Pageable pageable);

    /** Tra cứu theo từ khóa trong tri thức của một điểm đến (công cụ AI). */
    @Query("SELECT k FROM DestinationKnowledgeEntity k " +
           "WHERE k.active = true " +
           "  AND (k.destination.id = :destinationId " +
           "       OR (k.destination IS NULL AND LOWER(k.destinationName) = LOWER(:destinationName))) " +
           "  AND (LOWER(k.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "       OR LOWER(k.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "       OR (k.placeName IS NOT NULL AND LOWER(k.placeName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "ORDER BY k.priority DESC, k.createdAt DESC")
    List<DestinationKnowledgeEntity> searchActiveKnowledge(@Param("destinationId") Long destinationId,
                                                           @Param("destinationName") String destinationName,
                                                           @Param("keyword") String keyword,
                                                           Pageable pageable);

    /**
     * Tìm kiếm và lọc đa tiêu chí cho màn hình Admin.
     */
    @Query("SELECT k FROM DestinationKnowledgeEntity k " +
           "WHERE (:destination IS NULL OR :destination = '' OR LOWER(k.destinationName) = LOWER(:destination)) " +
           "  AND (:category IS NULL OR k.category = :category) " +
           "  AND (:search IS NULL OR :search = '' OR " +
           "       LOWER(k.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(k.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       (k.placeName IS NOT NULL AND LOWER(k.placeName) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY k.priority DESC, k.createdAt DESC")
    Page<DestinationKnowledgeEntity> findFiltered(
            @Param("destination") String destination,
            @Param("category") KnowledgeCategory category,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT DISTINCT k.destinationName FROM DestinationKnowledgeEntity k ORDER BY k.destinationName ASC")
    List<String> findDistinctDestinationNames();
}
