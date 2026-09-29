package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.PdfCustomizationDto;
import com.prandiumnutri.repository.PdfTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Fase 9H-1: resolucao de template PDF por dono (empresa OU practice particular).
 *
 * Regras:
 * - Practice vinculada a empresa (company_id != null) → template da empresa.
 * - Practice particular → template proprio.
 * - Ninguem configurou → retorna null (PrescriptionPdfService usa defaults Prandium).
 *
 * Nao usa FK real (owner_id logico) pra permitir owner_type polimorfico sem exigir
 * herança JPA — dois inteiros num par unique. Trade-off aceito porque a semantica
 * do dono e stateless (nunca deletamos template, so o dono).
 */
@Service
@RequiredArgsConstructor
public class PdfTemplateService {

    private final PdfTemplateRepository repository;

    /** Template efetivo pra usar no PDF de uma practice. Nao cria; retorna vazio se nao configurado. */
    @Transactional(readOnly = true)
    public Optional<PdfTemplate> resolveEffectiveFor(NutritionistPractice practice) {
        if (practice == null) return Optional.empty();
        if (practice.getCompany() != null) {
            return repository.findByOwnerTypeAndOwnerId(
                    PdfTemplateOwnerType.COMPANY, practice.getCompany().getId());
        }
        return repository.findByOwnerTypeAndOwnerId(
                PdfTemplateOwnerType.PRACTICE, practice.getId());
    }

    /** Get-or-create do template do dono indicado (usado quando o admin abre a tela de edicao). */
    @Transactional
    public PdfTemplate getOrCreate(PdfTemplateOwnerType type, Long ownerId) {
        return repository.findByOwnerTypeAndOwnerId(type, ownerId)
                .orElseGet(() -> repository.save(PdfTemplate.builder()
                        .ownerType(type).ownerId(ownerId).build()));
    }

    /** Atualiza template. Se nao existir, cria antes. */
    @Transactional
    public PdfTemplate upsert(PdfTemplateOwnerType type, Long ownerId, PdfCustomizationDto dto) {
        PdfTemplate t = getOrCreate(type, ownerId);
        if (dto.pdfLogoBase64() != null && dto.pdfLogoBase64().length() > 2_800_000) {
            throw new IllegalArgumentException("Logo muito grande (> 2MB)");
        }
        t.setPdfLogoBase64(blankToNull(dto.pdfLogoBase64()));
        t.setPdfPrimaryColor(blankToNull(dto.pdfPrimaryColor()));
        t.setPdfHeaderLine(blankToNull(dto.pdfHeaderLine()));
        t.setPdfFooterAddress(blankToNull(dto.pdfFooterAddress()));
        t.setPdfFooterPhone(blankToNull(dto.pdfFooterPhone()));
        t.setPdfFooterEmail(blankToNull(dto.pdfFooterEmail()));
        t.setPdfFooterWebsite(blankToNull(dto.pdfFooterWebsite()));
        t.setPdfFooterInstagram(blankToNull(dto.pdfFooterInstagram()));
        t.setPdfFooterNote(blankToNull(dto.pdfFooterNote()));
        return repository.save(t);
    }

    /** Le customizacao existente (sem criar). Retorna DTO vazio se nao houver template. */
    @Transactional(readOnly = true)
    public PdfCustomizationDto readOrEmpty(PdfTemplateOwnerType type, Long ownerId) {
        return repository.findByOwnerTypeAndOwnerId(type, ownerId)
                .map(this::toDto)
                .orElseGet(() -> new PdfCustomizationDto(null, null, null,
                        null, null, null, null, null, null));
    }

    public PdfCustomizationDto toDto(PdfTemplate t) {
        return new PdfCustomizationDto(
                t.getPdfLogoBase64(), t.getPdfPrimaryColor(),
                t.getPdfHeaderLine(),
                t.getPdfFooterAddress(), t.getPdfFooterPhone(),
                t.getPdfFooterEmail(), t.getPdfFooterWebsite(),
                t.getPdfFooterInstagram(), t.getPdfFooterNote());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
