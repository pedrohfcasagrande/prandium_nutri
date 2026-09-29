package com.prandiumnutri.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.PdfChangeRequestDto;
import com.prandiumnutri.dto.PdfCustomizationDto;
import com.prandiumnutri.dto.PdfTemplateFieldDto;
import com.prandiumnutri.repository.CompanyMemberRepository;
import com.prandiumnutri.repository.CompanyRepository;
import com.prandiumnutri.repository.NutritionistPracticeRepository;
import com.prandiumnutri.repository.PdfTemplateChangeRequestRepository;
import com.prandiumnutri.repository.PdfTemplateRepository;
import com.prandiumnutri.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fase 9H-3: fluxo de aprovacao de mudancas no template PDF.
 *
 * Fluxo:
 *  - Nutri vinculado (a empresa) chama {@link #create(User, PdfCustomizationDto, String)} pra sugerir.
 *  - Todos CompanyMembers ACTIVE recebem notificacao.
 *  - Admin da empresa chama {@link #approve} (aplica o estado proposto no template) ou {@link #reject}.
 *  - Nutri recebe notificacao de volta.
 */
@Service
@RequiredArgsConstructor
public class PdfTemplateChangeRequestService {

    private final PdfTemplateChangeRequestRepository repository;
    private final PdfTemplateRepository templateRepository;
    private final NutritionistPracticeRepository practiceRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final UserRepository userRepository;
    private final PdfTemplateService templateService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /** Nutri vinculado cria uma sugestao. Descobre a empresa via practice ativa/particular do proprio nutri. */
    @Transactional
    public PdfChangeRequestDto create(User nutri, Long practiceId,
                                       PdfCustomizationDto proposed, String note) {
        NutritionistPractice practice = practiceRepository.findById(practiceId)
                .orElseThrow(() -> new EntityNotFoundException("Practice nao encontrada"));
        if (practice.getNutritionist() == null || !practice.getNutritionist().getId().equals(nutri.getId())) {
            throw new AccessDeniedException("Practice pertence a outro nutricionista");
        }
        if (practice.getCompany() == null) {
            // Nutri particular nao precisa sugerir — edita direto.
            throw new IllegalStateException("Practice particular nao usa fluxo de sugestao.");
        }
        // O template alvo e o COMPANY. Garante que exista (empresa que ainda nao configurou).
        PdfTemplate target = templateService.getOrCreate(
                PdfTemplateOwnerType.COMPANY, practice.getCompany().getId());

        String json;
        try {
            json = objectMapper.writeValueAsString(proposed.fields());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar campos propostos", e);
        }

        PdfTemplateChangeRequest req = PdfTemplateChangeRequest.builder()
                .template(target)
                .requestedBy(nutri)
                .status(PdfChangeRequestStatus.PENDING)
                .proposedHeaderLine(blankToNull(proposed.pdfHeaderLine()))
                .proposedPrimaryColor(blankToNull(proposed.pdfPrimaryColor()))
                .proposedLogoBase64(blankToNull(proposed.pdfLogoBase64()))
                .proposedFields(json)
                .note(blankToNull(note))
                .build();
        req = repository.save(req);

        // Notifica todos CompanyMembers ACTIVE da empresa + o proprio owner
        notifyAdmins(practice.getCompany(),
                "Nova sugestao de mudanca no PDF",
                nutri.getName() + " propos mudancas no template da empresa.",
                req.getId());

        return toDto(req, target);
    }

    /** Admin lista requests da propria empresa (por status). */
    @Transactional(readOnly = true)
    public List<PdfChangeRequestDto> listForCompany(Long companyId, PdfChangeRequestStatus status) {
        PdfTemplate t = templateRepository.findByOwnerTypeAndOwnerId(PdfTemplateOwnerType.COMPANY, companyId)
                .orElse(null);
        if (t == null) return List.of();
        List<PdfTemplateChangeRequest> rows = status != null
                ? repository.findByTemplateIdAndStatusOrderByCreatedAtDesc(t.getId(), status)
                : repository.findByTemplateIdOrderByCreatedAtDesc(t.getId());
        return rows.stream().map(r -> toDto(r, t)).toList();
    }

    /** Nutri lista suas proprias sugestoes ativas (pra saber se ja tem uma PENDING). */
    @Transactional(readOnly = true)
    public List<PdfChangeRequestDto> listOwnPending(Long userId) {
        return repository.findByRequestedByIdAndStatusOrderByCreatedAtDesc(userId, PdfChangeRequestStatus.PENDING)
                .stream().map(r -> toDto(r, r.getTemplate())).toList();
    }

    /** Admin aprova: aplica o estado proposto no template + notifica nutri. */
    @Transactional
    public PdfChangeRequestDto approve(User reviewer, Long requestId, String reviewNote) {
        PdfTemplateChangeRequest req = fetchAsAdmin(reviewer, requestId);
        if (req.getStatus() != PdfChangeRequestStatus.PENDING) {
            throw new IllegalStateException("Solicitacao ja foi decidida.");
        }

        List<PdfTemplateFieldDto> fields;
        try {
            fields = objectMapper.readValue(req.getProposedFields(),
                    new TypeReference<List<PdfTemplateFieldDto>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar campos propostos", e);
        }

        PdfCustomizationDto dto = new PdfCustomizationDto(
                req.getProposedLogoBase64(),
                req.getProposedPrimaryColor(),
                req.getProposedHeaderLine(),
                fields);
        // Aplica no template alvo
        templateService.upsert(req.getTemplate().getOwnerType(), req.getTemplate().getOwnerId(), dto);

        req.setStatus(PdfChangeRequestStatus.APPROVED);
        req.setReviewedBy(reviewer);
        req.setReviewedAt(LocalDateTime.now());
        req.setReviewNote(blankToNull(reviewNote));
        req = repository.save(req);

        notificationService.notify(req.getRequestedBy().getId(), NotificationType.GENERIC,
                "Sua sugestao no PDF foi aprovada",
                reviewer.getName() + " aprovou sua sugestao. O template ja esta atualizado.",
                "PdfTemplateChangeRequest", req.getId());

        return toDto(req, req.getTemplate());
    }

    /** Admin rejeita: guarda motivo + notifica nutri. */
    @Transactional
    public PdfChangeRequestDto reject(User reviewer, Long requestId, String reviewNote) {
        PdfTemplateChangeRequest req = fetchAsAdmin(reviewer, requestId);
        if (req.getStatus() != PdfChangeRequestStatus.PENDING) {
            throw new IllegalStateException("Solicitacao ja foi decidida.");
        }
        req.setStatus(PdfChangeRequestStatus.REJECTED);
        req.setReviewedBy(reviewer);
        req.setReviewedAt(LocalDateTime.now());
        req.setReviewNote(blankToNull(reviewNote));
        req = repository.save(req);

        notificationService.notify(req.getRequestedBy().getId(), NotificationType.GENERIC,
                "Sua sugestao no PDF foi rejeitada",
                reviewer.getName() + " rejeitou sua sugestao"
                        + (req.getReviewNote() != null ? ": " + req.getReviewNote() : "."),
                "PdfTemplateChangeRequest", req.getId());

        return toDto(req, req.getTemplate());
    }

    // -------------------- helpers --------------------

    private PdfTemplateChangeRequest fetchAsAdmin(User reviewer, Long requestId) {
        PdfTemplateChangeRequest req = repository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitacao nao encontrada"));
        // Reviewer precisa ser owner ou CompanyMember ACTIVE da empresa dona do template.
        if (req.getTemplate().getOwnerType() != PdfTemplateOwnerType.COMPANY) {
            throw new AccessDeniedException("Somente templates de empresa tem fluxo de aprovacao");
        }
        Long companyId = req.getTemplate().getOwnerId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Empresa nao encontrada"));
        boolean isOwner = company.getOwner() != null && company.getOwner().getId().equals(reviewer.getId());
        boolean isMember = companyMemberRepository.findByUserIdAndStatus(reviewer.getId(), LinkStatus.ACTIVE)
                .stream().anyMatch(m -> m.getCompany() != null && m.getCompany().getId().equals(companyId));
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("Voce nao administra esta empresa");
        }
        return req;
    }

    private void notifyAdmins(Company company, String title, String body, Long entityId) {
        if (company.getOwner() != null) {
            notificationService.notify(company.getOwner().getId(), NotificationType.GENERIC,
                    title, body, "PdfTemplateChangeRequest", entityId);
        }
        companyMemberRepository.findByCompanyIdOrderByInvitedAtDesc(company.getId()).stream()
                .filter(m -> m.getStatus() == LinkStatus.ACTIVE && m.getUser() != null)
                // Nao duplica pro owner que ja foi notificado
                .filter(m -> company.getOwner() == null
                        || !m.getUser().getId().equals(company.getOwner().getId()))
                .forEach(m -> notificationService.notify(m.getUser().getId(),
                        NotificationType.GENERIC, title, body, "PdfTemplateChangeRequest", entityId));
    }

    private PdfChangeRequestDto toDto(PdfTemplateChangeRequest r, PdfTemplate templateHint) {
        List<PdfTemplateFieldDto> fields;
        try {
            fields = objectMapper.readValue(r.getProposedFields(),
                    new TypeReference<List<PdfTemplateFieldDto>>() {});
        } catch (JsonProcessingException e) {
            fields = List.of();
        }
        PdfCustomizationDto proposed = new PdfCustomizationDto(
                r.getProposedLogoBase64(), r.getProposedPrimaryColor(),
                r.getProposedHeaderLine(), fields);
        return new PdfChangeRequestDto(
                r.getId(),
                templateHint != null ? templateHint.getId() : null,
                r.getRequestedBy() != null ? r.getRequestedBy().getId() : null,
                r.getRequestedBy() != null ? r.getRequestedBy().getName() : null,
                r.getStatus(),
                proposed,
                r.getNote(),
                r.getReviewedBy() != null ? r.getReviewedBy().getId() : null,
                r.getReviewedBy() != null ? r.getReviewedBy().getName() : null,
                r.getReviewedAt(),
                r.getReviewNote(),
                r.getCreatedAt()
        );
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
