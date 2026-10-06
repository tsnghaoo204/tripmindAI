package com.tripmind.repositories;

import com.tripmind.entities.ExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {

    List<ExpenseEntity> findByTripIdOrderByExpenseDateDescCreatedAtDesc(Long tripId);

    @Query("SELECT e FROM ExpenseEntity e WHERE e.id = :id AND e.trip.user.id = :userId")
    Optional<ExpenseEntity> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByTripId(Long tripId);

    /** {@code [category, sum]} cho một chuyến. */
    @Query("SELECT e.category, SUM(e.amount) FROM ExpenseEntity e WHERE e.trip.id = :tripId GROUP BY e.category")
    List<Object[]> sumByCategory(@Param("tripId") Long tripId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseEntity e WHERE e.trip.id = :tripId AND e.expenseDate < :date")
    long sumBefore(@Param("tripId") Long tripId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseEntity e WHERE e.trip.id = :tripId AND e.expenseDate = :date")
    long sumOn(@Param("tripId") Long tripId, @Param("date") LocalDate date);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseEntity e
            WHERE e.trip.id = :tripId AND e.expenseDate >= :from AND e.expenseDate < :to
            """)
    long sumBetween(@Param("tripId") Long tripId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
