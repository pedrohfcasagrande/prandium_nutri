package com.nutritracker.repository;

import com.nutritracker.domain.CompanyNutritionist;
import com.nutritracker.domain.LinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyNutritionistRepository extends JpaRepository<CompanyNutritionist, Long> {

    List<CompanyNutritionist> findByCompanyIdOrderByInvitedAtDesc(Long companyId);

    List<CompanyNutritionist> findByNutritionistIdOrderByInvitedAtDesc(Long nutritionistId);

    List<CompanyNutritionist> findByInvitedByEmailAndStatus(String email, LinkStatus status);

    Optional<CompanyNutritionist> findByInviteToken(String token);

    @Query("""
           select count(cn) > 0 from CompanyNutritionist cn
           where cn.company.id = :companyId
             and cn.nutritionist.id = :nutriId
             and cn.status = 'ACTIVE'
           """)
    boolean existsActive(@Param("companyId") Long companyId, @Param("nutriId") Long nutritionistId);

    @Query("""
           select cn.nutritionist.id from CompanyNutritionist cn
           where cn.company.id = :companyId and cn.status = 'ACTIVE' and cn.nutritionist is not null
           """)
    List<Long> activeNutritionistIds(@Param("companyId") Long companyId);
}
