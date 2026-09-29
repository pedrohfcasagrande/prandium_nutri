package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.PdfCustomizationDto;
import com.prandiumnutri.dto.PdfTemplateFieldDto;
import com.prandiumnutri.repository.PdfTemplateFieldRepository;
import com.prandiumnutri.repository.PdfTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fase 9H-1 + 9H-2: template PDF por dono (COMPANY ou PRACTICE) com campos modulares.
 */
@Service
@RequiredArgsConstructor
public class PdfTemplateService {

    private final PdfTemplateRepository repository;
    private final PdfTemplateFieldRepository fieldRepository;

    /** Template efetivo pra usar no PDF de uma practice. Nao cria; vazio se nao configurado. */
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

    @Transactional
    public PdfTemplate getOrCreate(PdfTemplateOwnerType type, Long ownerId) {
        return repository.findByOwnerTypeAndOwnerId(type, ownerId)
                .orElseGet(() -> repository.save(PdfTemplate.builder()
                        .ownerType(type).ownerId(ownerId).build()));
    }

    /** Fase 9H-2: fields ordenados por position asc do template. */
    @Transactional(readOnly = true)
    public List<PdfTemplateField> listFields(Long templateId) {
        return fieldRepository.findByTemplateIdOrderByPositionAsc(templateId);
    }

    /**
     * Upsert do template + sincronizacao dos fields. A lista do DTO substitui a atual:
     * ids ausentes viram delete; ids novos viram insert; ids existentes viram update com
     * novo label/type/value e novo position (indice no array).
     */
    @Transactional
    public PdfTemplate upsert(PdfTemplateOwnerType type, Long ownerId, PdfCustomizationDto dto) {
        PdfTemplate t = getOrCreate(type, ownerId);
        if (dto.pdfLogoBase64() != null && dto.pdfLogoBase64().length() > 2_800_000) {
            throw new IllegalArgumentException("Logo muito grande (> 2MB)");
        }
        t.setPdfLogoBase64(blankToNull(dto.pdfLogoBase64()));
        t.setPdfPrimaryColor(blankToNull(dto.pdfPrimaryColor()));
        t.setPdfHeaderLine(blankToNull(dto.pdfHeaderLine()));
        t = repository.save(t);

        syncFields(t, dto.fields());
        return t;
    }

    private void syncFields(PdfTemplate template, List<PdfTemplateFieldDto> incoming) {
        List<PdfTemplateField> current = fieldRepository.findByTemplateIdOrderByPositionAsc(template.getId());
        Map<Long, PdfTemplateField> byId = new HashMap<>();
        for (PdfTemplateField f : current) byId.put(f.getId(), f);

        // Deletar primeiro os que sumiram da lista (libera slots de position).
        List<Long> incomingIds = incoming.stream()
                .map(PdfTemplateFieldDto::id).filter(java.util.Objects::nonNull).toList();
        for (PdfTemplateField f : current) {
            if (!incomingIds.contains(f.getId())) fieldRepository.delete(f);
        }
        // Fase 1: reposiciona os que sobreviveram pra posicoes altas (fora do range dos novos),
        // pra evitar violacao do unique(template_id, position) durante reorder.
        for (PdfTemplateField f : current) {
            if (incomingIds.contains(f.getId())) {
                f.setPosition(10_000 + f.getPosition());
                fieldRepository.saveAndFlush(f);
            }
        }
        // Fase 2: applica novo estado (position = indice no array) — update existentes, insert novos.
        for (int i = 0; i < incoming.size(); i++) {
            PdfTemplateFieldDto in = incoming.get(i);
            if (in.type() == null || in.label() == null || in.label().isBlank()) continue;
            if (in.id() != null && byId.containsKey(in.id())) {
                PdfTemplateField existing = byId.get(in.id());
                existing.setLabel(in.label().trim());
                existing.setType(in.type());
                existing.setValue(blankToNull(in.value()));
                existing.setPosition(i);
                fieldRepository.saveAndFlush(existing);
            } else {
                fieldRepository.save(PdfTemplateField.builder()
                        .template(template)
                        .label(in.label().trim())
                        .type(in.type())
                        .value(blankToNull(in.value()))
                        .position(i)
                        .build());
            }
        }
    }

    /** Le customizacao existente (sem criar). Retorna DTO vazio se nao houver template. */
    @Transactional(readOnly = true)
    public PdfCustomizationDto readOrEmpty(PdfTemplateOwnerType type, Long ownerId) {
        return repository.findByOwnerTypeAndOwnerId(type, ownerId)
                .map(this::toDto)
                .orElseGet(() -> new PdfCustomizationDto(null, null, null, List.of()));
    }

    public PdfCustomizationDto toDto(PdfTemplate t) {
        List<PdfTemplateFieldDto> fields = fieldRepository.findByTemplateIdOrderByPositionAsc(t.getId())
                .stream()
                .map(f -> new PdfTemplateFieldDto(f.getId(), f.getLabel(), f.getType(), f.getValue()))
                .toList();
        return new PdfCustomizationDto(
                t.getPdfLogoBase64(), t.getPdfPrimaryColor(), t.getPdfHeaderLine(),
                fields);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
