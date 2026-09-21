package com.prandiumnutri.domain;

public enum AppointmentStatus {
    /** Solicitado por cliente ou nutri; aguarda a outra parte confirmar. */
    REQUESTED,
    /** Confirmado pelos dois lados; consulta ativa e agendada. */
    CONFIRMED,
    /** Uma das partes pediu reagendamento e sugeriu nova data; aguarda a outra confirmar. */
    RESCHEDULE_REQUESTED,
    /** Cancelado por uma das partes. */
    CANCELED,
    /** Consulta ocorreu (marcada como realizada). */
    COMPLETED,
    /** Cliente nao compareceu. */
    NO_SHOW
}
