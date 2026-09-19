package com.prandiumnutri;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: garante que o ApplicationContext carrega no perfil test. */
@SpringBootTest
@ActiveProfiles("test")
class PrandiumNutriApplicationTests {

    @Test
    void contextLoads() {
        // pass se subir sem erros
    }
}
