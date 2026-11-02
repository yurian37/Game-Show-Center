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
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

public class LicenseManager {

    public static final String LICENSE_FILE_NAME = "license.lic";
    private static final String SECRET_SALT = "YonIAEnterprise_GSC_Offline_Secret_2026_KeySalt";
    private static final byte[] MAGIC_HEADER = new byte[]{'G', 'S', 'C', '2'};

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static LicensePayload cachedPayload = null;

    public enum StatusCategory {
        VALID_ACTIVE,
        UNBOUND_PENDING_ACTIVATION,
        HARDWARE_CONFLICT,
        ACTIVATION_WINDOW_EXPIRED,
        ANNUAL_EXPIRED,
        INVALID_OR_CORRUPT,
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
     * Attempts to activate using an encrypted license file chosen by the user.
     * If the file is fresh (unbound) and within 3 days of creation, binds to this machine's HWID,
     * calculates the 365-day annual expiration, and OVERWRITES the file on disk.
     */
    public static ActivationResult activateWithFile(File licenseFile) {
        if (licenseFile == null || !licenseFile.exists()) {
            return new ActivationResult(false, StatusCategory.NOT_FOUND, "El archivo de licencia especificado no existe.", null);
        }

        return evaluateLicenseFile(licenseFile, true);
    }

    /**
     * Evaluates a license file, optionally binding it if unbound.
     */
    private static ActivationResult evaluateLicenseFile(File licenseFile, boolean allowActivationBinding) {
        try {
            byte[] rawBytes = Files.readAllBytes(licenseFile.toPath());

            // 1. Check if it's the encrypted V2 binary format
            if (isV2EncryptedFormat(rawBytes)) {
                LicensePayload payload = decryptPayload(rawBytes);
                if (payload == null) {
                    return new ActivationResult(false, StatusCategory.INVALID_OR_CORRUPT,
                            "No se pudo desencriptar el archivo de licencia. Archivo dañado o clave incorrecta.", null);
                }

                String currentHwid = HardwareIdUtil.getHardwareId();

                // Case A: Unbound License (fresh from encryptor)
                if (!payload.isBound()) {
                    // Verify 3-day activation window from creation date
                    if (payload.isActivationWindowExpired()) {
                        String msg = String.format(
                                "El archivo de licencia ha vencido su período de activación permitido (máximo %d días desde su creación: %s). Por favor genera una nueva licencia.",
                                payload.getMaxActivationDays(), payload.formatCreatedAt());
                        return new ActivationResult(false, StatusCategory.ACTIVATION_WINDOW_EXPIRED, msg, payload);
                    }

                    if (!allowActivationBinding) {
                        return new ActivationResult(false, StatusCategory.UNBOUND_PENDING_ACTIVATION,
                                "El archivo de licencia es válido pero requiere ser activado.", payload);
                    }

                    // Perform hardware binding & annual activation
                    long now = System.currentTimeMillis();
                    payload.setStatus(LicensePayload.STATUS_BOUND);
                    payload.setBoundHwid(currentHwid);
                    payload.setActivatedAt(now);
                    payload.setExpiresAt(now + (payload.getAnnualValidityDays() * 24L * 60L * 60L * 1000L));
                    payload.setSignature(calculateSignature(payload));

                    // CRITICAL: Overwrite the loaded file so it cannot be reused on other machines!
                    try {
                        licenseFile.setWritable(true);
                        writeLicenseFile(licenseFile, payload);
                    } catch (Exception e) {
                        System.err.println("Warning: could not overwrite source license file: " + e.getMessage());
                    }

                    // Save to application's persistent locations
                    saveToPersistentStorage(payload);
                    cachedPayload = payload;

                    String successMsg = String.format(
                            "¡Licencia anual activada con éxito! Vinculada al equipo [%s]. Vigente por %d días hasta %s.",
                            currentHwid, payload.getAnnualValidityDays(), payload.formatExpiresAt());
                    return new ActivationResult(true, StatusCategory.VALID_ACTIVE, successMsg, payload);
                }

                // Case B: Already bound license
                if (!payload.getBoundHwid().equalsIgnoreCase(currentHwid)) {
                    String conflictMsg = String.format(
                            "⚠️ CONFLICTO DE HARDWARE: Este archivo de licencia ya fue utilizado previamente y está vinculado al HardwareID [%s]. No puede reutilizarse en este equipo (HardwareID actual: [%s]).",
                            payload.getBoundHwid(), currentHwid);
                    return new ActivationResult(false, StatusCategory.HARDWARE_CONFLICT, conflictMsg, payload);
                }

                // Bound to this machine - check annual expiration
                if (payload.isAnnualExpired()) {
                    String expiredMsg = String.format(
                            "Tu licencia anual expiró el %s. La vigencia de %d días ha concluido. Por favor renueva tu licencia.",
                            payload.formatExpiresAt(), payload.getAnnualValidityDays());
                    return new ActivationResult(false, StatusCategory.ANNUAL_EXPIRED, expiredMsg, payload);
                }

                cachedPayload = payload;
                if (allowActivationBinding) {
                    saveToPersistentStorage(payload);
                }

                long remainingDays = payload.getRemainingDays();
                String validMsg = String.format(
                        "Licencia anual activa y válida para este equipo. Vence el %s (%d días restantes).",
                        payload.formatExpiresAt(), remainingDays);
                return new ActivationResult(true, StatusCategory.VALID_ACTIVE, validMsg, payload);
            }

            // 2. Legacy / Text format fallback for developer backwards compatibility
            String textContent = new String(rawBytes, StandardCharsets.UTF_8).trim();
            if (textContent.contains("::")) {
                String[] parts = textContent.split("::");
                if (parts.length >= 2) {
                    String storedKey = parts[1];
                    String currentHwid = HardwareIdUtil.getHardwareId();
                    if (validateKey(currentHwid, storedKey)) {
                        LicensePayload legacyPayload = new LicensePayload();
                        legacyPayload.setStatus(LicensePayload.STATUS_BOUND);
                        legacyPayload.setBoundHwid(currentHwid);
                        legacyPayload.setExpiresAt(System.currentTimeMillis() + (365L * 24L * 60L * 60L * 1000L));
                        cachedPayload = legacyPayload;
                        return new ActivationResult(true, StatusCategory.VALID_ACTIVE, "Licencia heredada activa.", legacyPayload);
                    }
                }
            }

            return new ActivationResult(false, StatusCategory.INVALID_OR_CORRUPT, "Formato de archivo de licencia desconocido.", null);

        } catch (Exception e) {
            return new ActivationResult(false, StatusCategory.INVALID_OR_CORRUPT, "Error al leer el archivo de licencia: " + e.getMessage(), null);
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
            payload.setLicenseId("KEY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payload.setStatus(LicensePayload.STATUS_BOUND);
            payload.setBoundHwid(currentHwid);
            payload.setActivatedAt(System.currentTimeMillis());
            payload.setExpiresAt(System.currentTimeMillis() + (365L * 24L * 60L * 60L * 1000L));
            saveToPersistentStorage(payload);
            cachedPayload = payload;
            return true;
        }

        return false;
    }

    public static boolean validateKey(String hwid, String inputKey) {
        if (inputKey == null || inputKey.isBlank()) return false;
        String key = inputKey.trim().toUpperCase();

        String expectedKey = generateKeyWithSalt(hwid, SECRET_SALT);
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

    // --- CRYPTOGRAPHIC UTILITIES ---

    public static boolean isV2EncryptedFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 20) return false;
        return bytes[0] == MAGIC_HEADER[0] &&
               bytes[1] == MAGIC_HEADER[1] &&
               bytes[2] == MAGIC_HEADER[2] &&
               bytes[3] == MAGIC_HEADER[3];
    }

