package com.gameshowcenter.offline;

import com.gameshowcenter.offline.security.HardwareIdUtil;
import com.gameshowcenter.offline.security.LicenseEncryptorTool;
import com.gameshowcenter.offline.security.LicenseManager;
import com.gameshowcenter.offline.security.LicensePayload;
import com.gameshowcenter.offline.service.MegaCoreManagerService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class MegaCoreAndLicenseVerificationTest {

    public static void main(String[] args) {
        System.out.println("===================================================================");
        System.out.println("   TEST SUITE: MEGACORE PACKAGES & ENCRYPTED ANNUAL LICENSE       ");
        System.out.println("===================================================================");

        int passed = 0;
        int failed = 0;

        File tempDir = new File("target/test-license-scratch");
        tempDir.mkdirs();

        try {
            // TEST 1: Generate fresh encrypted license with Encryptor Tool
            System.out.println("\n[TEST 1] Generación de licencia encriptada con LicenseEncryptorTool...");
            File testLic1 = new File(tempDir, "test_fresh.lic");
            if (testLic1.exists()) testLic1.delete();

            LicensePayload generated = LicenseEncryptorTool.generateLicenseFile(testLic1, 3, 365);
            if (testLic1.exists() && testLic1.length() > 20 && generated != null) {
                System.out.println("  -> PASSED: Archivo creado con " + testLic1.length() + " bytes y formato encriptado.");
                passed++;
            } else {
                System.err.println("  -> FAILED: No se generó el archivo.");
                failed++;
            }

            // TEST 2: Inspect fresh license and verify 3-day activation window
            System.out.println("\n[TEST 2] Verificación de la ventana de activación de 3 días...");
            byte[] bytes1 = Files.readAllBytes(testLic1.toPath());
            LicensePayload readPayload = LicenseManager.decryptPayload(bytes1);
            if (readPayload != null &&
                !readPayload.isBound() &&
                !readPayload.isActivationWindowExpired() &&
                readPayload.getMaxActivationDays() == 3 &&
                readPayload.getAnnualValidityDays() == 365) {
                System.out.println("  -> PASSED: Estado UNBOUND, 3 días de gracia para activar, 365 días anuales.");
                passed++;
            } else {
                System.err.println("  -> FAILED: Metadatos incorrectos en la licencia generada.");
                failed++;
            }

            // TEST 3: Expired activation window (>3 days) should be rejected
            System.out.println("\n[TEST 3] Rechazo de licencia con más de 3 días de creación...");
            File testExpiredLic = new File(tempDir, "test_expired_window.lic");
            long fourDaysAgo = System.currentTimeMillis() - (4L * 24L * 60L * 60L * 1000L);
            LicensePayload expiredPayload = new LicensePayload();
            expiredPayload.setFormat(LicensePayload.FORMAT_VERSION_V3);
            expiredPayload.setLicenseId("LIC-TEST-EXPIRED");
            expiredPayload.setCreatedAt(fourDaysAgo);
            expiredPayload.setMaxActivationDays(3);
            expiredPayload.setAnnualValidityDays(365);
            expiredPayload.setStatus(LicensePayload.STATUS_UNBOUND);
            expiredPayload.setTier("ENTERPRISE_PRO");
            expiredPayload.setIssuer("YonIA Enterprise Security Authority");
            expiredPayload.setIssuerSignature(LicenseEncryptorTool.signDataWithEd25519(expiredPayload.getCanonicalIssuanceData(), LicenseEncryptorTool.ADMIN_PRIVATE_KEY_B64));
            LicenseManager.writeLicenseFile(testExpiredLic, expiredPayload);

            LicenseManager.ActivationResult expiredRes = LicenseManager.activateWithFile(testExpiredLic);
            if (!expiredRes.isSuccess() && expiredRes.getStatus() == LicenseManager.StatusCategory.ACTIVATION_WINDOW_EXPIRED) {
                System.out.println("  -> PASSED: Bloqueo exitoso con ACTIVATION_WINDOW_EXPIRED: " + expiredRes.getMessage());
                passed++;
            } else {
                System.err.println("  -> FAILED: Se esperaba rechazo por ventana de 3 días vencida. Status: " + expiredRes.getStatus());
                failed++;
            }

            // TEST 4: Activation binds HWID and modifies the file on disk
            System.out.println("\n[TEST 4] Activación vincula HardwareID y modifica físicamente el archivo en disco...");
            String currentHwid = HardwareIdUtil.getHardwareId();
            LicenseManager.ActivationResult actRes = LicenseManager.activateWithFile(testLic1);

            if (actRes.isSuccess() && actRes.getStatus() == LicenseManager.StatusCategory.VALID_ACTIVE) {
                // Verify the file was physically modified on disk
                byte[] modifiedBytes = Files.readAllBytes(testLic1.toPath());
                LicensePayload modifiedPayload = LicenseManager.decryptPayload(modifiedBytes);

                if (modifiedPayload != null &&
                    modifiedPayload.isBound() &&
                    currentHwid.equalsIgnoreCase(modifiedPayload.getBoundHwid()) &&
                    modifiedPayload.getExpiresAt() > System.currentTimeMillis()) {
                    System.out.println("  -> PASSED: Archivo en disco modificado con éxito. Bound HWID: " + modifiedPayload.getBoundHwid() + ", Expiración anual: " + modifiedPayload.formatExpiresAt());
                    passed++;
                } else {
                    System.err.println("  -> FAILED: El archivo en disco no quedó ligado al HardwareID.");
                    failed++;
                }
            } else {
                System.err.println("  -> FAILED: La activación falló: " + actRes.getMessage());
                failed++;
            }

            // TEST 5: Hardware conflict simulation (Anti-reuse)
            System.out.println("\n[TEST 5] Simulación de reuso en otra máquina (Conflicto de HardwareID)...");
            File foreignLic = new File(tempDir, "test_foreign_hwid.lic");
            LicensePayload foreignPayload = new LicensePayload();
            foreignPayload.setFormat(LicensePayload.FORMAT_VERSION_V3);
            foreignPayload.setLicenseId("LIC-TEST-FOREIGN");
            foreignPayload.setCreatedAt(System.currentTimeMillis());
            foreignPayload.setMaxActivationDays(3);
            foreignPayload.setAnnualValidityDays(365);
            foreignPayload.setTier("ENTERPRISE_PRO");
            foreignPayload.setIssuer("YonIA Enterprise Security Authority");
            foreignPayload.setIssuerSignature(LicenseEncryptorTool.signDataWithEd25519(foreignPayload.getCanonicalIssuanceData(), LicenseEncryptorTool.ADMIN_PRIVATE_KEY_B64));
            foreignPayload.setStatus(LicensePayload.STATUS_BOUND);
            foreignPayload.setBoundHwid("GSC-OTHER-MACHINE-9999");
            foreignPayload.setActivatedAt(System.currentTimeMillis());
            foreignPayload.setExpiresAt(System.currentTimeMillis() + (365L * 24L * 60L * 60L * 1000L));
            foreignPayload.setBoundSignature(LicenseManager.calculateHardwareBindingHash(foreignPayload, "GSC-OTHER-MACHINE-9999"));
            LicenseManager.writeLicenseFile(foreignLic, foreignPayload);

            LicenseManager.ActivationResult conflictRes = LicenseManager.activateWithFile(foreignLic);
            if (!conflictRes.isSuccess() && conflictRes.getStatus() == LicenseManager.StatusCategory.HARDWARE_CONFLICT) {
                System.out.println("  -> PASSED: Conflicto detectado y reuso bloqueado exitosamente: " + conflictRes.getMessage());
                passed++;
            } else {
                System.err.println("  -> FAILED: No se detectó el conflicto de hardware. Status: " + conflictRes.getStatus());
                failed++;
            }

            // TEST 5.1: Tampering / Forgery rejection (Asymmetric Cryptography Test)
            System.out.println("\n[TEST 5.1] Detección de adulteración y falsificación de firma Ed25519...");
            File forgedLic = new File(tempDir, "test_forged.lic");
            LicensePayload forgedPayload = new LicensePayload();
            forgedPayload.setFormat(LicensePayload.FORMAT_VERSION_V3);
            forgedPayload.setLicenseId("LIC-TEST-FORGED");
            forgedPayload.setCreatedAt(System.currentTimeMillis());
            forgedPayload.setMaxActivationDays(3);
            forgedPayload.setAnnualValidityDays(365);
            forgedPayload.setStatus(LicensePayload.STATUS_UNBOUND);
            forgedPayload.setTier("ENTERPRISE_PRO");
            forgedPayload.setIssuer("YonIA Enterprise Security Authority");
            // Fake signature
            forgedPayload.setIssuerSignature(Base64.getEncoder().encodeToString("FAKE-SIGNATURE-BYTES-123456789012345678901234567890123456789012345678901234567890".getBytes(StandardCharsets.UTF_8)));
            LicenseManager.writeLicenseFile(forgedLic, forgedPayload);

            LicenseManager.ActivationResult forgedRes = LicenseManager.activateWithFile(forgedLic);
            if (!forgedRes.isSuccess() && forgedRes.getStatus() == LicenseManager.StatusCategory.SIGNATURE_VERIFICATION_FAILED) {
                System.out.println("  -> PASSED: Falsificación detectada y rechazada exitosamente con SIGNATURE_VERIFICATION_FAILED: " + forgedRes.getMessage());
                passed++;
            } else {
                System.err.println("  -> FAILED: No se detectó la firma falsificada. Status: " + forgedRes.getStatus());
                failed++;
            }

            // TEST 6: MegaCore catalog validation
            System.out.println("\n[TEST 6] Verificación de la definición de MegaCore 1 y MegaCore 2...");
            List<String> core1 = MegaCoreManagerService.MEGACORE_1_GAMES;
            List<String> core2 = MegaCoreManagerService.MEGACORE_2_GAMES;

            boolean core1Valid = core1.contains("TimeLine") && core1.contains("Hangman") && core1.contains("TicTacToe")
                    && core1.contains("Trivia Quiz") && core1.contains("Zero Margin");
            boolean core2Valid = core2.contains("GeoLocation") && core2.contains("Guess Character") && core2.contains("Rapid Rhythm")
                    && core2.contains("Snap Solve") && core2.contains("Topic Takedown");

            if (core1Valid && core2Valid) {
                System.out.println("  -> PASSED: MegaCore 1 (" + core1.size() + " juegos) y MegaCore 2 (" + core2.size() + " juegos) definidos correctamente.");
                passed++;
            } else {
                System.err.println("  -> FAILED: Discrepancia en los juegos de MegaCore 1 o 2.");
                failed++;
            }

            // TEST 7: Descompresión e instalación de MegaCore-1.zip y MegaCore-2.zip
            System.out.println("\n[TEST 7] Descompresión e instalación real de paquetes MegaCore en directorio de prueba...");
            File testGamesDir = new File(tempDir, "installed_games");
            if (testGamesDir.exists()) {
                File[] list = testGamesDir.listFiles();
                if (list != null) for (File f : list) f.delete();
            }
            testGamesDir.mkdirs();

            File megacoresDir = MegaCoreManagerService.getInstance().resolveMegacoresDir();
            File zip1 = new File(megacoresDir, "MegaCore-1.zip");
            File zip2 = new File(megacoresDir, "MegaCore-2.zip");

            MegaCoreManagerService.InstallResult res1 = MegaCoreManagerService.getInstance().installPackage(zip1, testGamesDir);
            MegaCoreManagerService.InstallResult res2 = MegaCoreManagerService.getInstance().installPackage(zip2, testGamesDir);

            List<MegaCoreManagerService.MegaCoreStatus> catalogStatus = MegaCoreManagerService.getInstance().getCatalogStatus(testGamesDir);
            boolean allInstalled = catalogStatus.stream().allMatch(MegaCoreManagerService.MegaCoreStatus::isFullyInstalled);

            if (res1.isSuccess() && res2.isSuccess() && allInstalled) {
                System.out.println("  -> PASSED: Ambos paquetes descomprimidos e instalados (Total 10 juegos verificados en disco).");
                passed++;
            } else {
                System.err.println("  -> FAILED: Falló la instalación de paquetes MegaCore.");
                failed++;
            }

        } catch (Exception e) {
            System.err.println("Error general durante las pruebas: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }

        System.out.println("\n===================================================================");
        System.out.println("   RESULTADOS FINALES: PASSED = " + passed + " | FAILED = " + failed);
        System.out.println("===================================================================");

        if (failed > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }
}
