package com.prandiumnutri.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.MenuPlan;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.repository.MenuPlanRepository;
import com.prandiumnutri.support.CpfUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Renderiza HTML (Thymeleaf) → XHTML → PDF via OpenHTMLtoPDF.
 * Template: templates/pdf/prescription.html. Textos localizados via messages_*.properties.
 *
 * Fase 9D-1: template unico. Fase 9D-2 vai adicionar customizacao por nutri
 * (logo, cabecalho, cores) — os blocos `.doc-header` / `.doc-footer` do template
 * ja estao separados pra facilitar essa extensao.
 */
@Service
@RequiredArgsConstructor
public class PrescriptionPdfService {

    private final TemplateEngine templateEngine;
    private final MenuPlanRepository menuPlanRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] render(ClientAssessment assessment, User client, User nutritionist, Locale locale) {
        if (assessment.getTargetCalories() == null) {
            throw new IllegalStateException("Assessment nao tem prescricao (targetCalories=null)");
        }

        Context ctx = new Context(locale != null ? locale : new Locale("pt", "BR"));
        Map<String, Object> vars = buildContext(assessment, client, nutritionist);
        vars.forEach(ctx::setVariable);

        String html = templateEngine.process("pdf/prescription", ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            b.withHtmlContent(html, null);
            b.toStream(out);
            b.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao renderizar PDF de prescricao", e);
        }
    }

    private Map<String, Object> buildContext(ClientAssessment a, User client, User nutri) {
        Map<String, Object> v = new HashMap<>();
        v.put("assessment", a);
        v.put("client", client);
        v.put("nutritionist", nutri);

        LocalDateTime issued = a.getPrescribedAt() != null ? a.getPrescribedAt() : LocalDateTime.now();
        v.put("issuedAtFormatted", issued.toLocalDate().format(DATE_FMT));

        v.put("clientCpfMasked", client.getCpf() != null ? CpfUtil.mask(client.getCpf()) : null);
        v.put("clientAgeSex", formatAgeSex(client));

        // % de carboidratos no total calorico (pra caixa dos macros)
        if (a.getTargetCarbsG() != null && a.getTargetCalories() != null && a.getTargetCalories() > 0) {
            double carbsKcal = a.getTargetCarbsG() * 4.0;
            int pct = (int) Math.round((carbsKcal / a.getTargetCalories()) * 100);
            v.put("carbsPct", pct);
        } else {
            v.put("carbsPct", null);
        }

        // Cardapio ativo do cliente (se houver)
        MenuPlan active = menuPlanRepository
                .findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(client.getId())
                .orElse(null);
        v.put("activeMenu", active);

        return v;
    }

    private String formatAgeSex(User client) {
        StringBuilder sb = new StringBuilder();
        if (client.getBirthDate() != null) {
            int age = Period.between(client.getBirthDate(), LocalDate.now()).getYears();
            sb.append(age).append(" anos");
        }
        if (client.getSex() != null) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(client.getSex().name().charAt(0));
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
