package com.gameshowcenter.offline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.theme.ImageSourcesConfigManager;
import com.gameshowcenter.offline.theme.RagDocumentsConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AIService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static class AIResult {
        private boolean success;
        private String errorCode;
        private String errorMessage;
        private JsonNode generatedData;
        private JsonNode mergedSetup;
        private final List<String> copyrightWarnings = new ArrayList<>();

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public JsonNode getGeneratedData() {
            return generatedData;
        }

        public void setGeneratedData(JsonNode generatedData) {
            this.generatedData = generatedData;
        }

        public JsonNode getMergedSetup() {
            return mergedSetup;
        }

        public void setMergedSetup(JsonNode mergedSetup) {
            this.mergedSetup = mergedSetup;
        }

        public List<String> getCopyrightWarnings() {
            return copyrightWarnings;
        }
    }

    /**
     * Checks if the physical RAM is at least 16 GB (using 13.5 GB threshold to account for hardware/iGPU reserved memory).
     */
    public static boolean hasMinimumMemory() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long totalBytes = osBean.getTotalMemorySize();
            long minRequiredBytes = (long) (13.5 * 1024.0 * 1024.0 * 1024.0);
            return totalBytes >= minRequiredBytes;
        } catch (Throwable t) {
            return true; // Fallback if MXBean unavailable
        }
    }

    /**
     * Returns total system memory in Gigabytes.
     */
    public static double getTotalMemoryGigabytes() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return (double) osBean.getTotalMemorySize() / (1024.0 * 1024.0 * 1024.0);
        } catch (Throwable t) {
            return 0.0;
        }
    }

    /**
     * Tests if there is an active internet connection using fast socket checks.
     */
    public static boolean hasInternetConnection() {
        String[] hosts = {"commons.wikimedia.org", "1.1.1.1", "8.8.8.8"};
        for (String host : hosts) {
            try (Socket socket = new Socket()) {
                int port = host.contains("wikimedia") ? 443 : 53;
                socket.connect(new InetSocketAddress(host, port), 1500);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * Returns true if both requirements (>=16GB RAM and Internet) are met.
     */
    public static boolean isAIAvailable() {
        return hasMinimumMemory() && hasInternetConnection();
    }

    /**
     * Returns a human-readable localized reason why AI is disabled.
     */
    public static String getAIDisabledReason() {
        boolean mem = hasMinimumMemory();
        boolean net = hasInternetConnection();
        if (!mem && !net) {
            return I18n.get("settings.ai.disabled_both", String.format("%.1f", getTotalMemoryGigabytes()));
        } else if (!mem) {
            return I18n.get("settings.ai.disabled_ram", String.format("%.1f", getTotalMemoryGigabytes()));
        } else if (!net) {
            return I18n.get("settings.ai.disabled_net");
        }
        return null;
    }

    @FunctionalInterface
    public interface AIProgressListener {
        void onProgress(int current, int total, double percent, String step);
    }

    /**
     * Asynchronously invokes the Python AI worker script.
     */
    public static CompletableFuture<AIResult> generateGameSetupAsync(
            String gameName,
            String prompt,
            int count,
            int imagesPerRound,
            String mode,
            JsonNode currentSetup) {
        return generateGameSetupAsync(gameName, prompt, count, imagesPerRound, mode, currentSetup, null);
    }

    /**
     * Checks if the given game requires visual/image generation based on its ai_prompt.json configuration.
     */
    public static boolean isGameRequiringImages(String gameName) {
        if (gameName == null || gameName.isBlank()) return false;
        File f1 = new File("template-offline/games/" + gameName + "/ai_prompt.json");
        File f2 = new File("games/" + gameName + "/ai_prompt.json");
        File target = f1.exists() ? f1 : (f2.exists() ? f2 : null);
        if (target != null) {
            try {
                JsonNode root = objectMapper.readTree(target);
                if (root.has("requires_images") && root.get("requires_images").asBoolean()) {
                    return true;
                }
                if (root.has("generation_type")) {
                    String genType = root.get("generation_type").asText();
                    return "multi_image_entities".equalsIgnoreCase(genType) || "single_image_entities".equalsIgnoreCase(genType);
                }
            } catch (Exception ignored) {}
        }
        String norm = gameName.toLowerCase().replaceAll("[^a-z0-9]", "");
        return norm.contains("geolocation") || norm.contains("guesscharacter") || norm.contains("snapsolve");
    }

    /**
     * Asynchronously invokes the Python AI worker script with real-time progress callbacks.
     */
    public static CompletableFuture<AIResult> generateGameSetupAsync(
            String gameName,
            String prompt,
            int count,
            int imagesPerRound,
            String mode,
            JsonNode currentSetup,
            AIProgressListener progressListener) {
        return generateGameSetupAsync(gameName, prompt, count, imagesPerRound, mode, null, currentSetup, progressListener);
    }

    /**
     * Asynchronously invokes the Python AI worker script with image sources and real-time progress callbacks.
     */
    public static CompletableFuture<AIResult> generateGameSetupAsync(
            String gameName,
            String prompt,
            int count,
            int imagesPerRound,
            String mode,
            List<String> imageSources,
            JsonNode currentSetup,
            AIProgressListener progressListener) {

        return CompletableFuture.supplyAsync(() -> {
            AIResult result = new AIResult();

            // Find Python script
            File scriptFile = new File("template-offline/scripts/ai_worker.py");
            if (!scriptFile.exists()) {
                scriptFile = new File("scripts/ai_worker.py");
            }
            if (!scriptFile.exists()) {
                result.setSuccess(false);
                result.setErrorCode("SCRIPT_NOT_FOUND");
                result.setErrorMessage("No se encontró el script de backend 'scripts/ai_worker.py'.");
                return result;
            }

            File tempConfigFile = null;
            try {
                // Write temp config JSON file
                tempConfigFile = File.createTempFile("gsc_ai_req_", ".json");
                ObjectNode reqNode = objectMapper.createObjectNode();
                reqNode.put("game", gameName);
                reqNode.put("prompt", prompt);
                reqNode.put("count", count);
                reqNode.put("images_per_round", imagesPerRound);
                reqNode.put("mode", mode);
                reqNode.put("strictness", ImageSourcesConfigManager.getStrictness());

                ArrayNode sourcesArray = objectMapper.createArrayNode();
                if (imageSources != null && !imageSources.isEmpty()) {
                    for (String src : imageSources) {
                        sourcesArray.add(src);
                    }
                }
                reqNode.set("image_sources", sourcesArray);

                ArrayNode existingArr = objectMapper.createArrayNode();
                if ("add".equalsIgnoreCase(mode) && currentSetup != null) {
                    List<String> items = extractExistingItems(gameName, currentSetup);
                    for (String item : items) {
                        if (item != null && !item.isBlank()) {
                            existingArr.add(item.trim());
                        }
                    }
                }
                reqNode.set("existing_items", existingArr);

                ArrayNode ragDocsArray = objectMapper.createArrayNode();
                for (String doc : RagDocumentsConfigManager.getRagDocuments()) {
                    ragDocsArray.add(doc);
                }
                reqNode.set("rag_documents", ragDocsArray);

                objectMapper.writeValue(tempConfigFile, reqNode);

                // Build Process
                ProcessBuilder pb = new ProcessBuilder(
                        "python",
                        scriptFile.getAbsolutePath(),
                        "--config-file",
                        tempConfigFile.getAbsolutePath()
                );
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.redirectErrorStream(false);

                Process process = pb.start();

                // Read stderr asynchronously to prevent Windows OS pipe buffer deadlocks (64KB limit)
                StringBuilder stderr = new StringBuilder();
                Thread stderrReaderThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stderr.append(line).append("\n");
                        }
                    } catch (Exception ignored) {
                    }
                }, "AIService-Stderr-Reader");
                stderrReaderThread.setDaemon(true);
                stderrReaderThread.start();

                // Read stdout
                StringBuilder stdout = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("PROGRESS:")) {
                            if (progressListener != null) {
                                try {
                                    String jsonPart = line.substring("PROGRESS:".length()).trim();
                                    JsonNode pNode = objectMapper.readTree(jsonPart);
                                    int current = pNode.has("current") ? pNode.get("current").asInt() : 0;
                                    int total = pNode.has("total") ? pNode.get("total").asInt() : count;
                                    double pct = pNode.has("percent") ? pNode.get("percent").asDouble() : (total > 0 ? (double) current / total : 0.0);
                                    String step = pNode.has("step") ? pNode.get("step").asText() : "";
                                    progressListener.onProgress(current, total, pct, step);
                                } catch (Exception ignored) {
                                }
                            }
                        } else {
                            stdout.append(line).append("\n");
                        }
                    }
                }

                int exitCode = process.waitFor();
                try {
                    stderrReaderThread.join(1000);
                } catch (InterruptedException ignored) {}

                String outputStr = stdout.toString().trim();

                if (outputStr.isEmpty()) {
                    result.setSuccess(false);
                    result.setErrorCode("EMPTY_OUTPUT");
                    result.setErrorMessage("El motor de IA no devolvió respuesta. Log: " + stderr.toString().trim());
                    return result;
                }

                if (!outputStr.startsWith("{")) {
                    int idx = outputStr.indexOf("{");
                    if (idx >= 0) {
                        outputStr = outputStr.substring(idx);
                    }
                }

                JsonNode resJson = objectMapper.readTree(outputStr);
                String status = resJson.has("status") ? resJson.get("status").asText() : "error";

                if (!"success".equalsIgnoreCase(status)) {
                    result.setSuccess(false);
                    result.setErrorCode(resJson.has("error_code") ? resJson.get("error_code").asText() : "AI_ERROR");
                    result.setErrorMessage(resJson.has("message") ? resJson.get("message").asText() : "Error durante la generación.");
                    return result;
                }

                // Success!
                result.setSuccess(true);
                JsonNode generatedData = resJson.has("data") ? resJson.get("data") : null;
                result.setGeneratedData(generatedData);

                // Copyright warnings
                if (resJson.has("copyright_warnings") && resJson.get("copyright_warnings").isArray()) {
                    for (JsonNode w : resJson.get("copyright_warnings")) {
                        result.getCopyrightWarnings().add(w.asText());
                    }
                }

                // Merge or Overwrite with currentSetup
                JsonNode merged = mergeSetup(gameName, mode, currentSetup, generatedData, result.getCopyrightWarnings());
                result.setMergedSetup(merged);

                return result;

            } catch (Exception ex) {
                result.setSuccess(false);
                result.setErrorCode("EXECUTION_EXCEPTION");
                result.setErrorMessage("Error de ejecución del proceso de IA: " + ex.getMessage());
                return result;
            } finally {
                if (tempConfigFile != null && tempConfigFile.exists()) {
                    try {
                        tempConfigFile.delete();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    /**
     * Extracts existing metadata identifiers (location names, questions, words, category names, media titles)
     * from the game setup data to visualize them and prevent the AI from generating duplicates or similar items.
     */
    public static List<String> extractExistingItems(String gameName, JsonNode setupNode) {
        List<String> items = new ArrayList<>();
        if (setupNode == null || gameName == null) return items;
        String norm = gameName.toLowerCase().replace(" ", "_");

        if (norm.contains("geo")) {
            JsonNode locs = setupNode.has("locations") ? setupNode.get("locations") : null;
            if (locs != null && locs.isArray()) {
                for (JsonNode n : locs) {
                    if (n.has("location_name")) items.add(n.get("location_name").asText());
                    else if (n.has("locationName")) items.add(n.get("locationName").asText());
                    else if (n.has("name")) items.add(n.get("name").asText());
                }
            }
        } else if (norm.contains("trivia")) {
            JsonNode pool = setupNode.has("questionPool") ? setupNode.get("questionPool") :
                    (setupNode.has("question_pool") ? setupNode.get("question_pool") : null);
            if (pool != null && pool.isArray()) {
                for (JsonNode n : pool) {
                    if (n.has("question")) items.add(n.get("question").asText());
                }
            }
        } else if (norm.contains("hangman")) {
            JsonNode pool = setupNode.has("wordPool") ? setupNode.get("wordPool") :
                    (setupNode.has("word_pool") ? setupNode.get("word_pool") : null);
            if (pool != null && pool.isArray()) {
                for (JsonNode n : pool) {
                    items.add(n.asText());
                }
            }
        } else if (norm.contains("topic")) {
            JsonNode cats = setupNode.has("categories") ? setupNode.get("categories") : null;
            if (cats != null && cats.isArray()) {
                for (JsonNode n : cats) {
                    if (n.has("categoryName")) items.add(n.get("categoryName").asText());
                    else if (n.has("category_name")) items.add(n.get("category_name").asText());
                    else if (n.has("name")) items.add(n.get("name").asText());
                }
            }
        } else if (norm.contains("guess") || norm.contains("snap")) {
            JsonNode pool = setupNode.has("media_pool") ? setupNode.get("media_pool") :
                    (setupNode.has("mediaPool") ? setupNode.get("mediaPool") : null);
            if (pool != null && pool.isArray()) {
                for (JsonNode n : pool) {
                    if (n.has("title")) items.add(n.get("title").asText());
                    else if (n.has("name")) items.add(n.get("name").asText());
                }
            }
        }
        return items;
    }

    /**
     * Merges or overwrites the generated data into the current setup structure.
     */
    public static JsonNode mergeSetup(
            String gameName,
            String mode,
            JsonNode currentSetup,
            JsonNode generatedData,
            List<String> copyrightWarnings) {

        if (generatedData == null) {
            return currentSetup;
        }

        boolean overwrite = "overwrite".equalsIgnoreCase(mode) || currentSetup == null;
        ObjectNode root = (currentSetup != null && !overwrite) ? currentSetup.deepCopy() : objectMapper.createObjectNode();

        String norm = gameName.toLowerCase().replace(" ", "_");

        // Merge copyright warnings into setup if any
        if (copyrightWarnings != null && !copyrightWarnings.isEmpty()) {
            ArrayNode warnArr = root.has("copyright_warnings") && root.get("copyright_warnings").isArray()
                    ? (ArrayNode) root.get("copyright_warnings")
                    : root.putArray("copyright_warnings");
            Set<String> existing = new HashSet<>();
            for (JsonNode n : warnArr) existing.add(n.asText());
            for (String w : copyrightWarnings) {
                if (!existing.contains(w)) {
                    warnArr.add(w);
                    existing.add(w);
                }
            }
        }

        // 1. Trivia Quiz
        if (norm.contains("trivia")) {
            root.put("game", "Trivia_Quiz");
            ArrayNode newPool = generatedData.has("questionPool") ? (ArrayNode) generatedData.get("questionPool") : null;
            if (newPool != null) {
                if (overwrite || !root.has("questionPool") || !root.get("questionPool").isArray()) {
                    root.set("questionPool", newPool.deepCopy());
                    root.set("question_pool", newPool.deepCopy());
                } else {
                    ArrayNode existingArr = (ArrayNode) root.get("questionPool");
                    for (JsonNode q : newPool) {
                        existingArr.add(q.deepCopy());
                    }
                    root.set("question_pool", existingArr.deepCopy());
                }
            }
        }

        // 2. Hangman (Guarantee strictly unique, non-repeating words)
        else if (norm.contains("hangman")) {
            root.put("game", "Hangman");
            ArrayNode newWords = generatedData.has("wordPool") ? (ArrayNode) generatedData.get("wordPool") : null;
            if (newWords != null) {
                Set<String> seen = new HashSet<>();
                ArrayNode targetArr = objectMapper.createArrayNode();
                if (!overwrite && root.has("wordPool") && root.get("wordPool").isArray()) {
                    for (JsonNode existing : root.get("wordPool")) {
                        String normW = existing.asText().trim().toUpperCase();
                        if (!normW.isEmpty() && seen.add(normW)) {
                            targetArr.add(normW);
                        }
                    }
                }
                for (JsonNode w : newWords) {
                    String normW = w.asText().trim().toUpperCase();
                    if (!normW.isEmpty() && seen.add(normW)) {
                        targetArr.add(normW);
                    }
                }
                root.set("wordPool", targetArr.deepCopy());
                root.set("word_pool", targetArr.deepCopy());
            }
        }

        // 3. Topic Takedown
        else if (norm.contains("topic")) {
            root.put("game", "Topic_Takedown");
            if (generatedData.has("categories") && generatedData.get("categories").isArray()) {
                ArrayNode newCats = (ArrayNode) generatedData.get("categories");
                if (overwrite || !root.has("categories") || !root.get("categories").isArray()) {
                    root.set("categories", newCats.deepCopy());
                    root.put("numCategories", newCats.size());
                    root.put("num_categories", newCats.size());
                    root.put("questionsPerCategory", 4);
                    root.put("questions_per_category", 4);
                } else {
                    ArrayNode existingCats = (ArrayNode) root.get("categories");
                    for (JsonNode cat : newCats) {
                        if (existingCats.size() < 6) {
                            existingCats.add(cat.deepCopy());
                        }
                    }
                    root.put("numCategories", existingCats.size());
                    root.put("num_categories", existingCats.size());
                }
            }
        }

        // 4. GeoLocation
        else if (norm.contains("geo")) {
            root.put("game", "GeoLocation");
            if (generatedData.has("images_per_round")) {
                root.put("images_per_round", generatedData.get("images_per_round").asInt());
            }
            if (generatedData.has("locations") && generatedData.get("locations").isArray()) {
                ArrayNode newLocs = (ArrayNode) generatedData.get("locations");
                if (overwrite || !root.has("locations") || !root.get("locations").isArray()) {
                    root.set("locations", newLocs.deepCopy());
                } else {
                    ArrayNode existingLocs = (ArrayNode) root.get("locations");
                    for (JsonNode loc : newLocs) {
                        existingLocs.add(loc.deepCopy());
                    }
                }
            }
        }

        // 5. Guess Character
        else if (norm.contains("guess")) {
            root.put("game", "Guess_Character");
            if (generatedData.has("media_pool") && generatedData.get("media_pool").isArray()) {
                ArrayNode newMedia = (ArrayNode) generatedData.get("media_pool");
                if (overwrite || !root.has("media_pool") || !root.get("media_pool").isArray()) {
                    root.set("media_pool", newMedia.deepCopy());
                } else {
                    ArrayNode existingMedia = (ArrayNode) root.get("media_pool");
                    for (JsonNode m : newMedia) {
                        existingMedia.add(m.deepCopy());
                    }
                }
            }
        }

        // 6. Snap Solve
        else if (norm.contains("snap")) {
            root.put("game", "Snap_Solve");
            if (!root.has("selected_filters")) {
                ArrayNode filters = root.putArray("selected_filters");
                filters.add("displacement").add("swirl").add("pixelate").add("blur");
            }
            if (generatedData.has("media_pool") && generatedData.get("media_pool").isArray()) {
                ArrayNode newMedia = (ArrayNode) generatedData.get("media_pool");
                if (overwrite || !root.has("media_pool") || !root.get("media_pool").isArray()) {
                    root.set("media_pool", newMedia.deepCopy());
                } else {
                    ArrayNode existingMedia = (ArrayNode) root.get("media_pool");
                    for (JsonNode m : newMedia) {
                        existingMedia.add(m.deepCopy());
                    }
                }
            }
        }

        // 7. TimeLine
        else if (norm.contains("timeline") || norm.contains("linea")) {
            root.put("game", "TimeLine");
            if (generatedData.has("events") && generatedData.get("events").isArray()) {
                ArrayNode newEvents = (ArrayNode) generatedData.get("events");
                if (overwrite || !root.has("events") || !root.get("events").isArray()) {
                    root.set("events", newEvents.deepCopy());
                } else {
                    ArrayNode existingEvents = (ArrayNode) root.get("events");
                    for (JsonNode ev : newEvents) {
                        existingEvents.add(ev.deepCopy());
                    }
                }
            }
        }

        // Automatically purge unreferenced images to keep disk clean (Requirement 4)
        purgeOrphanImages(gameName, root);

        return root;
    }

    /**
     * Purges orphaned or unused generated images (ai_*.jpg/png) from the game's images folder
     * that are no longer referenced in the active setup JSON.
     */
    public static void purgeOrphanImages(String gameName, JsonNode setupData) {
        if (gameName == null || setupData == null) return;
        try {
            Set<String> usedFilenames = new HashSet<>();
            collectReferencedImages(setupData, usedFilenames);

            List<File> imageDirs = List.of(
                    new File("template-offline/games/" + gameName + "/images"),
                    new File("games/" + gameName + "/images")
            );

            for (File dir : imageDirs) {
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            String name = f.getName().toLowerCase();
                            // Only purge generated AI images (starting with ai_) to protect original bundled game assets
                            if (name.startsWith("ai_") && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
                                if (!usedFilenames.contains(name)) {
                                    try {
                                        f.delete();
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Warning purging orphan images for " + gameName + ": " + ex.getMessage());
        }
    }

    private static void collectReferencedImages(JsonNode node, Set<String> outSet) {
        if (node == null) return;
        if (node.isTextual()) {
            String text = node.asText().trim();
            if (text.contains("/") || text.contains("\\") || text.endsWith(".jpg") || text.endsWith(".png") || text.endsWith(".jpeg")) {
                String filename = new File(text).getName().toLowerCase();
                outSet.add(filename);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                collectReferencedImages(item, outSet);
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> collectReferencedImages(entry.getValue(), outSet));
        }
    }
}
