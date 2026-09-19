package com.nutritracker.repository;

import com.nutritracker.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    long countByCreatedAtAfter(LocalDateTime createdAt);

    long countByLastSeenAtAfter(LocalDateTime lastSeenAfter);

    @Modifying
    @Transactional
    @Query("update User u set u.lastSeenAt = :now where u.id = :id")
    void updateLastSeenAt(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Query("""
           select u from User u
           where u.role = com.nutritracker.domain.Role.NUTRITIONIST
             and u.acceptsNewClients = true
             and u.active = true
           order by u.name asc
           """)
    java.util.List<User> findAvailableNutris();
}
