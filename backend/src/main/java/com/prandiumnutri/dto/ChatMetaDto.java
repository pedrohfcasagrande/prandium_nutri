package com.prandiumnutri.dto;

/**
 * Metadados de um chat pro frontend renderizar cabecalho e checar contexto.
 * <ul>
 *   <li>{@code counterpartyId/Name/Role}: quem esta do outro lado (do ponto de vista do requester).</li>
 *   <li>{@code practiceId/practiceLabel}: a qual practice o vinculo pertence — usado
 *       pelo frontend do nutri pra sair do chat se o practice switcher trocou.</li>
 *   <li>{@code companyId}: null se practice particular; setado se vinculada a empresa.</li>
 * </ul>
 */
public record ChatMetaDto(
        Long linkId,
        Long counterpartyId,
        String counterpartyName,
        String counterpartyRole,
        Long practiceId,
        String practiceLabel,
        Long companyId
) {}
