package com.gameshowcenter.offline.theme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the AI image source configuration for Game Show Center.
 * Supports:
 * - Toggle for enabling/disabling internet search.
 * - 5 fixed, immutable internet repositories (Wikimedia Commons, Wikipedia, Freerange Stock, Openverse, NASA Images).
 * - Up to 10 user-selected local folders.
 * - Up to 2 school-defined custom web URLs.
 * Automatically persists to preferences.json.
 */
public class ImageSourcesConfigManager {

    public static final int MAX_LOCAL_FOLDERS = 10;
    public static final int MAX_SCHOOL_URLS = 2;

    public record FixedInternetSource(String id, String name, String url, String description) {}

    public static final List<FixedInternetSource> FIXED_INTERNET_SOURCES = List.of(
            new FixedInternetSource("wikimedia", "Wikimedia Commons", "https://commons.wikimedia.org/", "Base de datos global multimedia libre de Wikimedia"),
            new FixedInternetSource("wikipedia", "Wikipedia (PageImages)", "https://wikipedia.org/", "Fotografías oficiales de artículos enciclopédicos"),
            new FixedInternetSource("freerangestock", "Freerange Stock", "https://freerangestock.com/", "Banco de fotos libres y de dominio público"),
            new FixedInternetSource("openverse", "Openverse", "https://openverse.org/", "Buscador de más de 700M de imágenes Creative Commons"),
            new FixedInternetSource("nasa", "NASA Images & Media", "https://images.nasa.gov/", "Archivo oficial de imágenes espaciales, astronómicas y científicas")
    );

    private static boolean internetSearchEnabled = true;
    private static int strictness = 5; // Base 10 scale (1 = low strictness / NSFW only, 10 = extremely strict)
    private static final List<String> localFolders = new ArrayList<>();
    private static final List<String> schoolUrls = new ArrayList<>();

    private static final File PREFS_FILE = new File("preferences.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        loadPreferences();
    }

    public static synchronized void loadPreferences() {
        File f = PREFS_FILE;
        if (!f.exists()) {
            f = new File("template-offline/preferences.json");
        }
        if (!f.exists()) {
            return;
        }
        try {
            JsonNode root = MAPPER.readTree(f);
            if (root.has("image_sources")) {
                JsonNode isNode = root.get("image_sources");
                if (isNode.has("internet_search_enabled")) {
                    internetSearchEnabled = isNode.get("internet_search_enabled").asBoolean(true);
                }
                if (isNode.has("strictness")) {
                    strictness = Math.max(1, Math.min(10, isNode.get("strictness").asInt(5)));
                }
                if (isNode.has("local_folders") && isNode.get("local_folders").isArray()) {
                    localFolders.clear();
                    for (JsonNode fn : isNode.get("local_folders")) {
                        if (fn.isTextual() && localFolders.size() < MAX_LOCAL_FOLDERS) {
                            String path = fn.asText().trim();
                            if (!path.isEmpty() && !localFolders.contains(path)) {
                                localFolders.add(path);
                            }
                        }
                    }
                }
                if (isNode.has("school_urls") && isNode.get("school_urls").isArray()) {
                    schoolUrls.clear();
                    for (JsonNode un : isNode.get("school_urls")) {
                        if (un.isTextual() && schoolUrls.size() < MAX_SCHOOL_URLS) {
                            String url = un.asText().trim();
                            if (!url.isEmpty() && !schoolUrls.contains(url)) {
                                schoolUrls.add(url);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static synchronized void savePreferences() {
        try {
            File f = PREFS_FILE;
            if (!f.exists() && new File("template-offline/preferences.json").exists()) {
                f = new File("template-offline/preferences.json");
            }
            ObjectNode root = f.exists() ? (ObjectNode) MAPPER.readTree(f) : MAPPER.createObjectNode();

            ObjectNode isNode = MAPPER.createObjectNode();
            isNode.put("internet_search_enabled", internetSearchEnabled);
            isNode.put("strictness", strictness);

            ArrayNode foldersArr = MAPPER.createArrayNode();
            for (String folder : localFolders) {
                foldersArr.add(folder);
            }
            isNode.set("local_folders", foldersArr);

            ArrayNode urlsArr = MAPPER.createArrayNode();
            for (String url : schoolUrls) {
                urlsArr.add(url);
            }
            isNode.set("school_urls", urlsArr);

            root.set("image_sources", isNode);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(f, root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static synchronized int getStrictness() {
        return strictness;
    }

    public static synchronized void setStrictness(int value) {
        strictness = Math.max(1, Math.min(10, value));
        savePreferences();
    }

    public static synchronized boolean isInternetSearchEnabled() {
        return internetSearchEnabled;
    }

    public static synchronized void setInternetSearchEnabled(boolean enabled) {
        internetSearchEnabled = enabled;
        savePreferences();
    }

    public static synchronized List<String> getLocalFolders() {
        return Collections.unmodifiableList(new ArrayList<>(localFolders));
    }

    public static synchronized boolean addLocalFolder(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) return false;
        String clean = folderPath.trim();
        if (localFolders.size() >= MAX_LOCAL_FOLDERS) return false;
        if (localFolders.contains(clean)) return false;
        localFolders.add(clean);
        savePreferences();
        return true;
    }

    public static synchronized boolean addFolder(String folderPath) {
        return addLocalFolder(folderPath);
    }

    public static synchronized boolean removeLocalFolder(String folderPath) {
        if (folderPath == null) return false;
        boolean removed = localFolders.remove(folderPath.trim());
        if (removed) {
            savePreferences();
        }
        return removed;
    }

    public static synchronized List<String> getSchoolUrls() {
        return Collections.unmodifiableList(new ArrayList<>(schoolUrls));
    }

    public static synchronized boolean addSchoolUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String clean = url.trim();
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://" + clean;
        }
        if (schoolUrls.size() >= MAX_SCHOOL_URLS) return false;
        if (schoolUrls.contains(clean)) return false;
        schoolUrls.add(clean);
        savePreferences();
        return true;
    }

    public static synchronized boolean removeSchoolUrl(String url) {
        if (url == null) return false;
        boolean removed = schoolUrls.remove(url.trim());
        if (removed) {
            savePreferences();
        }
        return removed;
    }

    /**
     * Returns the active source identifiers to be passed to the AI worker.
     * When internet search is enabled: returns the 5 fixed internet sources + local folders + school URLs.
     * When internet search is disabled: returns ONLY local folders + school URLs.
     */
    public static synchronized List<String> getActiveSourcesForWorker() {
        List<String> sources = new ArrayList<>();
        if (internetSearchEnabled) {
            sources.add("wikimedia");
            sources.add("wikipedia");
            sources.add("https://freerangestock.com/");
            sources.add("openverse");
            sources.add("nasa");
        }
        for (String folder : localFolders) {
            sources.add("folder:" + folder);
        }
        for (String url : schoolUrls) {
            sources.add(url);
        }
        return sources;
    }

    /**
     * Returns whether any valid source is currently available for image retrieval.
     */
    public static synchronized boolean hasAnyActiveSource() {
        if (internetSearchEnabled) return true; // 5 fixed sources available
        return !localFolders.isEmpty() || !schoolUrls.isEmpty();
    }
}
