package com.prandiumnutri.repository;

import com.prandiumnutri.domain.PdfChangeRequestStatus;
import com.prandiumnutri.domain.PdfTemplateChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PdfTemplateChangeRequestRepository extends JpaRepository<PdfTemplateChangeRequest, Long> {
    List<PdfTemplateChangeRequest> findByTemplateIdAndStatusOrderByCreatedAtDesc(
            Long templateId, PdfChangeRequestStatus status);

    List<PdfTemplateChangeRequest> findByTemplateIdOrderByCreatedAtDesc(Long templateId);

    List<PdfTemplateChangeRequest> findByRequestedByIdAndStatusOrderByCreatedAtDesc(
            Long userId, PdfChangeRequestStatus status);
}
