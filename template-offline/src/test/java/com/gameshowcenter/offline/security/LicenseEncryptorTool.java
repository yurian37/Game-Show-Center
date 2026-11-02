package com.gameshowcenter.offline.security;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Administrative and Test Tool for generating and inspecting GSC2 encrypted license files.
 * Placed in test scope and administrative generator directory to prevent bundling into client production releases.
 */
public class LicenseEncryptorTool {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   🔐 GAME SHOW CENTER - ENCRIPTADOR DE LICENCIAS ANUALES   ");
        System.out.println("   Desarrollado por YonIA Enterprise - Release 2026            ");
        System.out.println("===============================================================\n");

        String outputPath = "license.lic";
        boolean inspectMode = false;
        String inspectFilePath = null;
        int activationGraceDays = 3;
        int annualValidityDays = 365;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--inspect".equalsIgnoreCase(arg) || "-i".equalsIgnoreCase(arg)) {
                inspectMode = true;
                if (i + 1 < args.length) {
                    inspectFilePath = args[++i];
                }
            } else if ("--out".equalsIgnoreCase(arg) || "-o".equalsIgnoreCase(arg)) {
                if (i + 1 < args.length) {
                    outputPath = args[++i];
                }
            } else if ("--grace".equalsIgnoreCase(arg)) {
                if (i + 1 < args.length) {
                    activationGraceDays = Integer.parseInt(args[++i]);
                }
            } else if ("--days".equalsIgnoreCase(arg)) {
                if (i + 1 < args.length) {
                    annualValidityDays = Integer.parseInt(args[++i]);
                }
            } else if (!arg.startsWith("-")) {
                outputPath = arg;
            }
        }

        if (inspectMode) {
            if (inspectFilePath == null) {
                inspectFilePath = outputPath;
            }
            inspectLicenseFile(new File(inspectFilePath));
            return;
        }

        // Generate fresh unactivated encrypted license
        generateLicenseFile(new File(outputPath), activationGraceDays, annualValidityDays);
    }

    /**
     * Generates a fresh unactivated encrypted license file.
     * At this point, the license is unbound and can only be activated within the grace period (3 days).
     */
    public static LicensePayload generateLicenseFile(File targetFile, int graceDays, int validityDays) {
        try {
            long now = System.currentTimeMillis();
            LicensePayload payload = new LicensePayload();
            payload.setFormat(LicensePayload.FORMAT_VERSION);
            payload.setLicenseId("LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payload.setCreatedAt(now);
            payload.setMaxActivationDays(graceDays);
            payload.setAnnualValidityDays(validityDays);
            payload.setStatus(LicensePayload.STATUS_UNBOUND);
            payload.setBoundHwid(null);
            payload.setActivatedAt(0);
            payload.setExpiresAt(0);

            LicenseManager.writeLicenseFile(targetFile, payload);

            System.out.println("✅ LICENCIA ENCRIPTADA GENERADA EXITOSAMENTE:");
            System.out.println("  -> Archivo destino:          " + targetFile.getAbsolutePath());
            System.out.println("  -> ID de Licencia:           " + payload.getLicenseId());
            System.out.println("  -> Fecha de Creación:        " + payload.formatCreatedAt());
            System.out.println("  -> Ventana de Activación:    " + payload.getMaxActivationDays() + " días (Caduca para activar: " + payload.formatActivationWindowExpiry() + ")");
            System.out.println("  -> Validez Anual tras uso:   " + payload.getAnnualValidityDays() + " días (1 Año completo)");
            System.out.println("  -> Estado inicial:           " + payload.getStatus() + " (Sin vincular a hardware)");
            System.out.println("  -> Tamaño del archivo:       " + targetFile.length() + " bytes (AES-256 encriptado)");
            System.out.println("\nℹ️ REGLA DE SEGURIDAD:");
            System.out.println("  El usuario final debe cargar este archivo dentro de los primeros 3 días.");
            System.out.println("  Al activarse en un equipo, el archivo se modificará automáticamente");
            System.out.println("  y quedará bloqueado exclusivamente para ese HardwareID.");
            System.out.println("===============================================================");
            return payload;
        } catch (Exception e) {
            System.err.println("❌ ERROR al generar la licencia: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Inspects an encrypted license file and prints all metadata in plain text.
     */
    public static void inspectLicenseFile(File licenseFile) {
        if (!licenseFile.exists()) {
            System.err.println("❌ Archivo no encontrado: " + licenseFile.getAbsolutePath());
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(licenseFile.toPath());
            if (!LicenseManager.isV2EncryptedFormat(bytes)) {
                System.err.println("❌ El archivo no posee el formato de encriptación GSC2 válido.");
                return;
            }

            LicensePayload payload = LicenseManager.decryptPayload(bytes);
            if (payload == null) {
                System.err.println("❌ No se pudo desencriptar el archivo. Clave inválida o datos alterados.");
                return;
            }

            String currentHwid = HardwareIdUtil.getHardwareId();

            System.out.println("🔍 METADATOS DEL ARCHIVO DE LICENCIA:");
            System.out.println("  -> Ruta de archivo:          " + licenseFile.getAbsolutePath());
            System.out.println("  -> Versión de formato:       " + payload.getFormat());
            System.out.println("  -> ID de Licencia:           " + payload.getLicenseId());
            System.out.println("  -> Fecha de Creación:        " + payload.formatCreatedAt());
            System.out.println("  -> Límite de Activación:     " + payload.formatActivationWindowExpiry() + " (" + payload.getMaxActivationDays() + " días)");
            System.out.println("  -> ¿Ventana vencida?:        " + (payload.isActivationWindowExpired() ? "SÍ (CADUCADA)" : "NO (Aún dentro de los 3 días)"));
            System.out.println("  -> Estado actual:            " + payload.getStatus());
            System.out.println("  -> HardwareID vinculado:     " + (payload.getBoundHwid() != null ? payload.getBoundHwid() : "[NINGUNO - DISPONIBLE]"));
            System.out.println("  -> Fecha de Activación:      " + (payload.getActivatedAt() > 0 ? payload.formatCreatedAt() : "Aún no activada"));
            System.out.println("  -> Fecha de Expiración Anual:" + (payload.getExpiresAt() > 0 ? payload.formatExpiresAt() : "Se fijará al activar (365 días)"));
            System.out.println("  -> Días restantes:           " + payload.getRemainingDays() + " días");

            System.out.println("\n💻 EVALUACIÓN CONTRA ESTE EQUIPO (HWID: " + currentHwid + "):");
            if (!payload.isBound()) {
                if (payload.isActivationWindowExpired()) {
                    System.out.println("  -> RESULTADO: ❌ CADUCADA. Superó los 3 días desde su creación sin activarse.");
                } else {
                    System.out.println("  -> RESULTADO: ✅ LISTA PARA ACTIVAR. Se ligará a este HardwareID al usarse.");
                }
            } else {
                if (payload.getBoundHwid().equalsIgnoreCase(currentHwid)) {
                    if (payload.isAnnualExpired()) {
                        System.out.println("  -> RESULTADO: ❌ EXPIRADA. La vigencia anual de 365 días ha concluido.");
                    } else {
                        System.out.println("  -> RESULTADO: ✅ VÁLIDA. Licencia activa en este equipo.");
                    }
                } else {
                    System.out.println("  -> RESULTADO: ⚠️ CONFLICTO DE HARDWARE. Ligada a otro equipo (" + payload.getBoundHwid() + "). REUSO BLOQUEADO.");
                }
            }
            System.out.println("===============================================================");
        } catch (Exception e) {
            System.err.println("❌ ERROR al inspeccionar la licencia: " + e.getMessage());
        }
    }
}
