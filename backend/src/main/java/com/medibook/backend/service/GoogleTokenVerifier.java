package com.medibook.backend.service;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class GoogleTokenVerifier {

    public record GoogleProfile(
            String subject,
            String email,
            String fullName
    ) {
    }

    private static final String JWK_SET_URI =
            "https://www.googleapis.com/oauth2/v3/certs";

    private final String clientId;
    private final JwtDecoder decoder;

    public GoogleTokenVerifier(
            @Value("${app.google.client-id:}") String configuredClientId
    ) {
        final String expectedAudience =
                configuredClientId == null ? "" : configuredClientId.trim();

        this.clientId = expectedAudience;

        NimbusJwtDecoder nimbusDecoder =
                NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();

        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String issuer = jwt.getClaimAsString("iss");

            if ("https://accounts.google.com".equals(issuer)
                    || "accounts.google.com".equals(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Issuer khong hop le.", null)
            );
        };

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> audience = jwt.getAudience();

            if (audience != null && audience.contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Audience khong hop le.", null)
            );
        };

        nimbusDecoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(),
                        issuerValidator,
                        audienceValidator
                )
        );

        this.decoder = nimbusDecoder;
    }

    public GoogleProfile verify(String credential) {
        if (clientId.isBlank()) {
            throw new IllegalStateException(
                    "Chua cau hinh app.google.client-id."
            );
        }

        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException(
                    "Thieu token dang nhap Google."
            );
        }

        Jwt jwt;

        try {
            jwt = decoder.decode(credential.trim());
        } catch (JwtException exception) {
            throw new IllegalArgumentException(
                    "Token Google khong hop le hoac da het han."
            );
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String hostedDomain = jwt.getClaimAsString("hd");

        Object verifiedClaim = jwt.getClaim("email_verified");
        boolean emailVerified =
                Boolean.TRUE.equals(verifiedClaim)
                        || "true".equalsIgnoreCase(String.valueOf(verifiedClaim));

        if (subject == null || subject.isBlank()
                || email == null || email.isBlank()
                || !emailVerified) {
            throw new IllegalArgumentException(
                    "Tai khoan Google chua co email da xac minh."
            );
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        boolean googleIsAuthoritative =
                normalizedEmail.endsWith("@gmail.com")
                        || (hostedDomain != null && !hostedDomain.isBlank());

        if (!googleIsAuthoritative) {
            throw new IllegalArgumentException(
                    "Chi ho tro Gmail hoac tai khoan Google Workspace."
            );
        }

        return new GoogleProfile(
                subject,
                normalizedEmail,
                jwt.getClaimAsString("name")
        );
    }
}