package com.prandiumnutri.service;

import com.prandiumnutri.domain.Company;
import com.prandiumnutri.domain.NutritionistPractice;
import com.prandiumnutri.domain.PracticeStatus;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.repository.NutritionistPracticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gerencia os perfis de trabalho do nutricionista (Fase 5A).
 *
 * Regras basicas:
 * - Cada nutri pode ter no maximo 1 practice particular (company_id NULL).
 * - Cada nutri pode ter varias practices vinculadas a empresas (uma por empresa).
 * - Bio/specialties/acceptsNewClients passam a viver por practice.
 *
 * Nesta fase 5A o modelo e criado mas ainda nao e consumido pelas telas —
 * o objetivo e ter o backfill pronto e um caminho para as proximas fases.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NutritionistPracticeService {

    private final NutritionistPracticeRepository repository;

    @Transactional(readOnly = true)
    public List<NutritionistPractice> listByNutri(Long nutritionistId) {
        return repository.findByNutritionistId(nutritionistId);
    }

    /**
     * Garante que o nutri tenha um perfil particular; cria se nao houver.
     * Copia bio/specialties/acceptsNewClients do proprio User (compat com V015)
     * quando ainda nao existe practice — util no auto-cadastro.
     */
    @Transactional
    public NutritionistPractice ensureParticular(User nutri) {
        return repository.findParticular(nutri.getId())
                .orElseGet(() -> repository.save(NutritionistPractice.builder()
                        .nutritionist(nutri)
                        .company(null)
                        .bio(nutri.getBio())
                        .specialties(nutri.getSpecialties())
                        .acceptsNewClients(nutri.isAcceptsNewClients())
                        .status(PracticeStatus.ACTIVE)
                        .build()));
    }

    /**
     * Cria (ou reativa) a practice do nutri vinculada a uma empresa. Idempotente.
     */
    @Transactional
    public NutritionistPractice ensureForCompany(User nutri, Company company) {
        return repository.findForCompany(nutri.getId(), company.getId())
                .map(existing -> {
                    if (existing.getStatus() != PracticeStatus.ACTIVE) {
                        existing.setStatus(PracticeStatus.ACTIVE);
                    }
                    return existing;
                })
                .orElseGet(() -> repository.save(NutritionistPractice.builder()
                        .nutritionist(nutri)
                        .company(company)
                        .acceptsNewClients(false)
                        .status(PracticeStatus.ACTIVE)
                        .build()));
    }

    /**
     * Suspende a practice do nutri em uma empresa (chamado quando vinculo empresa-nutri encerra).
     * Nao remove — mantem historico e permite reativar se houver reconvite.
     */
    @Transactional
    public void suspendForCompany(Long nutritionistId, Long companyId) {
        repository.findForCompany(nutritionistId, companyId).ifPresent(p -> {
            p.setStatus(PracticeStatus.SUSPENDED);
        });
    }

    /**
     * Fase 5E: quando o vinculo empresa-nutri encerra e o nutri **nao tem** particular,
     * a practice vinculada e "convertida" em particular (company_id = null). Assim o nutri
     * nunca perde o acesso e mantem clientes/sponsorship. Se ja houver particular, apenas
     * suspende (rota normal). Idempotente.
     *
     * Retorna true se converteu; false se apenas suspendeu (ou nao encontrou).
     */
    @Transactional
    public boolean endLinkWithCompany(Long nutritionistId, Long companyId) {
        var opt = repository.findForCompany(nutritionistId, companyId);
        if (opt.isEmpty()) return false;
        NutritionistPractice p = opt.get();
        boolean hasParticular = repository.findParticular(nutritionistId).isPresent();
        if (!hasParticular) {
            // Converte esta em particular. Mantem status ACTIVE e vinculos NCL/subscriptions.
            p.setCompany(null);
            p.setStatus(PracticeStatus.ACTIVE);
            return true;
        }
        p.setStatus(PracticeStatus.SUSPENDED);
        return false;
    }

    /** Discovery: lista practices ACTIVE + opt-in publicamente visiveis. */
    @Transactional(readOnly = true)
    public List<NutritionistPractice> availablePractices() {
        return repository.findAvailablePractices();
    }

    @Transactional(readOnly = true)
    public NutritionistPractice getOrThrow(Long practiceId) {
        return repository.findById(practiceId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Practice not found: " + practiceId));
    }

    /** Fase 9D-2-fix: garante que a practice pertence ao nutri antes de expor. */
    @Transactional(readOnly = true)
    public NutritionistPractice getOwnedByNutri(Long nutritionistId, Long practiceId) {
        NutritionistPractice p = getOrThrow(practiceId);
        if (p.getNutritionist() == null || !p.getNutritionist().getId().equals(nutritionistId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Practice pertence a outro nutricionista");
        }
        return p;
    }

    // Fase 9H-1: updatePdfCustomization foi movido pra PdfTemplateService (template
    // agora vive numa tabela propria, com owner COMPANY ou PRACTICE).
}
