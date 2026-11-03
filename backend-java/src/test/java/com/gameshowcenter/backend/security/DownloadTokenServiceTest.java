package com.gameshowcenter.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DownloadTokenServiceTest {

    private DownloadTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new DownloadTokenService();
        ReflectionTestUtils.setField(tokenService, "tokenSecret", "TEST_SECRET_KEY_FOR_UNIT_TESTS_12345678");
        ReflectionTestUtils.setField(tokenService, "tokenTtlMinutes", 15L);
    }

    @Test
    void testGenerateAndValidateValidToken() {
        String token = tokenService.generateDownloadToken("VIP_ENTERPRISE_ACCESS");
        assertNotNull(token);
        assertTrue(token.contains("."));

        DownloadTokenService.ValidationResult result = tokenService.validateAndConsumeToken(token);
        assertTrue(result.isValid());
        assertEquals(DownloadTokenService.TokenStatus.VALID, result.getStatus());
        assertEquals("VIP_ENTERPRISE_ACCESS", result.getTier());
        assertNotNull(result.getTokenId());
    }

    @Test
    void testReplayAttackPrevention_SingleUseConsumption() {
        String token = tokenService.generateDownloadToken("VIP_ENTERPRISE_ACCESS");

        // First use: Valid
        DownloadTokenService.ValidationResult firstUse = tokenService.validateAndConsumeToken(token);
        assertTrue(firstUse.isValid());

        // Second use (replay attack attempt): Blocked
        DownloadTokenService.ValidationResult secondUse = tokenService.validateAndConsumeToken(token);
        assertFalse(secondUse.isValid());
        assertEquals(DownloadTokenService.TokenStatus.ALREADY_CONSUMED, secondUse.getStatus());
    }

    @Test
    void testTamperedSignatureRejection() {
        String token = tokenService.generateDownloadToken("VIP_ENTERPRISE_ACCESS");
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + ".FORGED_INVALID_SIGNATURE_HEX";

        DownloadTokenService.ValidationResult result = tokenService.validateAndConsumeToken(tamperedToken);
        assertFalse(result.isValid());
        assertEquals(DownloadTokenService.TokenStatus.INVALID_SIGNATURE, result.getStatus());
    }

    @Test
    void testMissingOrMalformedToken() {
        DownloadTokenService.ValidationResult nullRes = tokenService.validateAndConsumeToken(null);
        assertFalse(nullRes.isValid());
        assertEquals(DownloadTokenService.TokenStatus.MISSING_TOKEN, nullRes.getStatus());

        DownloadTokenService.ValidationResult emptyRes = tokenService.validateAndConsumeToken("   ");
        assertFalse(emptyRes.isValid());
        assertEquals(DownloadTokenService.TokenStatus.MISSING_TOKEN, emptyRes.getStatus());

        DownloadTokenService.ValidationResult malformedRes = tokenService.validateAndConsumeToken("justOnePartWithoutPeriod");
        assertFalse(malformedRes.isValid());
        assertEquals(DownloadTokenService.TokenStatus.INVALID_FORMAT, malformedRes.getStatus());
    }
}
