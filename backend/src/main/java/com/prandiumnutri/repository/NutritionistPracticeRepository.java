package com.prandiumnutri.repository;

import com.prandiumnutri.domain.NutritionistPractice;
import com.prandiumnutri.domain.PracticeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NutritionistPracticeRepository extends JpaRepository<NutritionistPractice, Long> {

    /** Practices do nutri, particular primeiro (company_id NULL vem antes por ORDER). */
    @Query("""
           select p from NutritionistPractice p
           where p.nutritionist.id = :nutriId
           order by case when p.company is null then 0 else 1 end, p.createdAt asc
           """)
    List<NutritionistPractice> findByNutritionistId(@Param("nutriId") Long nutriId);

    List<NutritionistPractice> findByNutritionistIdAndStatus(Long nutritionistId, PracticeStatus status);

    /** Perfil particular do nutri (unico), se existir. */
    @Query("""
           select p from NutritionistPractice p
           where p.nutritionist.id = :nutriId and p.company is null
           """)
    Optional<NutritionistPractice> findParticular(@Param("nutriId") Long nutriId);

    /** Practice do nutri vinculada a uma empresa especifica. */
    @Query("""
           select p from NutritionistPractice p
           where p.nutritionist.id = :nutriId and p.company.id = :companyId
           """)
    Optional<NutritionistPractice> findForCompany(@Param("nutriId") Long nutriId,
                                                   @Param("companyId") Long companyId);

    List<NutritionistPractice> findByCompanyIdAndStatus(Long companyId, PracticeStatus status);

    /**
     * Practices publicamente visiveis: ACTIVE, opt-in, dono ainda com role NUTRITIONIST e conta ativa.
     * Fase 5B: substitui `findAvailableNutris` do UserRepository, que retornava 1 linha por user.
     */
    @Query("""
           select p from NutritionistPractice p
             join p.nutritionist n
           where p.status = 'ACTIVE'
             and p.acceptsNewClients = true
             and n.role = 'NUTRITIONIST'
             and n.active = true
           order by n.name asc, case when p.company is null then 0 else 1 end
           """)
    List<NutritionistPractice> findAvailablePractices();
}
