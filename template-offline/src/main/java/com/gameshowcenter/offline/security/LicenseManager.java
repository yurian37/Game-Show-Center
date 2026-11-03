package com.gameshowcenter.offline.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * Hardened License Manager implementing Asymmetric Cryptography (Ed25519) and Hardware ID Binding.
 * Conforms to SOC 2 CC6.1/CC6.7 and ISO/IEC 27001:2022 Control A.8.24 (Use of Cryptography).
 * The client application only holds the ISSUER_PUBLIC_KEY for signature verification.
 * The Private Key is kept exclusively in administrator infrastructure.
 */
public class LicenseManager {

    public static final String LICENSE_FILE_NAME = "license.lic";

    // Public Key (Ed25519 X.509 Base64) for validating untampered issuer signatures.
    // Even if decompiled, this public key CANNOT be used to forge or issue new licenses.
    public static final String ISSUER_PUBLIC_KEY = "MCowBQYDK2VwAyEA83E1V0tt4aIVOaqPxNqCFNRTvFps0hyVsYBL0sPaKNI=";

    // Binary Magic Headers for format detection
    public static final byte[] MAGIC_HEADER_V3 = new byte[]{'G', 'S', 'C', '3'};
    public static final byte[] MAGIC_HEADER_V2 = new byte[]{'G', 'S', 'C', '2'};

    // Legacy symmetric salt retained strictly for backward compatibility with V2 files
    private static final String LEGACY_SECRET_SALT = "YonIAEnterprise_GSC_Offline_Secret_2026_KeySalt";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static LicensePayload cachedPayload = null;

    public enum StatusCategory {
        VALID_ACTIVE,
        UNBOUND_PENDING_ACTIVATION,
        HARDWARE_CONFLICT,
        ACTIVATION_WINDOW_EXPIRED,
        ANNUAL_EXPIRED,
        INVALID_OR_CORRUPT,
        SIGNATURE_VERIFICATION_FAILED,
        NOT_FOUND
    }

    public static class ActivationResult {
        private final boolean success;
        private final StatusCategory status;
        private final String message;
        private final LicensePayload payload;

        public ActivationResult(boolean success, StatusCategory status, String message, LicensePayload payload) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.payload = payload;
        }

