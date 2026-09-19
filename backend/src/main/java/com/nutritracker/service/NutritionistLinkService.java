package com.nutritracker.service;

import com.nutritracker.domain.AuditAction;
import com.nutritracker.domain.LinkStatus;
import com.nutritracker.domain.NotificationType;
import com.nutritracker.domain.NutritionistClientLink;
import com.nutritracker.domain.Role;
import com.nutritracker.domain.User;
import com.nutritracker.dto.ClientLinkDto;
import com.nutritracker.dto.NutritionistLinkDto;
import com.nutritracker.dto.PendingInviteDto;
import com.nutritracker.repository.NutritionistClientLinkRepository;
import com.nutritracker.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NutritionistLinkService {

    private final NutritionistClientLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final FeatureGateService featureGate;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Value("${nutritracker.invite.expiration-days:14}")
    private int expirationDays;

    @Value("${nutritracker.reset.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    // --- Nutricionista ---

    @Transactional(readOnly = true)
    public List<ClientLinkDto> listClientsForNutri(Long nutritionistId) {
        return linkRepository.findByNutritionistIdOrderByInvitedAtDesc(nutritionistId).stream()
                .map(this::toClientLinkDto).toList();
    }

    @Transactional
    public ClientLinkDto invite(User nutritionist, String clientEmail) {
        if (nutritionist.getRole() != Role.NUTRITIONIST) {
            throw new AccessDeniedException("Apenas nutricionistas podem convidar clientes");
        }
        featureGate.assertCanAddClient(nutritionist.getId());

        String normalizedEmail = clientEmail.trim().toLowerCase();
        User existingClient = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (existingClient != null && existingClient.getId().equals(nutritionist.getId())) {
            throw new IllegalArgumentException("Voce nao pode se convidar como cliente");
        }

        if (existingClient != null
                && linkRepository.existsActiveLink(nutritionist.getId(), existingClient.getId())) {
            throw new IllegalArgumentException("Este cliente ja esta vinculado a voce");
        }

        String token = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        NutritionistClientLink link = NutritionistClientLink.builder()
                .nutritionist(nutritionist)
                .client(existingClient)
                .invitedByEmail(normalizedEmail)
                .inviteToken(token)
                .status(LinkStatus.PENDING)
                .invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .build();
        link = linkRepository.save(link);

        String link_url = frontendUrl + "/invite?token=" + token;
        String body = """
                Ola,

                A nutricionista %s (%s) convidou voce para acompanhar sua alimentacao no Nutri Tracker.

                Para aceitar ou recusar o convite, clique no link abaixo (valido por %d dias):

                %s

                Se voce nao tem uma conta ainda, sera guiado para criar uma antes de aceitar.

                Nutri Tracker
                """.formatted(nutritionist.getName(), nutritionist.getEmail(), expirationDays, link_url);
        mailService.send(normalizedEmail, "Convite de nutricionista - Nutri Tracker", body);

        auditService.record(AuditAction.CLIENT_INVITED, nutritionist.getId(), nutritionist.getRole(),
                "NutritionistClientLink", link.getId());
        return toClientLinkDto(link);
    }

    @Transactional
    public void revokeByNutri(User nutritionist, Long linkId) {
        NutritionistClientLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Vinculo nao encontrado"));
        if (!link.getNutritionist().getId().equals(nutritionist.getId())) {
            throw new AccessDeniedException("Voce nao pode revogar este vinculo");
        }
        link.setStatus(LinkStatus.REVOKED_BY_NUTRI);
        link.setRevokedAt(LocalDateTime.now());
        linkRepository.save(link);
    }

    // --- Cliente ---

    @Transactional(readOnly = true)
    public List<NutritionistLinkDto> listNutritionistsForClient(Long clientId) {
        return linkRepository.findByClientIdOrderByInvitedAtDesc(clientId).stream()
                .filter(l -> l.getStatus() == LinkStatus.ACTIVE)
                .map(this::toNutritionistLinkDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PendingInviteDto> pendingInvitesFor(User client) {
        // Convites pendentes: por email OU pelo client_id (se ja existia como user quando foi convidado)
        List<NutritionistClientLink> byEmail =
                linkRepository.findByInvitedByEmailAndStatus(client.getEmail().toLowerCase(), LinkStatus.PENDING);
        List<NutritionistClientLink> byClient = linkRepository.findByClientIdOrderByInvitedAtDesc(client.getId())
                .stream().filter(l -> l.getStatus() == LinkStatus.PENDING).toList();

        return java.util.stream.Stream.concat(byEmail.stream(), byClient.stream())
                .distinct()
                .filter(l -> LocalDateTime.now().isBefore(l.getExpiresAt()))
                .map(this::toPendingDto)
                .toList();
    }

    @Transactional
    public void acceptInvite(User client, String token) {
        NutritionistClientLink link = linkRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado"));
        validateInviteForClient(link, client);
        link.setClient(client);
        link.setStatus(LinkStatus.ACTIVE);
        link.setAcceptedAt(LocalDateTime.now());
        linkRepository.save(link);
        auditService.record(AuditAction.CLIENT_ACCEPTED, client.getId(), client.getRole(),
                "NutritionistClientLink", link.getId());
    }

    @Transactional
    public void rejectInvite(User client, String token) {
        NutritionistClientLink link = linkRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado"));
        validateInviteForClient(link, client);
        link.setStatus(LinkStatus.REJECTED);
        link.setRevokedAt(LocalDateTime.now());
        linkRepository.save(link);
    }

    @Transactional
    public void revokeByClient(User client, Long linkId) {
        NutritionistClientLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Vinculo nao encontrado"));
        if (link.getClient() == null || !link.getClient().getId().equals(client.getId())) {
            throw new AccessDeniedException("Voce nao pode revogar este vinculo");
        }
        link.setStatus(LinkStatus.REVOKED_BY_CLIENT);
        link.setRevokedAt(LocalDateTime.now());
        linkRepository.save(link);
    }

    private void validateInviteForClient(NutritionistClientLink link, User client) {
        if (link.getStatus() != LinkStatus.PENDING) {
            throw new IllegalArgumentException("Convite nao esta mais pendente");
        }
        if (LocalDateTime.now().isAfter(link.getExpiresAt())) {
            throw new IllegalArgumentException("Convite expirado");
        }
        boolean matchesEmail = link.getInvitedByEmail().equalsIgnoreCase(client.getEmail());
        boolean matchesClient = link.getClient() != null
                && link.getClient().getId().equals(client.getId());
        if (!matchesEmail && !matchesClient) {
            throw new AccessDeniedException("Este convite nao e seu");
        }
    }

    // --- 4D: pedidos iniciados pelo cliente ---

    @Transactional
    public ClientLinkDto requestLink(User client, Long nutritionistId, String message) {
        if (client.getRole() != Role.USER) {
            throw new AccessDeniedException("Somente clientes podem pedir vinculo");
        }
        featureGate.assertCanFindNutri(client.getId());
        User nutri = userRepository.findById(nutritionistId)
                .filter(u -> u.getRole() == Role.NUTRITIONIST && u.isActive())
                .orElseThrow(() -> new EntityNotFoundException("Nutricionista nao encontrado"));
        if (!nutri.isAcceptsNewClients()) {
            throw new IllegalStateException("Este nutricionista nao aceita novos clientes no momento");
        }
        if (linkRepository.existsActiveLink(nutri.getId(), client.getId())) {
            throw new IllegalArgumentException("Voce ja esta vinculado a este nutricionista");
        }

        String token = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        NutritionistClientLink link = NutritionistClientLink.builder()
                .nutritionist(nutri)
                .client(client)
                .invitedByEmail(client.getEmail().toLowerCase())
                .inviteToken(token)
                .status(LinkStatus.PENDING)
                .invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .initiatedByClient(true)
                .requestMessage(message)
                .build();
        link = linkRepository.save(link);

        notificationService.notify(
                nutri.getId(),
                NotificationType.GENERIC,
                "Novo pedido de cliente",
                client.getName() + " (" + client.getEmail() + ") quer se vincular a voce."
                        + (message != null && !message.isBlank() ? " Mensagem: " + message : ""),
                "NutritionistClientLink", link.getId()
        );
        auditService.record(AuditAction.CLIENT_INVITED, client.getId(), client.getRole(),
                "NutritionistClientLink", link.getId());
        return toClientLinkDto(link);
    }

    @Transactional
    public void acceptRequest(User nutritionist, Long linkId) {
        NutritionistClientLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Vinculo nao encontrado"));
        if (!link.getNutritionist().getId().equals(nutritionist.getId())) {
            throw new AccessDeniedException("Este vinculo nao e seu");
        }
        if (!link.isInitiatedByClient() || link.getStatus() != LinkStatus.PENDING) {
            throw new IllegalStateException("Pedido nao esta pendente");
        }
        link.setStatus(LinkStatus.ACTIVE);
        link.setAcceptedAt(LocalDateTime.now());
        linkRepository.save(link);
        notificationService.notify(
                link.getClient().getId(),
                NotificationType.GENERIC,
                "Nutricionista aceitou seu pedido",
                nutritionist.getName() + " aceitou acompanhar voce.",
                "NutritionistClientLink", link.getId()
        );
        auditService.record(AuditAction.CLIENT_ACCEPTED, nutritionist.getId(), nutritionist.getRole(),
                "NutritionistClientLink", link.getId());
    }

    @Transactional
    public void rejectRequest(User nutritionist, Long linkId, String note) {
        NutritionistClientLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Vinculo nao encontrado"));
        if (!link.getNutritionist().getId().equals(nutritionist.getId())) {
            throw new AccessDeniedException("Este vinculo nao e seu");
        }
        if (!link.isInitiatedByClient() || link.getStatus() != LinkStatus.PENDING) {
            throw new IllegalStateException("Pedido nao esta pendente");
        }
        link.setStatus(LinkStatus.REJECTED);
        link.setRevokedAt(LocalDateTime.now());
        link.setRejectNote(note);
        linkRepository.save(link);
        notificationService.notify(
                link.getClient().getId(),
                NotificationType.GENERIC,
                "Pedido recusado",
                nutritionist.getName() + " nao pode aceitar seu pedido no momento."
                        + (note != null && !note.isBlank() ? " Motivo: " + note : ""),
                "NutritionistClientLink", link.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<ClientLinkDto> listClientRequests(Long nutritionistId) {
        return linkRepository.findByNutritionistIdOrderByInvitedAtDesc(nutritionistId).stream()
                .filter(l -> l.isInitiatedByClient() && l.getStatus() == LinkStatus.PENDING)
                .map(this::toClientLinkDto).toList();
    }

    // --- Helper pra verificar vinculo (usado por CurrentUser em impersonation) ---
    public boolean hasActiveLink(Long nutritionistId, Long clientId) {
        return linkRepository.existsActiveLink(nutritionistId, clientId);
    }

    // --- Mappers ---

    private ClientLinkDto toClientLinkDto(NutritionistClientLink l) {
        User c = l.getClient();
        return new ClientLinkDto(
                l.getId(),
                c != null ? c.getId() : null,
                c != null ? c.getName() : "(pendente)",
                c != null ? c.getEmail() : l.getInvitedByEmail(),
                l.getStatus(), l.getInvitedAt(), l.getAcceptedAt(), l.getRevokedAt()
        );
    }

    private NutritionistLinkDto toNutritionistLinkDto(NutritionistClientLink l) {
        User n = l.getNutritionist();
        return new NutritionistLinkDto(l.getId(), n.getId(), n.getName(), n.getEmail(),
                l.getStatus(), l.getInvitedAt(), l.getAcceptedAt());
    }

    private PendingInviteDto toPendingDto(NutritionistClientLink l) {
        User n = l.getNutritionist();
        return new PendingInviteDto(l.getId(), l.getInviteToken(),
                n.getId(), n.getName(), n.getEmail(), l.getInvitedAt(), l.getExpiresAt());
    }
}
