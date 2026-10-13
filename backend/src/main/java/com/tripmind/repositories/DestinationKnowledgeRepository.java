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
     * Lấy danh sách tri thức chất lượng nhất đang kích hoạt cho một điểm đến để nạp vào AI prompt.
     */
    @Query("SELECT k FROM DestinationKnowledgeEntity k " +
           "WHERE LOWER(k.destinationName) LIKE LOWER(CONCAT('%', :destinationName, '%')) " +
           "  AND k.active = true " +
           "ORDER BY k.priority DESC, k.createdAt DESC")
    List<DestinationKnowledgeEntity> findActiveForAiPrompt(@Param("destinationName") String destinationName, Pageable pageable);

    /**
     * Tra cứu theo từ khóa phục vụ công cụ tra cứu AI Tool.
     */
    @Query("SELECT k FROM DestinationKnowledgeEntity k " +
           "WHERE (:destinationName IS NULL OR LOWER(k.destinationName) LIKE LOWER(CONCAT('%', :destinationName, '%'))) " +
           "  AND k.active = true " +
           "  AND (LOWER(k.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "       OR LOWER(k.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "       OR (k.placeName IS NOT NULL AND LOWER(k.placeName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "ORDER BY k.priority DESC, k.createdAt DESC")
    List<DestinationKnowledgeEntity> searchActiveKnowledge(@Param("destinationName") String destinationName,
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
