package com.prandiumnutri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PrandiumNutriApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrandiumNutriApplication.class, args);
    }
}
