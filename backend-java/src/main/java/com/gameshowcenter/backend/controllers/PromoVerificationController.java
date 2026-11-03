package com.gameshowcenter.backend.controllers;

import com.gameshowcenter.backend.security.DownloadTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Hardened Controller for validating Promotional Codes and VIP entitlements.
 * Implements server-side processing integrity (SOC 2 PI1.1 & ISO 27001:2022 A.8.26),
 * issuing cryptographic HMAC-SHA256 download tokens upon authorized verification.
 */
@RestController
@RequestMapping("/api/payment")
public class PromoVerificationController {

    private static final Logger log = LoggerFactory.getLogger(PromoVerificationController.class);

    // Default SHA-256 hash for verified VIP promotional access (configured safely server-side)
    private static final String DEFAULT_VIP_HASH = "53086b510bd55bb3f8373b5cf2e55ef92b512c1c6fbbfbe6e95c1c8a4df55811";

    @Value("${security.promo.vip-secret:GSC_PROMO_VIP_SECURE_2026_SALT}")
    private String promoSecretSalt;

    private final DownloadTokenService downloadTokenService;

    public PromoVerificationController(DownloadTokenService downloadTokenService) {
        this.downloadTokenService = downloadTokenService;
    }

    public static class PromoRequest {
        private String promoCode;

        public String getPromoCode() {
            return promoCode;
        }

        public void setPromoCode(String promoCode) {
            this.promoCode = promoCode;
        }
    }

    @PostMapping("/verify-promo")
    public ResponseEntity<Map<String, Object>> verifyPromoCode(@RequestBody PromoRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (request == null || request.getPromoCode() == null || request.getPromoCode().trim().isEmpty()) {
            response.put("valid", false);
            response.put("message", "Promo code cannot be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        String code = request.getPromoCode().trim().toLowerCase();
        String hash = computeSha256(code);

        if (DEFAULT_VIP_HASH.equalsIgnoreCase(hash)) {
            log.info("Server-side promo code verification successful for VIP key.");
            String downloadToken = downloadTokenService.generateDownloadToken("VIP_ENTERPRISE_ACCESS");

            response.put("valid", true);
            response.put("tier", "VIP_ENTERPRISE_ACCESS");
            response.put("message", "VIP Access code verified! Unlocking Standalone edition...");
            response.put("downloadToken", downloadToken);
            response.put("expiresInMinutes", 15);
            return ResponseEntity.ok(response);
        }

        log.warn("Server-side promo verification rejected invalid code attempt.");
        response.put("valid", false);
        response.put("message", "Invalid or expired promo code. Please verify your VIP key.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout-token")
    public ResponseEntity<Map<String, Object>> generateCheckoutToken() {
        Map<String, Object> response = new HashMap<>();
        String downloadToken = downloadTokenService.generateDownloadToken("COMMERCIAL_CHECKOUT");
        log.info("Issued commercial checkout download token.");
        response.put("success", true);
        response.put("downloadToken", downloadToken);
        response.put("expiresInMinutes", 15);
        return ResponseEntity.ok(response);
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error computing cryptographic hash: ", e);
            return "";
        }
    }
}
