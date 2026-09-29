package com.prandiumnutri.repository;

import com.prandiumnutri.domain.PdfTemplate;
import com.prandiumnutri.domain.PdfTemplateOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PdfTemplateRepository extends JpaRepository<PdfTemplate, Long> {
    Optional<PdfTemplate> findByOwnerTypeAndOwnerId(PdfTemplateOwnerType ownerType, Long ownerId);
}