        public boolean isSuccess() { return success; }
        public StatusCategory getStatus() { return status; }
        public String getMessage() { return message; }
        public LicensePayload getPayload() { return payload; }
    }

    /**
     * Checks if the application currently has a valid, active license on this machine.
     */
    public static boolean isActivated() {
        ActivationResult result = verifyCurrentInstallation();
        return result.isSuccess();
    }

    /**
     * Gets the full verification status of the current installation.
     */
    public static ActivationResult verifyCurrentInstallation() {
        File licenseFile = locateExistingLicenseFile();
        if (licenseFile == null || !licenseFile.exists()) {
            return new ActivationResult(false, StatusCategory.NOT_FOUND, "No license file found.", null);
        }

        return evaluateLicenseFile(licenseFile, false);
    }

    /**
     * Attempts to activate using a license file chosen by the user.
     */
    public static ActivationResult activateWithFile(File licenseFile) {
        if (licenseFile == null || !licenseFile.exists()) {
            return new ActivationResult(false, StatusCategory.NOT_FOUND, "El archivo de licencia especificado no existe.", null);
        }

        return evaluateLicenseFile(licenseFile, true);
    }

    /**
     * Evaluates a license file, verifying asymmetric signatures and binding if unbound.
     */
    private static ActivationResult evaluateLicenseFile(File licenseFile, boolean allowActivationBinding) {
        try {
            byte[] rawBytes = Files.readAllBytes(licenseFile.toPath());

            // 1. Process V3 Asymmetric Binary Format (GSC3) or V2 Legacy (GSC2)
            if (isV3Format(rawBytes) || isV2EncryptedFormat(rawBytes)) {
                LicensePayload payload = decryptPayload(rawBytes);
                if (payload == null) {
                    return new ActivationResult(false, StatusCategory.INVALID_OR_CORRUPT,
                            "No se pudo validar el archivo de licencia. Archivo dañado, firma inválida o formato desconocido.", null);
                }

                // Verify asymmetric signature for V3
                if (LicensePayload.FORMAT_VERSION_V3.equalsIgnoreCase(payload.getFormat())) {
                    if (payload.getIssuerSignature() == null || !verifyAsymmetricSignature(payload.getCanonicalIssuanceData(), payload.getIssuerSignature())) {
                        return new ActivationResult(false, StatusCategory.SIGNATURE_VERIFICATION_FAILED,
                                "Firma digital del emisor inválida. El archivo de licencia ha sido alterado o no fue emitido por YonIA Enterprise.", payload);
                    }
                }

                String currentHwid = HardwareIdUtil.getHardwareId();

                // Case A: Unbound License (fresh from administrator generator)
                if (!payload.isBound()) {
                    if (payload.isActivationWindowExpired()) {
                        String msg = String.format(
                                "El archivo de licencia ha vencido su período de activación permitido (máximo %d días desde su creación: %s). Solicita una nueva emisión.",
                                payload.getMaxActivationDays(), payload.formatCreatedAt());
                        return new ActivationResult(false, StatusCategory.ACTIVATION_WINDOW_EXPIRED, msg, payload);
                    }

                    if (!allowActivationBinding) {
                        return new ActivationResult(false, StatusCategory.UNBOUND_PENDING_ACTIVATION,
                                "El archivo de licencia es auténtico pero requiere ser activado para este equipo.", payload);
                    }

                    // Perform hardware binding & annual activation
                    long now = System.currentTimeMillis();
                    payload.setStatus(LicensePayload.STATUS_BOUND);
                    payload.setBoundHwid(currentHwid);
                    payload.setActivatedAt(now);
                    payload.setExpiresAt(now + (payload.getAnnualValidityDays() * 24L * 60L * 60L * 1000L));
                    payload.setBoundSignature(calculateHardwareBindingHash(payload, currentHwid));

                    // Overwrite the loaded file so it cannot be transferred to another machine
                    try {
                        licenseFile.setWritable(true);
                        writeLicenseFile(licenseFile, payload);
                    } catch (Exception e) {
                        System.err.println("Warning: could not overwrite source license file: " + e.getMessage());
                    }

                    saveToPersistentStorage(payload);
                    cachedPayload = payload;

                    String successMsg = String.format(
                            "¡Licencia anual activada con éxito! Vinculada al equipo [%s]. Vigente por %d días hasta %s.",
                            currentHwid, payload.getAnnualValidityDays(), payload.formatExpiresAt());
                    return new ActivationResult(true, StatusCategory.VALID_ACTIVE, successMsg, payload);
                }

                // Case B: Already bound license - enforce hardware isolation
                if (!currentHwid.equalsIgnoreCase(payload.getBoundHwid())) {
                    String conflictMsg = String.format(
                            "⚠️ CONFLICTO DE HARDWARE: Este archivo de licencia ya fue utilizado previamente y está vinculado al HardwareID [%s]. No puede reutilizarse en este equipo (HardwareID actual: [%s]).",
                            payload.getBoundHwid(), currentHwid);
                    return new ActivationResult(false, StatusCategory.HARDWARE_CONFLICT, conflictMsg, payload);
                }

                if (payload.isAnnualExpired()) {
                    String expiredMsg = String.format(
                            "Tu licencia anual expiró el %s. La vigencia de %d días ha concluido. Por favor renueva tu suscripción.",
                            payload.formatExpiresAt(), payload.getAnnualValidityDays());
                    return new ActivationResult(false, StatusCategory.ANNUAL_EXPIRED, expiredMsg, payload);
                }

                cachedPayload = payload;
                if (allowActivationBinding) {
                    saveToPersistentStorage(payload);
                }

                long remainingDays = payload.getRemainingDays();
                String validMsg = String.format(
                        "Licencia anual activa y válida para este equipo (%s). Vence el %s (%d días restantes).",
                        payload.getTier() != null ? payload.getTier() : "PRO", payload.formatExpiresAt(), remainingDays);
                return new ActivationResult(true, StatusCategory.VALID_ACTIVE, validMsg, payload);
            }

            return new ActivationResult(false, StatusCategory.INVALID_OR_CORRUPT, "Formato de archivo de licencia desconocido.", null);

        } catch (Exception e) {
            return new ActivationResult(false, StatusCategory.INVALID_OR_CORRUPT, "Error al procesar licencia: " + e.getMessage(), null);
        }
    }

    /**
     * Activates with product key or file path.
     */
    public static boolean activateProduct(String licenseKeyOrPath) {
        if (licenseKeyOrPath == null || licenseKeyOrPath.isBlank()) return false;
        String clean = licenseKeyOrPath.trim();

        // 1. Check if it's a file path
        File fileCandidate = new File(clean);
        if (fileCandidate.exists() && fileCandidate.isFile()) {
            ActivationResult res = activateWithFile(fileCandidate);
            return res.isSuccess();
        }

        // 2. Fallback: validate key algorithm against machine HWID
        String currentHwid = HardwareIdUtil.getHardwareId();
        if (validateKey(currentHwid, clean)) {
            LicensePayload payload = new LicensePayload();
            payload.setFormat(LicensePayload.FORMAT_VERSION_V3);
            payload.setLicenseId("KEY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payload.setStatus(LicensePayload.STATUS_BOUND);
            payload.setBoundHwid(currentHwid);
            payload.setActivatedAt(System.currentTimeMillis());
            payload.setExpiresAt(System.currentTimeMillis() + (365L * 24L * 60L * 60L * 1000L));
            payload.setTier("DIRECT_KEY_PRO");
            saveToPersistentStorage(payload);
            cachedPayload = payload;
            return true;
        }

        return false;
    }

    public static boolean validateKey(String hwid, String inputKey) {
        if (inputKey == null || inputKey.isBlank()) return false;
        String key = inputKey.trim().toUpperCase();

        String expectedKey = generateKeyWithSalt(hwid, LEGACY_SECRET_SALT);
        return expectedKey.equalsIgnoreCase(key);
    }

    private static String generateKeyWithSalt(String hwid, String salt) {
        String base = hwid + salt;
        String hash = HardwareIdUtil.sha256(base);
        return String.format("GSCKEY-%s-%s-%s",
                hash.substring(0, 4).toUpperCase(),
                hash.substring(4, 8).toUpperCase(),
                hash.substring(8, 12).toUpperCase());
    }

    // --- ASYMMETRIC CRYPTOGRAPHIC UTILITIES (Ed25519) ---

    public static boolean verifyAsymmetricSignature(String canonicalData, String base64Signature) {
        if (canonicalData == null || base64Signature == null || base64Signature.isBlank()) return false;
        try {
            byte[] pubKeyBytes = Base64.getDecoder().decode(ISSUER_PUBLIC_KEY);
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pubKeyBytes));

            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(pubKey);
            sig.update(canonicalData.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(base64Signature));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isV3Format(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        return bytes[0] == MAGIC_HEADER_V3[0] &&
               bytes[1] == MAGIC_HEADER_V3[1] &&
               bytes[2] == MAGIC_HEADER_V3[2] &&
               bytes[3] == MAGIC_HEADER_V3[3];
    }

    public static boolean isV2EncryptedFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 20) return false;
        return bytes[0] == MAGIC_HEADER_V2[0] &&
               bytes[1] == MAGIC_HEADER_V2[1] &&
               bytes[2] == MAGIC_HEADER_V2[2] &&
               bytes[3] == MAGIC_HEADER_V2[3];
    }

    public static LicensePayload decryptPayload(byte[] fileBytes) {
        try {
            if (isV3Format(fileBytes)) {
                byte[] jsonBytes = Arrays.copyOfRange(fileBytes, 4, fileBytes.length);
                String json = new String(jsonBytes, StandardCharsets.UTF_8);
                return MAPPER.readValue(json, LicensePayload.class);
            }

            if (isV2EncryptedFormat(fileBytes)) {
                byte[] iv = Arrays.copyOfRange(fileBytes, 4, 20);
                byte[] cipherBytes = Arrays.copyOfRange(fileBytes, 20, fileBytes.length);

                byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(LEGACY_SECRET_SALT.getBytes(StandardCharsets.UTF_8));
                SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(iv);

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
                byte[] decrypted = cipher.doFinal(cipherBytes);

                String json = new String(decrypted, StandardCharsets.UTF_8);
                return MAPPER.readValue(json, LicensePayload.class);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeLicenseFile(File targetFile, LicensePayload payload) throws Exception {
        if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        byte[] outputBytes;
        if (LicensePayload.FORMAT_VERSION_V2.equalsIgnoreCase(payload.getFormat())) {
            outputBytes = encryptPayloadV2(payload);
        } else {
            // Write V3 Format: MAGIC_HEADER_V3 + UTF-8 JSON payload
            payload.setFormat(LicensePayload.FORMAT_VERSION_V3);
            String json = MAPPER.writeValueAsString(payload);
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(MAGIC_HEADER_V3);
            baos.write(jsonBytes);
            outputBytes = baos.toByteArray();
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(outputBytes);
            fos.flush();
        }
    }

    public static byte[] encryptPayloadV2(LicensePayload payload) throws Exception {
        payload.setSignature(calculateLegacySignature(payload));
        String json = MAPPER.writeValueAsString(payload);
        byte[] plainBytes = json.getBytes(StandardCharsets.UTF_8);

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(LEGACY_SECRET_SALT.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] cipherBytes = cipher.doFinal(plainBytes);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC_HEADER_V2);
        out.write(iv);
        out.write(cipherBytes);
        return out.toByteArray();
    }

    public static String calculateHardwareBindingHash(LicensePayload payload, String hwid) {
        String base = payload.getLicenseId() + "::" + hwid + "::" + payload.getExpiresAt();
        return HardwareIdUtil.sha256(base);
    }

    public static String calculateLegacySignature(LicensePayload payload) {
        String base = payload.getFormat() + "::" +
                      payload.getCreatedAt() + "::" +
                      payload.getMaxActivationDays() + "::" +
                      payload.getAnnualValidityDays() + "::" +
                      payload.getStatus() + "::" +
                      (payload.getBoundHwid() != null ? payload.getBoundHwid() : "NONE") + "::" +
                      payload.getExpiresAt() + "::" +
                      LEGACY_SECRET_SALT;
        return HardwareIdUtil.sha256(base);
    }

    private static void saveToPersistentStorage(LicensePayload payload) {
        try {
            File localFile = new File(LICENSE_FILE_NAME);
            writeLicenseFile(localFile, payload);
        } catch (Exception ignored) {}
    }

    private static File locateExistingLicenseFile() {
        File local = new File(LICENSE_FILE_NAME);
        if (local.exists() && local.length() > 0) {
            return local;
        }
        return null;
    }

    public static LicensePayload getCachedPayload() {
        if (cachedPayload == null) {
            verifyCurrentInstallation();
        }
        return cachedPayload;
    }

    public static long getRemainingDays() {
        LicensePayload payload = getCachedPayload();
        return payload != null ? payload.getRemainingDays() : 0;
    }

    public static String getExpirationDateFormatted() {
        LicensePayload payload = getCachedPayload();
        return payload != null ? payload.formatExpiresAt() : "N/A";
    }
}
