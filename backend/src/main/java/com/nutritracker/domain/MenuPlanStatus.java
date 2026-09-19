package com.nutritracker.domain;

public enum MenuPlanStatus {
    /** Ainda nao submetido (rascunho ou salvo sem nutri). */
    DRAFT,
    /** Aguardando aprovacao do nutricionista. */
    PENDING,
    /** Aprovado (ou nunca precisou de aprovacao — clientes sem nutri). */
    APPROVED,
    /** Rejeitado pelo nutricionista. review_note tem o motivo. */
    REJECTED
}
