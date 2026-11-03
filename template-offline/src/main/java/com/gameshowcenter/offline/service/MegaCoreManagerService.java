package com.gameshowcenter.offline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MegaCoreManagerService {

    private static final MegaCoreManagerService INSTANCE = new MegaCoreManagerService();
    private final ObjectMapper mapper = new ObjectMapper();

    public static final List<String> MEGACORE_1_GAMES = List.of(
            "TimeLine", "Hangman", "TicTacToe", "Trivia Quiz", "Zero Margin"
    );

    public static final List<String> MEGACORE_2_GAMES = List.of(
            "GeoLocation", "Guess Character", "Rapid Rhythm", "Snap Solve", "Topic Takedown"
    );

    // Security & Anti-Zip Bomb Constraints (SOC 2 A1.1 / ISO 27001:2022 A.8.6)
    public static final long MAX_TOTAL_UNCOMPRESSED_BYTES = 500 * 1024 * 1024L; // 500 MB
    public static final int MAX_TOTAL_ENTRIES = 2_000;
    public static final double MAX_COMPRESSION_RATIO = 100.0;

    public static MegaCoreManagerService getInstance() {
        return INSTANCE;
    }

    public static class InstallResult {
        private final boolean success;
        private final int coreNumber;
        private final String title;
        private final List<String> installedGames;
        private final int assetsExtracted;
        private final String message;

        public InstallResult(boolean success, int coreNumber, String title, List<String> installedGames, String message) {
            this(success, coreNumber, title, installedGames, 0, message);
        }

        public InstallResult(boolean success, int coreNumber, String title, List<String> installedGames, int assetsExtracted, String message) {
            this.success = success;
            this.coreNumber = coreNumber;
            this.title = title;
            this.installedGames = installedGames != null ? installedGames : new ArrayList<>();
            this.assetsExtracted = assetsExtracted;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public int getCoreNumber() { return coreNumber; }
        public String getTitle() { return title; }
        public List<String> getInstalledGames() { return installedGames; }
        public int getAssetsExtracted() { return assetsExtracted; }
        public String getMessage() { return message; }
    }

    public static class InstalledActivity {
        private final String name;
        private final String folderName;
        private final boolean hasSetup;
        private final boolean hasDescription;

        public InstalledActivity(String name, String folderName, boolean hasSetup, boolean hasDescription) {
            this.name = name;
            this.folderName = folderName;
            this.hasSetup = hasSetup;
            this.hasDescription = hasDescription;
        }

        public String getName() { return name; }
        public String getFolderName() { return folderName; }
        public boolean isHasSetup() { return hasSetup; }
        public boolean isHasDescription() { return hasDescription; }
    }

    public static class MegaCoreStatus {
        private final int number;
        private final String title;
        private final List<String> expectedGames;
        private final List<String> installedGames;

        public MegaCoreStatus(int number, String title, List<String> expectedGames, List<String> installedGames) {
            this.number = number;
            this.title = title;
            this.expectedGames = expectedGames;
            this.installedGames = installedGames;
        }

        public int getNumber() { return number; }
        public String getTitle() { return title; }
        public List<String> getExpectedGames() { return expectedGames; }
        public List<String> getInstalledGames() { return installedGames; }
        public boolean isFullyInstalled() { return installedGames.size() >= expectedGames.size(); }
        public int getInstalledCount() { return installedGames.size(); }
        public int getTotalCount() { return expectedGames.size(); }
    }

    /**
     * Resolves the primary games directory based on current runtime environment.
     */
    public File resolveGamesDir() {
        File directGames = new File("games");
        if (directGames.exists() && directGames.isDirectory()) {
            return directGames;
        }

        File repoGames = new File("template-offline", "games");
        if (repoGames.exists() && repoGames.isDirectory()) {
            return repoGames;
        }

        // If inside template-offline, always use ./games
        if (new File("pom.xml").exists() || new File("src").exists()) {
            directGames.mkdirs();
            return directGames;
        }

        directGames.mkdirs();
        return directGames;
    }

    /**
     * Resolves the application assets directory based on current runtime environment.
     */
    public File resolveAssetsDir() {
        File directAssets = new File("assets");
        if (directAssets.exists() && directAssets.isDirectory()) {
            return directAssets;
        }

        File repoAssets = new File("template-offline", "assets");
        if (repoAssets.exists() && repoAssets.isDirectory()) {
            return repoAssets;
        }

        File gamesParentAssets = new File(resolveGamesDir().getParentFile(), "assets");
        if (gamesParentAssets.exists() && gamesParentAssets.isDirectory()) {
            return gamesParentAssets;
        }

        directAssets.mkdirs();
        return directAssets;
    }

    /**
     * Resolves the directory containing megacore zip packages.
     */
    public File resolveMegacoresDir() {
        File[] candidates = new File[]{
                new File("megacores"),
                new File("release", "megacores"),
                new File("template-offline", "megacores"),
                new File("template-offline/release", "megacores"),
                new File("release/GameShowCenter-Cascara", "megacores")
        };
        for (File c : candidates) {
            if (c.exists() && c.isDirectory()) {
                File[] zips = c.listFiles((d, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".zip") || lower.endsWith(".megacore");
                });
                if (zips != null && zips.length > 0) {
                    return c;
                }
            }
        }
        for (File c : candidates) {
            if (c.exists() && c.isDirectory()) {
                return c;
            }
        }
        File def = new File("megacores");
        def.mkdirs();
        return def;
    }

    /**
     * Inspects a zip file and returns the list of games contained inside.
     */
    public List<String> inspectPackageGames(File zipFile) {
        List<String> games = new ArrayList<>();
        if (zipFile == null || !zipFile.exists()) return games;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            Set<String> discovered = new LinkedHashSet<>();
            byte[] buf = new byte[4096];

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/').trim();
                while (name.startsWith("/")) name = name.substring(1);

                String[] segs = name.split("/");
                while (segs.length > 1) {
                    String first = segs[0].toLowerCase();
                    if (first.startsWith("megacore") || first.equals("megacores")) {
                        name = name.substring(segs[0].length() + 1);
                        while (name.startsWith("/")) name = name.substring(1);
                        segs = name.split("/");
                    } else {
                        break;
                    }
                }

                if (name.toLowerCase().startsWith("games/")) {
                    name = name.substring("games/".length());
                    while (name.startsWith("/")) name = name.substring(1);
                    segs = name.split("/");
                }

                if (name.equalsIgnoreCase("megacore.json") || name.endsWith("/megacore.json")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int l;
                    while ((l = zis.read(buf)) > 0) baos.write(buf, 0, l);
                    try {
                        JsonNode root = mapper.readTree(baos.toByteArray());
                        if (root.has("games") && root.get("games").isArray()) {
                            for (JsonNode g : root.get("games")) {
                                discovered.add(g.asText());
                            }
                        }
                    } catch (Exception ignored) {}
                } else if (!name.toLowerCase().startsWith("assets/") && segs.length > 0 && !segs[0].isBlank()) {
                    if (!segs[0].equalsIgnoreCase("assets") && !segs[0].equalsIgnoreCase("megacore.json")) {
                        discovered.add(segs[0]);
                    }
                }
                zis.closeEntry();
            }
            games.addAll(discovered);
        } catch (Exception ignored) {}
        return games;
    }

    /**
     * Returns the list of currently installed activities/minigames found in gamesDir.
     */
    public List<InstalledActivity> getInstalledActivities(File gamesDir) {
        if (gamesDir == null) gamesDir = resolveGamesDir();
        List<InstalledActivity> list = new ArrayList<>();
        if (!gamesDir.exists() || !gamesDir.isDirectory()) return list;

        File[] subdirs = gamesDir.listFiles(File::isDirectory);
        if (subdirs == null) return list;

        Arrays.sort(subdirs, Comparator.comparing(File::getName));
        for (File dir : subdirs) {
            if (dir.getName().equalsIgnoreCase("assets") || dir.getName().startsWith(".")) continue;
            boolean hasSetup = new File(dir, "setup.json").exists();
            File descFile = new File(dir, "description.json");
            boolean hasDesc = descFile.exists();
            String displayName = dir.getName();
            if (hasDesc) {
                try {
                    JsonNode node = mapper.readTree(descFile);
                    if (node.has("name") && !node.get("name").asText().isBlank()) {
                        displayName = node.get("name").asText();
                    }
                } catch (Exception ignored) {}
            }
            list.add(new InstalledActivity(displayName, dir.getName(), hasSetup, hasDesc));
        }
        return list;
    }

    /**
     * Checks if a specific MegaCore package is already fully installed in gamesDir.
     */
    public boolean isMegaCoreFullyInstalled(int coreNumber, File gamesDir) {
        List<MegaCoreStatus> statuses = getCatalogStatus(gamesDir);
        for (MegaCoreStatus s : statuses) {
            if (s.getNumber() == coreNumber) {
                return s.isFullyInstalled();
            }
        }
        return false;
    }

    /**
     * Uncompresses and installs a MegaCore zip/package into the games directory and assets directory.
     * Automatically extracts games into gamesDir and linked game assets into assetsDir.
     * Accurately reports the names of all games included in the loaded package.
     */
    public InstallResult installPackage(File packageFile, File gamesDir) {
        if (packageFile == null || !packageFile.exists()) {
            return new InstallResult(false, 0, "Desconocido", null, 0, "El archivo de paquete no existe.");
        }

        if (gamesDir == null) {
            gamesDir = resolveGamesDir();
        }
        if (!gamesDir.exists()) {
            gamesDir.mkdirs();
        }

        File assetsDir = resolveAssetsDir();
        if (!assetsDir.exists()) {
            assetsDir.mkdirs();
        }

        Path gamesPath = gamesDir.toPath().toAbsolutePath().normalize();
        Path assetsPath = assetsDir.toPath().toAbsolutePath().normalize();
        Set<String> discoveredGameFolders = new LinkedHashSet<>();
        List<String> manifestGames = new ArrayList<>();
        int detectedCoreNumber = 0;
        String detectedTitle = null;
        int assetsExtractedCount = 0;
        long totalUncompressedBytes = 0;
        int totalEntriesCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(packageFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                totalEntriesCount++;
                if (totalEntriesCount > MAX_TOTAL_ENTRIES) {
                    System.err.println("Security Alert: Zip Bomb detected. File contains > " + MAX_TOTAL_ENTRIES + " entries.");
                    return new InstallResult(false, 0, null, List.of(),
                            "Security Alert: Bloqueo Anti-Zip Bomb. El paquete contiene más de " + MAX_TOTAL_ENTRIES + " entradas.");
                }

                String rawName = entry.getName();
                if (rawName == null || rawName.isBlank()) {
                    zis.closeEntry();
                    continue;
                }

                // Check anomaly in compression ratio if reported
                if (entry.getSize() > 0 && entry.getCompressedSize() > 0) {
                    double ratio = (double) entry.getSize() / entry.getCompressedSize();
                    if (ratio > MAX_COMPRESSION_RATIO) {
                        System.err.println("Security Alert: Abnormal compression ratio (" + String.format(Locale.US, "%.1f", ratio) + ":1) on entry: " + rawName);
                        return new InstallResult(false, 0, null, List.of(),
                                "Security Alert: Bloqueo Anti-Zip Bomb. Ratio de compresión anómalo detectado en: " + rawName);
                    }
                }

                // Standardize slashes
                String entryName = rawName.replace('\\', '/').trim();
                while (entryName.startsWith("/")) {
                    entryName = entryName.substring(1);
                }

                // Strip wrapper folders like "MegaCore-1/", "MegaCore 1/", "megacores/"
                String[] segments = entryName.split("/");
                while (segments.length > 1) {
                    String first = segments[0].toLowerCase().trim();
                    if (first.startsWith("megacore") || first.equals("megacores")) {
                        entryName = entryName.substring(segments[0].length() + 1);
                        while (entryName.startsWith("/")) {
                            entryName = entryName.substring(1);
                        }
                        segments = entryName.split("/");
                    } else {
                        break;
                    }
                }

                // If games/ is top prefix, strip it so game folder is at top level
                if (entryName.toLowerCase().startsWith("games/")) {
                    entryName = entryName.substring("games/".length());
                    while (entryName.startsWith("/")) {
                        entryName = entryName.substring(1);
                    }
                    segments = entryName.split("/");
                }

                if (entryName.isBlank()) {
                    zis.closeEntry();
                    continue;
                }

                // Read megacore manifest if present
                if (entryName.equalsIgnoreCase("megacore.json") || entryName.endsWith("/megacore.json")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    try {
                        JsonNode root = mapper.readTree(baos.toByteArray());
                        if (root.has("megacoreNumber")) detectedCoreNumber = root.get("megacoreNumber").asInt();
                        else if (root.has("coreNumber")) detectedCoreNumber = root.get("coreNumber").asInt();
                        if (root.has("title")) detectedTitle = root.get("title").asText();
                        if (root.has("games") && root.get("games").isArray()) {
                            for (JsonNode g : root.get("games")) {
                                manifestGames.add(g.asText());
                            }
                        }
                    } catch (Exception ignored) {}
                    zis.closeEntry();
                    continue;
                }

                // Support single-game zip containing description.json / setup.json directly at root
                if (segments.length == 1 && (entryName.equalsIgnoreCase("description.json") || entryName.equalsIgnoreCase("setup.json"))) {
                    String fallbackGame = packageFile.getName();
                    if (fallbackGame.contains(".")) {
                        fallbackGame = fallbackGame.substring(0, fallbackGame.lastIndexOf('.'));
                    }
                    entryName = fallbackGame + "/" + entryName;
                    segments = entryName.split("/");
                }

                boolean isAsset = entryName.toLowerCase().startsWith("assets/");
                Path targetBase = isAsset ? assetsPath : gamesPath;
                String relativePath = isAsset ? entryName.substring("assets/".length()) : entryName;
                while (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                if (relativePath.isBlank()) {
                    zis.closeEntry();
                    continue;
                }

                Path targetPath = targetBase.resolve(relativePath).normalize();

                // Security check: Prevent Zip Slip path traversal
                if (!targetPath.startsWith(targetBase)) {
                    zis.closeEntry();
                    continue;
                }

                boolean isDir = entry.isDirectory() || entryName.endsWith("/");
                if (isDir) {
                    Files.createDirectories(targetPath);
                } else {
                    Path parent = targetPath.getParent();
                    if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    if (Files.isDirectory(targetPath)) {
                        zis.closeEntry();
                        continue;
                    }

                    try (OutputStream fos = Files.newOutputStream(targetPath)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            totalUncompressedBytes += len;
                            if (totalUncompressedBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                                System.err.println("Security Alert: Zip Bomb detected. Uncompressed payload > " + (MAX_TOTAL_UNCOMPRESSED_BYTES / (1024 * 1024)) + " MB.");
                                return new InstallResult(false, 0, null, List.of(),
                                        "Security Alert: Bloqueo Anti-Zip Bomb. El tamaño total extraído supera el límite de seguridad (500 MB).");
                            }
                            fos.write(buffer, 0, len);
                        }
                    } catch (IOException ioe) {
                        System.err.println("Advertencia al extraer " + targetPath.getFileName() + ": " + ioe.getMessage());
                    }

                    if (isAsset) {
                        assetsExtractedCount++;
                    }
                }

                // Detect top-level game directory
                if (!isAsset) {
                    String[] finalParts = entryName.split("/");
                    if (finalParts.length > 0 && !finalParts[0].isBlank() &&
                            !finalParts[0].equalsIgnoreCase("megacore.json") &&
                            !finalParts[0].equalsIgnoreCase("assets")) {
                        discoveredGameFolders.add(finalParts[0]);
                    }
                }

                zis.closeEntry();
            }

            // Determine final list of installed games
            Set<String> finalGamesSet = new LinkedHashSet<>();
            if (!manifestGames.isEmpty()) {
                finalGamesSet.addAll(manifestGames);
            }
            finalGamesSet.addAll(discoveredGameFolders);
            List<String> installedList = new ArrayList<>(finalGamesSet);

            // Deduce MegaCore number from filename or discovered games if not in manifest
            if (detectedCoreNumber == 0) {
                String fileName = packageFile.getName().toLowerCase();
                if (fileName.contains("megacore-1") || fileName.contains("megacore_1") || fileName.contains("megacore1") || fileName.contains("megacore 1")) {
                    detectedCoreNumber = 1;
                    detectedTitle = "MegaCore 1";
                } else if (fileName.contains("megacore-2") || fileName.contains("megacore_2") || fileName.contains("megacore2") || fileName.contains("megacore 2")) {
                    detectedCoreNumber = 2;
                    detectedTitle = "MegaCore 2";
                }
            }

            if (detectedTitle == null || detectedTitle.isBlank()) {
                String baseFileName = packageFile.getName();
                if (baseFileName.contains(".")) {
                    baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf('.'));
                }
                detectedTitle = baseFileName;
            }

            String msg = String.format("¡%s cargado con éxito! Se han instalado las siguientes actividades: %s.",
                    detectedTitle, String.join(", ", installedList));
            if (assetsExtractedCount > 0) {
                msg += String.format(" Se han integrado %d recursos y assets requeridos.", assetsExtractedCount);
            }

            return new InstallResult(true, detectedCoreNumber, detectedTitle, installedList, assetsExtractedCount, msg);

        } catch (Exception e) {
            return new InstallResult(false, 0, "Error", null, 0, "Fallo al descomprimir el paquete MegaCore: " + e.getMessage());
        }
    }

    /**
     * Automatically inspects a megacores/ directory and uncompresses any packages into games/.
     * Fully generic: supports any MegaCore (MegaCore 1, 2, 3, 4, 5...) and avoids redundant uncompression.
     */
    public int autoScanAndInstallPacks(File megacoresDir, File gamesDir) {
        if (megacoresDir == null || !megacoresDir.exists() || !megacoresDir.isDirectory()) {
            megacoresDir = resolveMegacoresDir();
        }
        if (gamesDir == null || !gamesDir.exists()) {
            gamesDir = resolveGamesDir();
        }

        File[] files = megacoresDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".zip") || lower.endsWith(".megacore");
        });

        if (files == null || files.length == 0) {
            return 0;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));

        int installedPacks = 0;
        for (File zipFile : files) {
            List<String> gamesInZip = inspectPackageGames(zipFile);
            boolean allPresent = !gamesInZip.isEmpty();
            for (String g : gamesInZip) {
                File targetGame = new File(gamesDir, g);
                if (!targetGame.exists() || !targetGame.isDirectory()) {
                    allPresent = false;
                    break;
                }
            }

            if (allPresent) {
                installedPacks++;
                continue;
            }

            InstallResult res = installPackage(zipFile, gamesDir);
            if (res.isSuccess()) {
                installedPacks++;
            }
        }
        return installedPacks;
    }

    /**
     * Legacy status method maintained for test suite and compatibility.
     */
    public List<MegaCoreStatus> getCatalogStatus(File gamesDir) {
        if (gamesDir == null) {
            gamesDir = resolveGamesDir();
        }

        List<MegaCoreStatus> list = new ArrayList<>();
        Set<String> activeFolderNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        if (gamesDir.exists() && gamesDir.isDirectory()) {
            File[] subs = gamesDir.listFiles(File::isDirectory);
            if (subs != null) {
                for (File f : subs) {
                    activeFolderNames.add(f.getName());
                }
            }
        }

        // MegaCore 1
        List<String> core1Installed = new ArrayList<>();
        for (String expected : MEGACORE_1_GAMES) {
            if (isGamePresent(expected, activeFolderNames)) {
                core1Installed.add(expected);
            }
        }
        list.add(new MegaCoreStatus(1, "MegaCore 1", MEGACORE_1_GAMES, core1Installed));

        // MegaCore 2
        List<String> core2Installed = new ArrayList<>();
        for (String expected : MEGACORE_2_GAMES) {
            if (isGamePresent(expected, activeFolderNames)) {
                core2Installed.add(expected);
            }
        }
        list.add(new MegaCoreStatus(2, "MegaCore 2", MEGACORE_2_GAMES, core2Installed));

        return list;
    }

    private boolean isGamePresent(String gameName, Set<String> activeFolders) {
        if (activeFolders.contains(gameName)) return true;
        // Aliases check
        if (gameName.equalsIgnoreCase("Snap Solve") && activeFolders.contains("Snap")) return true;
        if (gameName.equalsIgnoreCase("Snap") && activeFolders.contains("Snap Solve")) return true;
        if (gameName.equalsIgnoreCase("Rapid Rhythm") && (activeFolders.contains("Rapid Rhytim") || activeFolders.contains("Rapid_Rhythm"))) return true;
        return false;
    }

    private boolean isCore1Game(String folderName) {
        for (String g : MEGACORE_1_GAMES) {
            if (g.equalsIgnoreCase(folderName)) return true;
        }
        return false;
    }

    private boolean isCore2Game(String folderName) {
        for (String g : MEGACORE_2_GAMES) {
            if (g.equalsIgnoreCase(folderName)) return true;
        }
        if (folderName.equalsIgnoreCase("Snap") || folderName.equalsIgnoreCase("Rapid Rhytim")) return true;
        return false;
    }
}
