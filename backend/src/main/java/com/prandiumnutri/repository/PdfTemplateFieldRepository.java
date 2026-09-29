package com.prandiumnutri.repository;

import com.prandiumnutri.domain.PdfTemplateField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PdfTemplateFieldRepository extends JpaRepository<PdfTemplateField, Long> {
    List<PdfTemplateField> findByTemplateIdOrderByPositionAsc(Long templateId);
    void deleteByTemplateId(Long templateId);
}
