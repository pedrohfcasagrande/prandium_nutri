package com.prandiumnutri.controller;

import com.prandiumnutri.config.WebPushConfig;
import com.prandiumnutri.dto.PushSubscriptionDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.PushSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/me/push-subscriptions")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionService service;
    private final WebPushConfig webPushConfig;
    private final CurrentUser currentUser;

    /** Retorna a chave publica VAPID pra frontend subscribe(). */
    @GetMapping("/config")
    public Map<String, Object> config() {
        String key = webPushConfig.getVapidPublicKey();
        return Map.of(
                "vapidPublicKey", key,
                "enabled", key != null && !key.isBlank()
        );
    }

    @PostMapping
    public Map<String, Object> subscribe(@RequestBody PushSubscriptionDto dto) {
        var saved = service.subscribe(currentUser.get(), dto);
        return Map.of("id", saved.getId(), "endpoint", saved.getEndpoint());
    }

    @DeleteMapping
    public void unsubscribe(@RequestParam String endpoint) {
        service.unsubscribe(endpoint);
    }
}
