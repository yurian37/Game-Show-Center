package com.gameshowcenter.backend.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/download")
@CrossOrigin(origins = "*")
public class DownloadController {

    @GetMapping("/offline-template")
    public void downloadOfflineTemplate(HttpServletResponse response) {
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
                // fallback to dynamic packaging
            }
        }

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"GameShowCenter-Offline-Bundle.zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            // 1. Add Instructions File
            String instructions = """
                ====================================================
                GAME SHOW CENTER - MODO OFFLINE (Developed by YuyiStudio)
                ====================================================

                INSTRUCCIONES DE USO:
                1. Asegúrate de tener instalado Java 17 o superior en tu equipo.
                2. Para iniciar el programa, haz doble clic en:
                   'template-offline-1.0.0-standalone.jar'
                   o ejecuta en consola:
                   java -jar template-offline-1.0.0-standalone.jar

                3. Al abrir por primera vez, ingresa tu clave de licencia.
                   (Clave de Prueba / Maestra: YUYI-STUDIO-PRO-2026)

                4. La carpeta 'games/' contiene los 5 juegos básicos. Puedes
                   agregar o descargar más juegos pegando su carpeta en 'games/'.
                ====================================================
                """;
            zipBytes(zos, "GameShowCenter-Offline/INSTRUCCIONES_DE_USO.txt", instructions.getBytes("UTF-8"));

            // 2. Add Executable Standalone JAR
            File jarFile = new File(templateOfflineDir, "target/template-offline-1.0.0-standalone.jar");
            if (!jarFile.exists()) {
                jarFile = new File(templateOfflineDir, "target/template-offline-1.0.0.jar");
            }
            if (jarFile.exists()) {
                zipFile(zos, jarFile, "GameShowCenter-Offline/template-offline-1.0.0-standalone.jar");
            }

            // 3. Add Games Folder (Contains the 5 basic minigames)
            File gamesFolder = new File(templateOfflineDir, "games");
            if (gamesFolder.exists()) {
                zipFolder(zos, gamesFolder, "GameShowCenter-Offline/games");
            }

            // 4. Add Assets Folder
            File assetsFolder = new File(templateOfflineDir, "assets");
            if (assetsFolder.exists()) {
                zipFolder(zos, assetsFolder, "GameShowCenter-Offline/assets");
            }

            zos.finish();
        } catch (Exception e) {
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
