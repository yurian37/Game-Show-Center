package com.gameshowcenter.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for issuing and verifying cryptographic One-Time / Ephemeral Download Tokens.
 * Implements HMAC-SHA256 signature verification and replay prevention (single-use consumption)
 * according to SOC 2 CC6.1 and ISO/IEC 27001:2022 A.8.24 (Cryptographic Controls).
 */
@Service
public class DownloadTokenService {

    private static final Logger log = LoggerFactory.getLogger(DownloadTokenService.class);
    private static final String HMAC_ALGO = "HmacSHA256";

    @Value("${security.download.token-secret:GSC_SECURE_DOWNLOAD_HMAC_SECRET_2026_YONIA_ENTERPRISE_KEY}")
    private String tokenSecret;

    @Value("${security.download.token-ttl-minutes:15}")
    private long tokenTtlMinutes;

    // Cache of consumed token JTIs to enforce single-use protection and prevent token replay.
    // Maps JTI -> Expiration Timestamp (epoch millis) for cleanup.
    private final Map<String, Long> consumedTokens = new ConcurrentHashMap<>();

    public enum TokenStatus {
        VALID,
        MISSING_TOKEN,
        INVALID_FORMAT,
        EXPIRED,
        INVALID_SIGNATURE,
        ALREADY_CONSUMED
    }

    public static class ValidationResult {
        private final boolean valid;
        private final TokenStatus status;
        private final String message;
        private final String tier;
        private final String tokenId;

        public ValidationResult(boolean valid, TokenStatus status, String message, String tier, String tokenId) {
            this.valid = valid;
            this.status = status;
            this.message = message;
            this.tier = tier;
            this.tokenId = tokenId;
        }

        public boolean isValid() { return valid; }
        public TokenStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public String getTier() { return tier; }
        public String getTokenId() { return tokenId; }
    }

    /**
     * Generates a signed, ephemeral HMAC-SHA256 download token.
     *
     * @param tier Licensing tier granted by the token (e.g., VIP_ENTERPRISE_ACCESS)
     * @return Compact token string: Base64Url(payload) + "." + Base64Url(hmacSignature)
     */
    public String generateDownloadToken(String tier) {
        cleanExpiredConsumedTokens();

        String jti = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long expiresAt = now + (tokenTtlMinutes * 60L * 1000L);

        // Format: jti:tier:iat:exp
        String canonicalPayload = String.format("%s:%s:%d:%d",
                jti,
                tier != null ? tier : "STANDARD",
                now,
                expiresAt);

        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(canonicalPayload.getBytes(StandardCharsets.UTF_8));
        String signature = computeHmac(encodedPayload);

        log.info("Issued signed download token [JTI: {}] for tier [{}] expiring in {} minutes.", jti, tier, tokenTtlMinutes);
        return encodedPayload + "." + signature;
    }

    /**
     * Validates the cryptographic signature, checks expiration, and consumes the token to prevent replay.
     */
    public ValidationResult validateAndConsumeToken(String rawToken) {
        cleanExpiredConsumedTokens();

        if (rawToken == null || rawToken.trim().isEmpty()) {
            return new ValidationResult(false, TokenStatus.MISSING_TOKEN, "Download token is missing.", null, null);
        }

        String[] parts = rawToken.trim().split("\\.");
        if (parts.length != 2) {
            return new ValidationResult(false, TokenStatus.INVALID_FORMAT, "Malformed download token structure.", null, null);
        }

        String encodedPayload = parts[0];
        String receivedSignature = parts[1];

        // 1. Verify cryptographic HMAC signature using constant-time comparison
        String expectedSignature = computeHmac(encodedPayload);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), receivedSignature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Security Alert: Tampered or invalid download token signature detected.");
            return new ValidationResult(false, TokenStatus.INVALID_SIGNATURE, "Invalid cryptographic token signature.", null, null);
        }

        // 2. Decode and parse canonical payload
        String payloadText;
        try {
            payloadText = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return new ValidationResult(false, TokenStatus.INVALID_FORMAT, "Failed to decode token payload.", null, null);
        }

        String[] fields = payloadText.split(":");
        if (fields.length < 4) {
            return new ValidationResult(false, TokenStatus.INVALID_FORMAT, "Corrupted token payload claims.", null, null);
        }

        String jti = fields[0];
        String tier = fields[1];
        long expiresAt;
        try {
            expiresAt = Long.parseLong(fields[3]);
        } catch (NumberFormatException e) {
            return new ValidationResult(false, TokenStatus.INVALID_FORMAT, "Invalid token timestamp claims.", null, null);
        }

        // 3. Check expiration
        long now = System.currentTimeMillis();
        if (now > expiresAt) {
            log.warn("Download token [JTI: {}] rejected: Expired at {}.", jti, expiresAt);
            return new ValidationResult(false, TokenStatus.EXPIRED, "Download token has expired (validity is 15 minutes). Please request a fresh token.", tier, jti);
        }

        // 4. Enforce Single-Use (Replay prevention)
        if (consumedTokens.containsKey(jti)) {
            log.warn("Security Alert: Replay attack or duplicate download attempt detected for consumed token [JTI: {}].", jti);
            return new ValidationResult(false, TokenStatus.ALREADY_CONSUMED, "Download token has already been consumed and cannot be reused.", tier, jti);
        }

        // Mark as consumed
        consumedTokens.put(jti, expiresAt);
        log.info("Download token [JTI: {}] successfully validated and consumed for tier [{}].", jti, tier);

        return new ValidationResult(true, TokenStatus.VALID, "Token valid.", tier, jti);
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKey = new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 signature", e);
        }
    }

    private void cleanExpiredConsumedTokens() {
        long now = System.currentTimeMillis();
        consumedTokens.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
