package com.prandiumnutri.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.MenuPlan;
import com.prandiumnutri.domain.NutritionistPractice;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.repository.MenuPlanRepository;
import com.prandiumnutri.repository.NutritionistPracticeRepository;
import com.prandiumnutri.support.CpfUtil;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
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
 * Fase 9D-2: nutri customiza logo, cor primaria, header/footer text (HTML sanitizado via Jsoup).
 * Auto-snapshot: cada render bem-sucedido dispara {@link ClientAssessmentService#createSnapshot}
 * com label "PDF emitido em ..." para garantir rastreabilidade sem tabela nova (backlog 9D-3).
 */
@Service
@RequiredArgsConstructor
public class PrescriptionPdfService {

    private final TemplateEngine templateEngine;
    private final MenuPlanRepository menuPlanRepository;
    private final NutritionistPracticeRepository practiceRepository;
    private final ClientAssessmentService assessmentService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TS_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Cor teal Prandium (default quando nutri nao personaliza)
    private static final String DEFAULT_PRIMARY      = "#0e8f83";
    private static final String DEFAULT_PRIMARY_DARK = "#0a6c63";

    // Whitelist para os campos custom header/footer — apenas formatacao basica + links
    private static final Safelist HTML_SAFELIST = Safelist.basicWithImages()
            .removeTags("img") // sem <img> aqui (logo tem campo proprio)
            .addAttributes("a", "href", "title", "target");

    public byte[] render(ClientAssessment assessment, User client, User nutritionist, Locale locale) {
        return render(assessment, client, nutritionist, locale, /* snapshot */ true);
    }

    /** Sobrecarga para permitir preview (sem snapshot) e testes. */
    public byte[] render(ClientAssessment assessment, User client, User nutritionist,
                          Locale locale, boolean createSnapshot) {
        if (assessment.getTargetCalories() == null) {
            throw new IllegalStateException("Assessment nao tem prescricao (targetCalories=null)");
        }

        Context ctx = new Context(locale != null ? locale : new Locale("pt", "BR"));
        Map<String, Object> vars = buildContext(assessment, client, nutritionist);
        vars.forEach(ctx::setVariable);

        String html = templateEngine.process("pdf/prescription", ctx);
        byte[] pdf;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            b.withHtmlContent(html, null);
            b.toStream(out);
            b.run();
            pdf = out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao renderizar PDF de prescricao", e);
        }

        // Auto-snapshot pra rastreabilidade: garante que "o PDF que o cliente recebeu em X"
        // possa ser reconstituido mesmo depois da ficha mudar. So faz se o nutri estiver
        // conhecido (via assessment.nutritionist) — no preview passa createSnapshot=false.
        if (createSnapshot && assessment.getNutritionist() != null && assessment.getClient() != null) {
            try {
                assessmentService.createSnapshot(
                        assessment.getNutritionist(),
                        assessment.getClient().getId(),
                        null,
                        "PDF emitido em " + LocalDateTime.now().format(TS_FMT)
                );
            } catch (Exception ignore) {
                // Snapshot falho nao pode bloquear o download.
            }
        }
        return pdf;
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

        // Fase 9D-2-fix: customizacao por practice (tenant).
        // Preferencia: practice do assessment (contexto do atendimento).
        // Fallback: particular do nutri (quando ficha nao tem practice ligada).
        NutritionistPractice source = a.getPractice();
        if (source == null && nutri != null) {
            source = practiceRepository.findParticular(nutri.getId()).orElse(null);
        }

        String primary = source != null ? validColorOrNull(source.getPdfPrimaryColor()) : null;
        v.put("primaryColor",     primary != null ? primary : DEFAULT_PRIMARY);
        v.put("primaryColorDark", primary != null ? darken(primary) : DEFAULT_PRIMARY_DARK);
        v.put("headerHtml", source != null ? sanitize(source.getPdfHeaderText()) : null);
        v.put("footerHtml", source != null ? sanitize(source.getPdfFooterText()) : null);
        v.put("pdfLogoBase64", source != null ? source.getPdfLogoBase64() : null);

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

    /** Limpa HTML pra evitar tags perigosas — mantem so <b> <i> <a href> <br> <p>. */
    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return Jsoup.clean(raw, HTML_SAFELIST);
    }

    /** Aceita "#RRGGBB" ou "#RRGGBBAA". Retorna null (usa default) se invalido. */
    private String validColorOrNull(String c) {
        if (c == null) return null;
        return c.matches("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$") ? c : null;
    }

    /** Escurece 18% pra usar como sombra/gradient escuro. */
    private String darken(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            r = (int) (r * 0.82); g = (int) (g * 0.82); b = (int) (b * 0.82);
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return DEFAULT_PRIMARY_DARK;
        }
    }
}
