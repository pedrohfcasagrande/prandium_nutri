package com.prandiumnutri.support;

/**
 * Utilidades para CPF. Fase 6A — LGPD-aware:
 *   - {@link #mask(String)} produz "123.***.***-45" (visao segura para telas admin/nutri).
 *   - Nunca serializar/logar CPF completo. Sempre passar pelo mask() nas fronteiras (DTO, log).
 *
 * Nao valida DV — CPFs no seed sao mockados sequenciais (V021).
 * Se validacao matematica for necessaria, adicionar {@code isValid()} usando digitos verificadores.
 */
public final class CpfUtil {

    private CpfUtil() {}

    /** Normaliza (remove tudo que nao for digito). Retorna null se input for null. */
    public static String normalize(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("\\D", "");
    }

    /**
     * Mascara CPF para exibicao. Preserva apenas os 2 ultimos digitos.
     * Entrada com formatacao (123.456.789-00) ou sem — sao normalizadas.
     * Retorna "***" quando input invalido ou vazio.
     *
     * Exemplo: "12345678901" -> "***.***.***-01"
     *
     * Nota: versoes anteriores mantinham os 3 primeiros digitos, mas isso gerava
     * confusao em CPFs de seed sequenciais (todos comecavam com "000") e vazava
     * mais bits do que o necessario. 2 digitos finais bastam pra desambiguacao visual.
     */
    public static String mask(String cpf) {
        String d = normalize(cpf);
        if (d == null || d.length() != 11) return "***";
        return "***.***.***-" + d.substring(9);
    }
}
