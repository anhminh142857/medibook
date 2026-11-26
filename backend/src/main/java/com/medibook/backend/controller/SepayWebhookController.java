package com.medibook.backend.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibook.backend.service.SepayWebhookService;

@RestController
@RequestMapping("/api/payments/sepay")
public class SepayWebhookController {

    private static final Logger log =
            LoggerFactory.getLogger(SepayWebhookController.class);

    private final boolean enabled;
    private final String apiKey;
    private final SepayWebhookService webhookService;

    public SepayWebhookController(
            @Value("${app.sepay.webhook-enabled:false}")
            boolean enabled,
            @Value("${app.sepay.webhook-api-key:}")
            String apiKey,
            SepayWebhookService webhookService
    ) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization,
            @RequestBody Map<String, Object> payload
    ) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return reply(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    false,
                    "Webhook SePay chua duoc cau hinh."
            );
        }

        if (!hasValidApiKey(authorization)) {
            int receivedLength = authorization != null
                    && authorization.startsWith("Apikey ")
                    ? authorization.substring(7).length()
                    : -1;

            log.warn(
                    "SEPAY_AUTH_FAILED receivedKeyLength={} "
                            + "configuredKeyLength={}",
                    receivedLength,
                    apiKey.length()
            );

            return reply(
                    HttpStatus.UNAUTHORIZED,
                    false,
                    "Khoa xac thuc webhook khong hop le."
            );
        }

        webhookService.handle(payload);

        return ResponseEntity.ok(
                Map.<String, Object>of("success", true)
        );
    }

    private boolean hasValidApiKey(String authorization) {
        if (authorization == null) {
            return false;
        }

        String expected = "Apikey " + apiKey;

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                authorization.getBytes(StandardCharsets.UTF_8)
        );
    }

    private ResponseEntity<Map<String, Object>> reply(
            HttpStatus status,
            boolean success,
            String message
    ) {
        return ResponseEntity.status(status).body(
                Map.<String, Object>of(
                        "success", success,
                        "message", message
                )
        );
    }
}