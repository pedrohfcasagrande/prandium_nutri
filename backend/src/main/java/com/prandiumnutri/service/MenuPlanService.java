package com.prandiumnutri.service;

import com.prandiumnutri.domain.LinkStatus;
import com.prandiumnutri.domain.MenuPlan;
import com.prandiumnutri.domain.MenuPlanStatus;
import com.prandiumnutri.domain.NotificationType;
import com.prandiumnutri.domain.NutritionistClientLink;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.MenuPlanDto;
import com.prandiumnutri.domain.SuggestionScope;
import com.prandiumnutri.dto.SuggestionResponse;
import com.prandiumnutri.repository.MenuPlanRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.UserRepository;
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
    private final MenuSuggestionService suggestionService;

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
                .anyMatch(l -> l.getStatus() == com.prandiumnutri.domain.LinkStatus.ACTIVE);
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

    // -------- Nutritionist authoring (creates/edits menu-plans on behalf of client) --------

    /**
     * Verifica que o nutri tem vinculo ACTIVE com o cliente e retorna o link.
     * Se practiceId != null, exige tambem que o link seja da practice indicada.
     */
    private NutritionistClientLink authorizeNutriForClient(Long nutriId, Long clientId, Long practiceId) {
        return linkRepository.findByNutritionistIdOrderByInvitedAtDesc(nutriId).stream()
                .filter(l -> l.getStatus() == LinkStatus.ACTIVE
                          && l.getClient() != null && l.getClient().getId().equals(clientId)
                          && (practiceId == null
                              || (l.getPractice() != null && l.getPractice().getId().equals(practiceId))))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Voce nao atende este cliente"));
    }

    @Transactional(readOnly = true)
    public List<MenuPlanDto> listForClientByNutri(Long nutriId, Long clientId, Long practiceId) {
        authorizeNutriForClient(nutriId, clientId, practiceId);
        return repository.findByUserIdOrderByCreatedAtDesc(clientId).stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public MenuPlanDto createForClient(User nutri, Long clientId, Long practiceId, MenuPlanDto dto) {
        authorizeNutriForClient(nutri.getId(), clientId, practiceId);
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente nao encontrado"));
        MenuPlan plan = MenuPlan.builder()
                .user(client)
                .name(dto.name())
                .source(dto.source())
                .content(dto.content())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .status(MenuPlanStatus.APPROVED)
                .reviewedAt(LocalDateTime.now())
                .reviewedById(nutri.getId())
                .build();
        if (dto.active()) {
            repository.deactivateAllForUser(clientId);
            plan.setActive(true);
        }
        MenuPlan saved = repository.save(plan);
        notificationService.notify(
                client.getId(),
                NotificationType.GENERIC,
                "Novo cardapio de " + nutri.getName(),
                "Seu nutricionista " + nutri.getName() + " criou o cardapio \"" + saved.getName() + "\" para voce.",
                "MenuPlan", saved.getId()
        );
        return toDto(saved);
    }

    @Transactional
    public MenuPlanDto updateForClient(User nutri, Long planId, Long practiceId, MenuPlanDto dto) {
        MenuPlan plan = repository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("MenuPlan not found"));
        Long clientId = plan.getUser() != null ? plan.getUser().getId() : null;
        if (clientId == null) throw new EntityNotFoundException("Cardapio sem dono");
        authorizeNutriForClient(nutri.getId(), clientId, practiceId);

        plan.setName(dto.name());
        plan.setSource(dto.source());
        plan.setContent(dto.content());
        plan.setStartDate(dto.startDate());
        plan.setEndDate(dto.endDate());
        plan.setStatus(MenuPlanStatus.APPROVED);
        plan.setReviewedAt(LocalDateTime.now());
        plan.setReviewedById(nutri.getId());
        if (dto.active()) {
            repository.deactivateAllForUser(clientId);
            plan.setActive(true);
        } else {
            plan.setActive(false);
        }
        MenuPlan saved = repository.save(plan);
        notificationService.notify(
                clientId,
                NotificationType.GENERIC,
                "Cardapio atualizado por " + nutri.getName(),
                "Seu cardapio \"" + saved.getName() + "\" foi atualizado pelo seu nutricionista.",
                "MenuPlan", saved.getId()
        );
        return toDto(saved);
    }

    /**
     * Nutri gera um cardapio via IA para o cliente. Retorna texto pronto pra ser
     * colado no editor de cardapio; nao persiste nada. Autoriza vinculo ativo
     * naquela practice. O gate de plano (por practice) e aplicado pelo
     * MenuSuggestionService.
     */
    @Transactional
    public SuggestionResponse generateForClient(User nutri, Long clientId, Long practiceId,
                                                 String extraNotes, SuggestionScope scope) {
        authorizeNutriForClient(nutri.getId(), clientId, practiceId);
        SuggestionScope effective = scope == null ? SuggestionScope.WEEK : scope;
        return suggestionService.suggest(clientId, extraNotes, effective, practiceId);
    }

    @Transactional
    public void deleteForClient(User nutri, Long planId, Long practiceId) {
        MenuPlan plan = repository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("MenuPlan not found"));
        Long clientId = plan.getUser() != null ? plan.getUser().getId() : null;
        if (clientId == null) throw new EntityNotFoundException("Cardapio sem dono");
        authorizeNutriForClient(nutri.getId(), clientId, practiceId);
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
        return listPendingForNutri(nutriId, null);
    }

    @Transactional(readOnly = true)
    public List<MenuPlanDto> listPendingForNutri(Long nutriId, Long practiceId) {
        var pending = repository.findBySubmittedToNutriIdAndStatusOrderBySubmittedAtDesc(nutriId, MenuPlanStatus.PENDING);
        if (practiceId == null) {
            return pending.stream().map(this::toDto).toList();
        }
        // Filtra apenas os cardapios cujos clientes estao vinculados a esta practice do nutri
        return pending.stream().filter(plan -> {
            Long clientId = plan.getUser() != null ? plan.getUser().getId() : null;
            if (clientId == null) return false;
            return linkRepository.findByClientIdOrderByInvitedAtDesc(clientId).stream()
                    .anyMatch(l -> l.getNutritionist() != null
                              && l.getNutritionist().getId().equals(nutriId)
                              && l.getPractice() != null
                              && l.getPractice().getId().equals(practiceId));
        }).map(this::toDto).toList();
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
