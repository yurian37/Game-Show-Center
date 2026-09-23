package com.gameshowcenter.offline.i18n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class I18n {

    public static class LanguageOption {
        public final String code;
        public final String name;
        public final String flag;

        public LanguageOption(String code, String name, String flag) {
            this.code = code;
            this.name = name;
            this.flag = flag;
        }

        @Override
        public String toString() {
            return flag + " " + name;
        }
    }

    public static final List<LanguageOption> SUPPORTED_LANGUAGES = List.of(
        new LanguageOption("en", "English", "🌐"),
        new LanguageOption("es", "Español", "🌐"),
        new LanguageOption("fr", "Français", "🌐"),
        new LanguageOption("pt", "Português", "🌐")
    );

    private static String currentLanguage = "en";
    private static final Map<String, Properties> BUNDLES = new HashMap<>();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static final File PREFS_FILE = new File("preferences.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        loadAllBundles();
        loadSavedLanguage();
    }

    private static void loadAllBundles() {
        for (LanguageOption lang : SUPPORTED_LANGUAGES) {
            Properties props = new Properties();
            String path = "/i18n/messages_" + lang.code + ".properties";
            try (InputStream is = I18n.class.getResourceAsStream(path)) {
                if (is != null) {
                    try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        props.load(reader);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load i18n bundle for " + lang.code + ": " + e.getMessage());
            }
            BUNDLES.put(lang.code, props);
        }
    }

    private static void loadSavedLanguage() {
        if (!PREFS_FILE.exists()) return;
        try {
            JsonNode root = MAPPER.readTree(PREFS_FILE);
            if (root.has("language")) {
                String savedLang = root.get("language").asText();
                if (isSupported(savedLang)) {
                    currentLanguage = savedLang;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveLanguagePreference(String lang) {
        try {
            ObjectNode root = PREFS_FILE.exists() ? (ObjectNode) MAPPER.readTree(PREFS_FILE) : MAPPER.createObjectNode();
            root.put("language", lang);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(PREFS_FILE, root);
        } catch (Exception ignored) {
        }
    }

    public static boolean isSupported(String code) {
        if (code == null) return false;
        for (LanguageOption opt : SUPPORTED_LANGUAGES) {
            if (opt.code.equalsIgnoreCase(code)) return true;
        }
        return false;
    }

    private static boolean switchingLocked = false;

    public static boolean isSwitchingLocked() {
        return switchingLocked;
    }

    public static void setSwitchingLocked(boolean locked) {
        switchingLocked = locked;
    }

    public static String getLanguage() {
        return currentLanguage;
    }

    public static void setLanguage(String code) {
        if (switchingLocked) return;
        if (code == null || !isSupported(code)) return;
        if (!currentLanguage.equalsIgnoreCase(code)) {
            currentLanguage = code.toLowerCase();
            saveLanguagePreference(currentLanguage);
            notifyListeners();
        }
    }

    public static void addListener(Runnable listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static void notifyListeners() {
        for (Runnable r : LISTENERS) {
            try {
                r.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves localized text for the given key.
     * Fallback order: Current Language -> English ('en') -> Key itself.
     */
    public static String get(String key, Object... args) {
        if (key == null) return "";

        Properties currentBundle = BUNDLES.get(currentLanguage);
        String val = (currentBundle != null) ? currentBundle.getProperty(key) : null;

        // Fallback to English if not present in current language
        if (val == null || val.trim().isEmpty()) {
            Properties enBundle = BUNDLES.get("en");
            val = (enBundle != null) ? enBundle.getProperty(key) : null;
        }

        if (val == null || val.trim().isEmpty()) {
            val = key;
        }

        if (args != null && args.length > 0) {
            if (val.contains("{0}") || val.contains("{1}")) {
                for (int i = 0; i < args.length; i++) {
                    val = val.replace("{" + i + "}", String.valueOf(args[i]));
                }
                return val;
            }
            try {
                return String.format(val, args);
            } catch (Exception e) {
                try {
                    return java.text.MessageFormat.format(val, args);
                } catch (Exception ignored) {
                    return val;
                }
            }
        }
        return val;
    }
}
