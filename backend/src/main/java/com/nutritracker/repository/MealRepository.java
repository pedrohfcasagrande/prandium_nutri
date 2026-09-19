package com.nutritracker.repository;

import com.nutritracker.domain.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findByUserIdAndDateOrderByTimeAsc(Long userId, LocalDate date);
    List<Meal> findByUserIdOrderByDateDescTimeDesc(Long userId);

    @Query("select count(m) from Meal m where m.user.id in :userIds and m.date >= :since")
    long countByUserIdsSince(@Param("userIds") Collection<Long> userIds, @Param("since") LocalDate since);

    @Query("select count(distinct m.date) from Meal m where m.user.id = :userId and m.date >= :since")
    long countDistinctDatesSince(@Param("userId") Long userId, @Param("since") LocalDate since);

    @Query("select max(m.date) from Meal m where m.user.id = :userId")
    LocalDate lastMealDate(@Param("userId") Long userId);
}
