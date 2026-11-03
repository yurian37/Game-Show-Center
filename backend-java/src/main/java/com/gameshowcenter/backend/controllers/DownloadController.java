package com.gameshowcenter.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameshowcenter.backend.security.DownloadTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Hardened Download Controller protected by Ephemeral One-Time HMAC Download Tokens.
 * Conforms to SOC 2 CC6.1 & ISO/IEC 27001:2022 A.8.24 (Cryptographic Download Integrity).
 */
@RestController
@RequestMapping("/api/download")
public class DownloadController {

    private static final Logger log = LoggerFactory.getLogger(DownloadController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DownloadTokenService downloadTokenService;

    public DownloadController(DownloadTokenService downloadTokenService) {
        this.downloadTokenService = downloadTokenService;
    }

    @GetMapping("/offline-template")
    public void downloadOfflineTemplate(
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestHeader(value = "X-Download-Token", required = false) String tokenHeader,
            HttpServletResponse response) throws IOException {

        String token = (tokenParam != null && !tokenParam.isBlank()) ? tokenParam : tokenHeader;

        // 1. Enforce Ephemeral HMAC-SHA256 Token Validation & Replay Consumption
        DownloadTokenService.ValidationResult validationResult = downloadTokenService.validateAndConsumeToken(token);
        if (!validationResult.isValid()) {
            log.warn("Unauthorized download request blocked. Reason: {} ({})", validationResult.getMessage(), validationResult.getStatus());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpServletResponse.SC_FORBIDDEN);
            error.put("error", "Forbidden");
            error.put("code", validationResult.getStatus().name());
            error.put("message", "Acceso denegado: " + validationResult.getMessage());

            response.getWriter().write(MAPPER.writeValueAsString(error));
            response.getWriter().flush();
            return;
        }

        log.info("Authorized download granted for token [JTI: {}] with tier [{}].",
                validationResult.getTokenId(), validationResult.getTier());

        // 2. Stream Binary Package to Authorized Client
        File projectRoot = findProjectRoot();
        File templateOfflineDir = new File(projectRoot, "template-offline");
        File prebuiltZip = new File(templateOfflineDir, "release/GameShowCenter-Offline-v1.0.0.zip");

        if (prebuiltZip.exists()) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"GameShowCenter-Offline-v1.0.0.zip\"");
            response.setHeader("Content-Length", String.valueOf(prebuiltZip.length()));
            try (InputStream is = new FileInputStream(prebuiltZip);
                 OutputStream os = response.getOutputStream()) {
                is.transferTo(os);
                os.flush();
                return;
            } catch (Exception e) {
                log.warn("Could not stream prebuilt zip, falling back to dynamic bundle: {}", e.getMessage());
            }
        }

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"GameShowCenter-Offline-Bundle.zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            // Instructions
            String instructions = """
                ====================================================
                GAME SHOW CENTER - MODO OFFLINE (Developed by YonIA Enterprise)
                ====================================================

                INSTRUCCIONES DE USO:
                1. Asegurate de tener instalado Java 17 o superior en tu equipo.
                2. Para iniciar el programa, haz doble clic en:
                   'template-offline-1.0.0-standalone.jar'
                   o ejecuta en consola:
                   java -jar template-offline-1.0.0-standalone.jar

                3. Al abrir por primera vez, selecciona 'Examinar Archivo de Licencia (.lic)'
                   e introduce el archivo de licencia comercial (.lic) proporcionado
                   por el administrador para tu suscripcion anual.

                4. La carpeta 'games/' contiene los minijuegos instalados. Puedes
                   agregar o actualizar juegos mediante el gestor de paquetes MegaCore.
                ====================================================
                """;
            zipBytes(zos, "GameShowCenter-Offline/INSTRUCCIONES_DE_USO.txt", instructions.getBytes("UTF-8"));

            // Executable Standalone JAR
            File jarFile = new File(templateOfflineDir, "target/template-offline-1.0.0-standalone.jar");
            if (!jarFile.exists()) {
                jarFile = new File(templateOfflineDir, "target/template-offline-1.0.0.jar");
            }
            if (jarFile.exists()) {
                zipFile(zos, jarFile, "GameShowCenter-Offline/template-offline-1.0.0-standalone.jar");
            }

            // Games Folder (Basic minigames)
            File gamesFolder = new File(templateOfflineDir, "games");
            if (gamesFolder.exists()) {
                zipFolder(zos, gamesFolder, "GameShowCenter-Offline/games");
            }

            // Assets Folder
            File assetsFolder = new File(templateOfflineDir, "assets");
            if (assetsFolder.exists()) {
                zipFolder(zos, assetsFolder, "GameShowCenter-Offline/assets");
            }

            zos.finish();
        } catch (Exception e) {
            log.error("Failed to generate dynamic offline bundle: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private File findProjectRoot() {
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            if (new File(current, "template-offline").exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        return new File(".");
    }

    private void zipBytes(ZipOutputStream zos, String entryName, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private void zipFile(ZipOutputStream zos, File file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        try (InputStream is = new FileInputStream(file)) {
            is.transferTo(zos);
        }
        zos.closeEntry();
    }

    private void zipFolder(ZipOutputStream zos, File folderToZip, String parentPath) throws IOException {
        File[] files = folderToZip.listFiles();
        if (files == null) return;

        for (File file : files) {
            String entryPath = parentPath + "/" + file.getName();
            if (file.isDirectory()) {
                zipFolder(zos, file, entryPath);
            } else {
                zipFile(zos, file, entryPath);
            }
        }
    }
}