    public static byte[] encryptPayload(LicensePayload payload) throws Exception {
        payload.setSignature(calculateSignature(payload));
        String json = MAPPER.writeValueAsString(payload);
        byte[] plainBytes = json.getBytes(StandardCharsets.UTF_8);

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(SECRET_SALT.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] cipherBytes = cipher.doFinal(plainBytes);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC_HEADER);
        out.write(iv);
        out.write(cipherBytes);
        return out.toByteArray();
    }

    public static LicensePayload decryptPayload(byte[] fileBytes) {
        try {
            if (!isV2EncryptedFormat(fileBytes)) return null;

            byte[] iv = Arrays.copyOfRange(fileBytes, 4, 20);
            byte[] cipherBytes = Arrays.copyOfRange(fileBytes, 20, fileBytes.length);

            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(SECRET_SALT.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(cipherBytes);

            String json = new String(decrypted, StandardCharsets.UTF_8);
            return MAPPER.readValue(json, LicensePayload.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeLicenseFile(File targetFile, LicensePayload payload) throws Exception {
        if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        byte[] encryptedBytes = encryptPayload(payload);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(encryptedBytes);
            fos.flush();
        }
    }

    public static String calculateSignature(LicensePayload payload) {
        String base = payload.getFormat() + "::" +
                      payload.getCreatedAt() + "::" +
                      payload.getMaxActivationDays() + "::" +
                      payload.getAnnualValidityDays() + "::" +
                      payload.getStatus() + "::" +
                      (payload.getBoundHwid() != null ? payload.getBoundHwid() : "NONE") + "::" +
                      payload.getExpiresAt() + "::" +
                      SECRET_SALT;
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
