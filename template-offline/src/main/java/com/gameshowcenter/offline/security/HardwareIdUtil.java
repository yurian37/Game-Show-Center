package com.gameshowcenter.offline.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HardwareIdUtil {

    private static String cachedHwid = null;

    /**
     * Gets a unique, reproducible Hardware ID for the current machine.
     * Combines OS user home, processor ID / motherboard UUID, and generates a formatted hash.
     */
    public static synchronized String getHardwareId() {
        if (cachedHwid != null) {
            return cachedHwid;
        }

        StringBuilder rawInfo = new StringBuilder();

        // 1. Windows Motherboard / System UUID
        String sysUuid = getCommandOutput("powershell", "-NoProfile", "-Command", "(Get-CimInstance Win32_ComputerSystemProduct).UUID");
        if (sysUuid == null || sysUuid.isBlank()) {
            sysUuid = getCommandOutput("wmic", "csproduct", "get", "uuid");
        }
        if (sysUuid != null && !sysUuid.isBlank()) {
            rawInfo.append(sysUuid.trim());
        }

        // 2. CPU Processor ID or Name
        String cpuId = getCommandOutput("powershell", "-NoProfile", "-Command", "(Get-CimInstance Win32_Processor).ProcessorId");
        if (cpuId != null && !cpuId.isBlank()) {
            rawInfo.append("-").append(cpuId.trim());
        }

        // 3. Fallback OS properties
        rawInfo.append("-").append(System.getProperty("os.name", ""))
               .append("-").append(System.getProperty("user.name", ""));

        // Hashing via SHA-256
        String hash = sha256(rawInfo.toString());
        if (hash.length() >= 16) {
            // Format as GSC-XXXX-XXXX-XXXX
            cachedHwid = String.format("GSC-%s-%s-%s",
                    hash.substring(0, 4).toUpperCase(),
                    hash.substring(4, 8).toUpperCase(),
                    hash.substring(8, 12).toUpperCase());
        } else {
            cachedHwid = "GSC-OFFLINE-DEFAULT-ID";
        }

        return cachedHwid;
    }

    private static String getCommandOutput(String... commandArgs) {
        try {
            Process process = new ProcessBuilder(commandArgs).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank() && !line.toLowerCase().contains("uuid") && !line.toLowerCase().contains("processorid")) {
                        sb.append(line.trim());
                    }
                }
                process.waitFor();
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            return Integer.toHexString(base.hashCode());
        }
    }
}
