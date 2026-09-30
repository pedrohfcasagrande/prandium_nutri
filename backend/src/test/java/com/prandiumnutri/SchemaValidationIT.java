package com.prandiumnutri;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test: boota o contexto Spring contra MySQL real (via CI service
 * container) com Flyway aplicando migrations. Hibernate roda em
 * {@code ddl-auto=validate} e falha se qualquer entity nao bate com o schema
 * gerado pelas migrations.
 *
 * <p>Justificativa: o perfil {@code test} normal usa H2 com {@code create-drop},
 * ou seja, Hibernate cria o schema a partir das entities — impossivel detectar
 * mismatch entity vs migration. Este smoke pega:
 * <ul>
 *   <li>Nomes de coluna gerados pelo naming strategy que nao batem com o SQL
 *       das migrations (ex.: {@code proteinGPerKg} → {@code proteingper_kg} vs
 *       {@code protein_g_per_kg} esperado).</li>
 *   <li>Tipos incompativeis (ex.: entity VARCHAR(64) vs migration CHAR(64)).</li>
 *   <li>NOT NULL/UNIQUE constraints ausentes na entity.</li>
 * </ul>
 *
 * <p>Ativa so no CI job {@code backend-schema-smoke}, que sobe MySQL 8 service
 * container. Local: rodar via {@code mvn test -Pci-mysql -Dtest=SchemaValidationIT}
 * apontando pra um MySQL local.
 *
 * <p>Requer flag {@code prandiumnutri.ci-mysql.enabled=true} pra evitar rodar
 * acidentalmente no perfil test normal (o {@code @ActiveProfiles} sozinho nao
 * basta porque Spring Boot tests ignoram profile config se nao ha datasource
 * dispnovel).
 */
@SpringBootTest
@ActiveProfiles("ci-mysql")
class SchemaValidationIT {

    /**
     * O contexto so sobe se: MySQL disponivel + Flyway migra tudo + Hibernate
     * valida todas as entities. Se qualquer entity nao bate com o schema, o
     * SpringApplication falha ao inicializar. Este teste passa apenas se o
     * contexto subiu sem erros.
     */
    @Test
    void contextBootsAndSchemaValidates() {
        // vazio de proposito — o beneficio esta no boot do @SpringBootTest
    }
}
