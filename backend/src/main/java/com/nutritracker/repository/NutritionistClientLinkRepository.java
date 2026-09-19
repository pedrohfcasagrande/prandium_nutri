package com.nutritracker.repository;

import com.nutritracker.domain.LinkStatus;
import com.nutritracker.domain.NutritionistClientLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NutritionistClientLinkRepository extends JpaRepository<NutritionistClientLink, Long> {

    List<NutritionistClientLink> findByNutritionistIdOrderByInvitedAtDesc(Long nutritionistId);

    List<NutritionistClientLink> findByClientIdOrderByInvitedAtDesc(Long clientId);

    List<NutritionistClientLink> findByInvitedByEmailAndStatus(String email, LinkStatus status);

    Optional<NutritionistClientLink> findByInviteToken(String token);

    @Query("""
           select count(l) > 0 from NutritionistClientLink l
           where l.nutritionist.id = :nutriId
             and l.client.id = :clientId
             and l.status = 'ACTIVE'
           """)
    boolean existsActiveLink(@Param("nutriId") Long nutritionistId, @Param("clientId") Long clientId);

    @Query("""
           select count(l) from NutritionistClientLink l
           where l.nutritionist.id = :nutriId
             and l.status in ('PENDING', 'ACTIVE')
           """)
    long countActiveOrPending(@Param("nutriId") Long nutritionistId);

    @Query("""
           select l.nutritionist.id as nutriId, count(l) as total
           from NutritionistClientLink l
           where l.status = 'ACTIVE'
           group by l.nutritionist.id
           order by total desc
           """)
    List<NutriClientCount> topNutrisByActiveClients(org.springframework.data.domain.Pageable pageable);

    interface NutriClientCount {
        Long getNutriId();
        Long getTotal();
    }
}
