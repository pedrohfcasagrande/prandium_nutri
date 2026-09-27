-- Fase 9D: assinatura digital (imagem PNG) do nutri, embedada no rodape do PDF de prescricao.
-- Armazenamento como data URL base64 pra simplificar deploy (sem storage externo);
-- limite razoavel de ~2MB. Migracao futura pra S3/GCS se ficar apertado.
ALTER TABLE users
    ADD COLUMN signature_base64 MEDIUMTEXT NULL;
