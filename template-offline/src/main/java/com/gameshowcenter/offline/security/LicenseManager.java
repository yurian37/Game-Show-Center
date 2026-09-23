package com.gameshowcenter.offline.security;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

public class LicenseManager {

    private static final String LICENSE_FILE_NAME = "license.lic";
    private static final String SECRET_SALT = "YuyiStudio_GSC_Offline_Secret_2026_KeySalt";
    private static final String MASTER_KEY = "YUYI-STUDIO-PRO-2026";

    /**
     * Checks if the current machine is activated.
     * Reads license.lic and verifies if the stored encrypted hardware ID matches this machine's HWID.
     */
    public static boolean isActivated() {
        File licenseFile = getLicenseFile();
        if (!licenseFile.exists()) {
            return false;
        }

        try {
            String content = Files.readString(licenseFile.toPath(), StandardCharsets.UTF_8).trim();
            String[] parts = content.split("::");
            if (parts.length < 2) {
                return false;
            }

            String storedEncryptedHwid = parts[0];
            String storedKey = parts[1];

            String currentHwid = HardwareIdUtil.getHardwareId();

            // Verify Key Validity for current HWID
            if (!validateKey(currentHwid, storedKey)) {
                return false;
            }

            // Verify HWID signature matches current machine
            String expectedEncryptedHwid = encryptHwid(currentHwid);
            return expectedEncryptedHwid.equals(storedEncryptedHwid);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Attempts to activate the product with the provided license key.
     */
    public static boolean activateProduct(String licenseKey) {
        if (licenseKey == null) return false;
        String cleanKey = licenseKey.trim().toUpperCase();

        String currentHwid = HardwareIdUtil.getHardwareId();

        if (!validateKey(currentHwid, cleanKey)) {
            return false;
        }

        try {
            File licenseFile = getLicenseFile();
            if (licenseFile.getParentFile() != null && !licenseFile.getParentFile().exists()) {
                licenseFile.getParentFile().mkdirs();
            }

            String encryptedHwid = encryptHwid(currentHwid);
            String fileContent = encryptedHwid + "::" + cleanKey + "::" + System.currentTimeMillis();

            Files.writeString(licenseFile.toPath(), fileContent, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Validates if a license key is valid for a given HWID.
     */
    public static boolean validateKey(String hwid, String inputKey) {
        if (inputKey == null || inputKey.isBlank()) return false;
        String key = inputKey.trim().toUpperCase();

        if (MASTER_KEY.equalsIgnoreCase(key)) {
            return true;
        }

        String expectedKey = generateValidKeyForHwid(hwid);
        return expectedKey.equalsIgnoreCase(key);
    }

    /**
     * Generates the deterministic valid license key for a given HWID.
     * Format: GSCKEY-XXXX-XXXX-XXXX
     */
    public static String generateValidKeyForHwid(String hwid) {
        String base = hwid + SECRET_SALT;
        String hash = HardwareIdUtil.sha256(base);
        return String.format("GSCKEY-%s-%s-%s",
                hash.substring(0, 4).toUpperCase(),
                hash.substring(4, 8).toUpperCase(),
                hash.substring(8, 12).toUpperCase());
    }

    private static String encryptHwid(String hwid) {
        String base = hwid + "::" + SECRET_SALT;
        return Base64.getEncoder().encodeToString(HardwareIdUtil.sha256(base).getBytes(StandardCharsets.UTF_8));
    }

    private static File getLicenseFile() {
        // Look in local working directory first
        File localFile = new File(LICENSE_FILE_NAME);
        if (localFile.exists()) {
            return localFile;
        }
        // Fallback to user home .gsc_offline directory
        String userHome = System.getProperty("user.home", ".");
        return new File(userHome, ".gsc_offline" + File.separator + LICENSE_FILE_NAME);
    }
}
