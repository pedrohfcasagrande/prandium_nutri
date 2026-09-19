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
     * Mascara CPF para exibicao. Preserva os 3 primeiros e 2 ultimos digitos.
     * Entrada com formatacao (123.456.789-00) ou sem — sao normalizadas.
     * Retorna "***" quando input invalido ou vazio.
     *
     * Exemplo: "12345678901" -> "123.***.***-01"
     */
    public static String mask(String cpf) {
        String d = normalize(cpf);
        if (d == null || d.length() != 11) return "***";
        return d.substring(0, 3) + ".***.***-" + d.substring(9);
    }
}
