package com.gameshowcenter.offline.security;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Hardened Administrative and Testing Tool for generating V3 Asymmetric (Ed25519) License Files.
 * Contains the administrator private key strictly in test scope / admin repository (never in client production classes).
 */
public class LicenseEncryptorTool {

    // Administrator Private Key (Ed25519 PKCS#8 Base64) - Strictly for testing and admin generator
    public static final String ADMIN_PRIVATE_KEY_B64 = "MC4CAQAwBQYDK2VwBCIEIMWWjMG9+AUgDO5S6A8saSWaN+ytmYTvxoFLKvDk07S1";

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   🔐 GAME SHOW CENTER - EMISOR DE LICENCIAS ASIMÉTRICAS V3   ");
        System.out.println("   Algoritmo: Ed25519 Digital Signature - Release 2026         ");
        System.out.println("===============================================================\n");

        String outputPath = "license.lic";
        boolean inspectMode = false;
        String inspectFilePath = null;
        int activationGraceDays = 3;
        int annualValidityDays = 365;
        String tier = "ENTERPRISE_PRO";

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
            } else if ("--tier".equalsIgnoreCase(arg)) {
                if (i + 1 < args.length) {
                    tier = args[++i];
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

        generateLicenseFile(new File(outputPath), activationGraceDays, annualValidityDays, tier);
    }

    public static LicensePayload generateLicenseFile(File targetFile, int graceDays, int validityDays) {
        return generateLicenseFile(targetFile, graceDays, validityDays, "ENTERPRISE_PRO");
    }

    /**
     * Generates a fresh unactivated V3 encrypted license file signed with Ed25519.
     */
    public static LicensePayload generateLicenseFile(File targetFile, int graceDays, int validityDays, String tier) {
        try {
            long now = System.currentTimeMillis();
            LicensePayload payload = new LicensePayload();
            payload.setFormat(LicensePayload.FORMAT_VERSION_V3);
            payload.setLicenseId("LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payload.setCreatedAt(now);
            payload.setMaxActivationDays(graceDays);
            payload.setAnnualValidityDays(validityDays);
            payload.setStatus(LicensePayload.STATUS_UNBOUND);
            payload.setBoundHwid(null);
            payload.setActivatedAt(0);
            payload.setExpiresAt(0);
            payload.setTier(tier);
            payload.setIssuer("YonIA Enterprise Security Authority");

            // Sign canonical issuance data using administrator Ed25519 private key
            String signature = signDataWithEd25519(payload.getCanonicalIssuanceData(), ADMIN_PRIVATE_KEY_B64);
            payload.setIssuerSignature(signature);

            LicenseManager.writeLicenseFile(targetFile, payload);

            System.out.println("✅ LICENCIA V3 (Ed25519) GENERADA CON ÉXITO:");
            System.out.println("  -> Archivo destino:          " + targetFile.getAbsolutePath());
            System.out.println("  -> Formato Criptográfico:    " + payload.getFormat() + " (Firma Digital Asimétrica)");
            System.out.println("  -> ID de Licencia:           " + payload.getLicenseId());
            System.out.println("  -> Nivel (Tier):             " + payload.getTier());
            System.out.println("  -> Fecha de Creación:        " + payload.formatCreatedAt());
            System.out.println("  -> Ventana de Activación:    " + payload.getMaxActivationDays() + " días (Caduca para activar: " + payload.formatActivationWindowExpiry() + ")");
            System.out.println("  -> Validez Anual tras uso:   " + payload.getAnnualValidityDays() + " días");
            System.out.println("  -> Firma Asimétrica:         " + payload.getIssuerSignature().substring(0, 16) + "...");
            System.out.println("===============================================================");
            return payload;
        } catch (Exception e) {
            System.err.println("❌ ERROR al generar la licencia: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Signs arbitrary canonical text using Ed25519 private key.
     */
    public static String signDataWithEd25519(String data, String privateKeyBase64) throws Exception {
        byte[] privKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privKeyBytes));

        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(privKey);
        sig.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = sig.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * Inspects a license file (V3 or V2) and prints all verification metrics.
     */
    public static void inspectLicenseFile(File licenseFile) {
        if (!licenseFile.exists()) {
            System.err.println("❌ Archivo no encontrado: " + licenseFile.getAbsolutePath());
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(licenseFile.toPath());
            boolean isV3 = LicenseManager.isV3Format(bytes);
            boolean isV2 = LicenseManager.isV2EncryptedFormat(bytes);

            if (!isV3 && !isV2) {
                System.err.println("❌ El archivo no posee un formato de licencia reconocido (ni GSC3 ni GSC2).");
                return;
            }

            LicensePayload payload = LicenseManager.decryptPayload(bytes);
            if (payload == null) {
                System.err.println("❌ No se pudo decodificar el payload de la licencia.");
                return;
            }

            String currentHwid = HardwareIdUtil.getHardwareId();
            boolean signatureValid = false;
            if (isV3) {
                signatureValid = LicenseManager.verifyAsymmetricSignature(payload.getCanonicalIssuanceData(), payload.getIssuerSignature());
            }

            System.out.println("🔍 METADATOS DEL ARCHIVO DE LICENCIA:");
            System.out.println("  -> Ruta de archivo:          " + licenseFile.getAbsolutePath());
            System.out.println("  -> Versión de formato:       " + payload.getFormat());
            System.out.println("  -> ID de Licencia:           " + payload.getLicenseId());
            System.out.println("  -> Nivel (Tier):             " + payload.getTier());
            System.out.println("  -> Emisor (Issuer):          " + payload.getIssuer());
            System.out.println("  -> ¿Firma Ed25519 Válida?:   " + (signatureValid ? "✅ SÍ (Auténtica)" : (isV2 ? "ℹ️ Legacy V2 HMAC" : "❌ INVÁLIDA")));
            System.out.println("  -> Fecha de Creación:        " + payload.formatCreatedAt());
            System.out.println("  -> Límite de Activación:     " + payload.formatActivationWindowExpiry() + " (" + payload.getMaxActivationDays() + " días)");
            System.out.println("  -> Estado actual:            " + payload.getStatus());
            System.out.println("  -> HardwareID vinculado:     " + (payload.getBoundHwid() != null ? payload.getBoundHwid() : "[NINGUNO - DISPONIBLE]"));
            System.out.println("  -> Días restantes:           " + payload.getRemainingDays() + " días");
            System.out.println("===============================================================");
        } catch (Exception e) {
            System.err.println("❌ ERROR al inspeccionar la licencia: " + e.getMessage());
        }
    }
}
