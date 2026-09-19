package com.nutritracker.service;

import com.nutritracker.domain.LinkStatus;
import com.nutritracker.domain.MenuPlan;
import com.nutritracker.domain.MenuPlanStatus;
import com.nutritracker.domain.NotificationType;
import com.nutritracker.domain.NutritionistClientLink;
import com.nutritracker.domain.User;
import com.nutritracker.dto.MenuPlanDto;
import com.nutritracker.repository.MenuPlanRepository;
import com.nutritracker.repository.NutritionistClientLinkRepository;
import com.nutritracker.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuPlanService {

    private final MenuPlanRepository repository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final NutritionistClientLinkRepository linkRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<MenuPlanDto> list(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MenuPlanDto activeFor(Long userId) {
        return repository.findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .map(this::toDto).orElse(null);
    }

    @Transactional
    public MenuPlanDto save(Long userId, MenuPlanDto dto) {
        boolean isNew = dto.id() == null;
        MenuPlan plan = dto.id() != null
                ? repository.findById(dto.id()).orElseThrow(() -> new EntityNotFoundException("MenuPlan not found"))
                : MenuPlan.builder().user(userService.getOrThrow(userId)).build();

        boolean hasActiveNutri = linkRepository.findByClientIdOrderByInvitedAtDesc(userId).stream()
                .anyMatch(l -> l.getStatus() == com.nutritracker.domain.LinkStatus.ACTIVE);
        boolean contentChanged = plan.getContent() != null && !plan.getContent().equals(dto.content());

        if (isNew) {
            // Novo cardapio: se tem nutri, comeca como DRAFT (precisa submeter); senao ja aprovado
            plan.setStatus(hasActiveNutri ? MenuPlanStatus.DRAFT : MenuPlanStatus.APPROVED);
        } else if (hasActiveNutri && contentChanged) {
            // Edicao de cardapio de user com nutri e o conteudo mudou -> volta para DRAFT (precisa reaprovar)
            plan.setStatus(MenuPlanStatus.DRAFT);
            plan.setActive(false);
            plan.setReviewedAt(null);
            plan.setReviewedById(null);
            plan.setReviewNote(null);
            plan.setSubmittedAt(null);
            plan.setSubmittedToNutriId(null);
        }

        plan.setName(dto.name());
        plan.setSource(dto.source());
        plan.setContent(dto.content());
        plan.setStartDate(dto.startDate());
        plan.setEndDate(dto.endDate());

        // Ativacao so permitida se APPROVED
        boolean wantsActive = dto.active() && plan.getStatus() == MenuPlanStatus.APPROVED;
        if (wantsActive) {
            repository.deactivateAllForUser(userId);
            plan.setActive(true);
        } else if (!dto.active()) {
            plan.setActive(false);
        }
        return toDto(repository.save(plan));
    }

    @Transactional
    public MenuPlanDto setActive(Long userId, Long id, boolean active) {
        MenuPlan plan = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuPlan not found"));
        if (!plan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Este cardapio nao pertence ao usuario");
        }
        if (active && plan.getStatus() != MenuPlanStatus.APPROVED) {
            throw new IllegalStateException("Cardapio precisa estar aprovado para ser ativado");
        }
        if (active) {
            repository.deactivateAllForUser(userId);
        }
        plan.setActive(active);
        return toDto(repository.save(plan));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        MenuPlan plan = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuPlan not found"));
        if (!plan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Este cardapio nao pertence ao usuario");
        }
        repository.delete(plan);
    }

    // -------- Approval flow --------

    @Transactional
    public MenuPlanDto submit(Long userId, Long planId) {
        MenuPlan plan = repository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("MenuPlan not found"));
        if (!plan.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Cardapio nao pertence ao usuario");
        }
        // Pega primeiro nutri ativo vinculado ao user
        NutritionistClientLink link = linkRepository.findByClientIdOrderByInvitedAtDesc(userId).stream()
                .filter(l -> l.getStatus() == LinkStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Voce nao tem nutricionista vinculado"));

        plan.setStatus(MenuPlanStatus.PENDING);
        plan.setSubmittedToNutriId(link.getNutritionist().getId());
        plan.setSubmittedAt(LocalDateTime.now());
        plan.setReviewNote(null);
        plan.setReviewedAt(null);
        plan.setReviewedById(null);
        plan.setActive(false);   // nao pode estar ativo enquanto aguarda
        plan = repository.save(plan);

        User client = plan.getUser();
        notificationService.notify(
                link.getNutritionist().getId(),
                NotificationType.MENU_SUBMITTED,
                "Novo cardapio para revisao",
                client.getName() + " enviou o cardapio \"" + plan.getName() + "\" para sua analise.",
                "MenuPlan", plan.getId()
        );
        return toDto(plan);
    }

    @Transactional
    public MenuPlanDto approve(User nutri, Long planId) {
        MenuPlan plan = fetchAndAuthorizeReviewer(nutri, planId);
        plan.setStatus(MenuPlanStatus.APPROVED);
        plan.setReviewedAt(LocalDateTime.now());
        plan.setReviewedById(nutri.getId());
        plan.setReviewNote(null);
        plan = repository.save(plan);

        notificationService.notify(
                plan.getUser().getId(),
                NotificationType.MENU_APPROVED,
                "Cardapio aprovado",
                "Seu cardapio \"" + plan.getName() + "\" foi aprovado por " + nutri.getName() + ".",
                "MenuPlan", plan.getId()
        );
        return toDto(plan);
    }

    @Transactional
    public MenuPlanDto reject(User nutri, Long planId, String reason) {
        MenuPlan plan = fetchAndAuthorizeReviewer(nutri, planId);
        plan.setStatus(MenuPlanStatus.REJECTED);
        plan.setReviewedAt(LocalDateTime.now());
        plan.setReviewedById(nutri.getId());
        plan.setReviewNote(reason);
        plan = repository.save(plan);

        notificationService.notify(
                plan.getUser().getId(),
                NotificationType.MENU_REJECTED,
                "Cardapio recusado",
                "Seu cardapio \"" + plan.getName() + "\" foi recusado por " + nutri.getName() +
                        ". Motivo: " + (reason == null ? "-" : reason),
                "MenuPlan", plan.getId()
        );
        return toDto(plan);
    }

    @Transactional(readOnly = true)
    public List<MenuPlanDto> listPendingForNutri(Long nutriId) {
        return repository.findBySubmittedToNutriIdAndStatusOrderBySubmittedAtDesc(nutriId, MenuPlanStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    private MenuPlan fetchAndAuthorizeReviewer(User nutri, Long planId) {
        MenuPlan plan = repository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("MenuPlan not found"));
        if (plan.getStatus() != MenuPlanStatus.PENDING) {
            throw new IllegalStateException("Cardapio nao esta pendente de aprovacao");
        }
        if (plan.getSubmittedToNutriId() == null || !plan.getSubmittedToNutriId().equals(nutri.getId())) {
            throw new AccessDeniedException("Este cardapio nao foi submetido a voce");
        }
        return plan;
    }

    private MenuPlanDto toDto(MenuPlan m) {
        User owner = m.getUser();
        return new MenuPlanDto(m.getId(), m.getName(), m.getSource(), m.getContent(),
                m.getStartDate(), m.getEndDate(), m.isActive(), m.getCreatedAt(),
                m.getStatus(), m.getSubmittedToNutriId(), m.getSubmittedAt(),
                m.getReviewedAt(), m.getReviewNote(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getName() : null,
                owner != null ? owner.getEmail() : null);
    }
}
