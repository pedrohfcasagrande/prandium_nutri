-- V021: Fase 6A — CPF obrigatorio para todo usuario; CRN opcional para nutris.
-- CPF: 11 digitos (armazenado sem formatacao). Mascarado nas telas via CpfUtil.mask().
-- CRN: sigla + numero (ex: "CRN3 12345"). Opcional (varios nutris podem ainda nao ter registrado).

-- Passo 1: adiciona colunas nullable
ALTER TABLE users
    ADD COLUMN cpf VARCHAR(11) NULL AFTER email,
    ADD COLUMN crn VARCHAR(30) NULL AFTER cpf;

-- Passo 2: backfill de CPFs mockados sequenciais (baseados no id, zero-padded).
-- Nao sao validos pelo algoritmo de digitos verificadores, mas atendem UNIQUE.
UPDATE users SET cpf = LPAD(id, 11, '0') WHERE cpf IS NULL;

-- Passo 3: torna NOT NULL + UNIQUE
ALTER TABLE users
    MODIFY COLUMN cpf VARCHAR(11) NOT NULL,
    ADD CONSTRAINT uq_users_cpf UNIQUE (cpf);

-- CRN unico quando presente (multiplos NULLs sao permitidos)
ALTER TABLE users
    ADD CONSTRAINT uq_users_crn UNIQUE (crn);

CREATE INDEX idx_users_cpf ON users (cpf);
CREATE INDEX idx_users_crn ON users (crn);
