package com.prandiumnutri.controller;

import com.prandiumnutri.dto.PdfVerificationDto;
import com.prandiumnutri.service.PrescriptionIssuanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fase 9D-3: verificacao publica de autenticidade de PDF de prescricao.
 * SEM autenticacao — qualquer um pode colar o hash e conferir se o PDF e legitimo.
 * Nao expoe dados sensiveis do cliente (so iniciais + CPF mascarado).
 */
@RestController
@RequiredArgsConstructor
public class PdfVerificationController {

    private final PrescriptionIssuanceService issuanceService;

    @GetMapping("/api/pdf-verify/{hash}")
    public PdfVerificationDto verify(@PathVariable String hash) {
        return issuanceService.verify(hash);
    }
}
