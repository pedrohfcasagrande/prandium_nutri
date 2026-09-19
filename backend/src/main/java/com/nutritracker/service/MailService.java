package com.nutritracker.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Enviador de emails. Em dev (nutritracker.mail.enabled=false) loga o conteudo no console
 * ao inves de enviar via SMTP.
 */
@Service
@Slf4j
public class MailService {

    @Value("${nutritracker.mail.enabled:false}")
    private boolean enabled;

    @Value("${nutritracker.mail.from:no-reply@nutritracker.local}")
    private String from;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("MailService desabilitado (nutritracker.mail.enabled=false). Emails serao logados no console.");
        } else if (mailSender == null) {
            log.warn("nutritracker.mail.enabled=true mas JavaMailSender nao configurado. Emails serao logados no console.");
        }
    }

    public void send(String to, String subject, String body) {
        if (!enabled || mailSender == null) {
            log.info("========== EMAIL (modo dev - nao enviado) ==========\nPara: {}\nAssunto: {}\n\n{}\n===================================================",
                    to, subject, body);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
        log.info("Email enviado para {}: {}", to, subject);
    }
}
