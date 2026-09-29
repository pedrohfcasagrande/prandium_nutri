-- Fase 9F: link de videochamada + lembrete automatico 24h antes da consulta.
ALTER TABLE appointments
    ADD COLUMN meeting_url        VARCHAR(500) NULL,   -- ex.: https://meet.google.com/xxx-yyyy-zzz
    ADD COLUMN reminder_sent_at   DATETIME     NULL;   -- populado pelo job pra evitar reenvio

-- Index pra job de lembretes escanear rapido: appointments confirmados em janela [now, now+24h]
-- que ainda nao receberam reminder.
CREATE INDEX idx_appointments_reminder ON appointments (status, scheduled_at, reminder_sent_at);
