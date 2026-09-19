package com.nutritracker.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;
    private final boolean configured;

    public GoogleTokenVerifier(@Value("${nutritracker.google.client-id:}") String clientId) {
        this.configured = clientId != null && !clientId.isBlank();
        this.verifier = configured
                ? new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build()
                : null;
    }

    public GoogleIdToken.Payload verify(String idToken) {
        if (!configured) {
            throw new IllegalStateException("Google Sign-In nao configurado no servidor (GOOGLE_CLIENT_ID vazio)");
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new IllegalArgumentException("Token Google invalido");
            }
            return token.getPayload();
        } catch (Exception e) {
            log.warn("Falha ao verificar token Google", e);
            throw new IllegalArgumentException("Token Google invalido");
        }
    }
}
