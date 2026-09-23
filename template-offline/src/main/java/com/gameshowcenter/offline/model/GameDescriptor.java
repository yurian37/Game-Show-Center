package com.gameshowcenter.offline.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.i18n.I18n;
import com.gameshowcenter.offline.service.GamePluginClassLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameDescriptor {

    public static class Translation {
        public String name;
        public String description;
        public String instructions;

        public Translation(String name, String description) {
            this(name, description, "");
        }

        public Translation(String name, String description, String instructions) {
            this.name = name;
            this.description = description;
            this.instructions = instructions;
        }
    }

    private String name;
    private String description;
    private String instructions;
    private String author;
    private String folderPath;
    private String stageClass;
    private String editorClass;
    private JsonNode setupData;
    private List<String> availableModes = new ArrayList<>();
    private int playOrder = 0;
    private GamePluginClassLoader pluginClassLoader;
    private final Map<String, Translation> translations = new HashMap<>();

    public GameDescriptor() {}

    public void addTranslation(String lang, String name, String description) {
        addTranslation(lang, name, description, "");
    }

    public void addTranslation(String lang, String name, String description, String instructions) {
        if (lang != null) {
            translations.put(lang.toLowerCase(), new Translation(name, description, instructions));
        }
    }

    public Map<String, Translation> getTranslations() {
        return translations;
    }

    public String getName() {
        return getEnglishName();
    }

    public String getName(String lang) {
        return getEnglishName();
    }

    public String getEnglishName() {
        Translation enT = translations.get("en");
        if (enT != null && enT.name != null && !enT.name.trim().isEmpty()) {
            return enT.name;
        }
        return name != null ? name : "Minigame";
    }

    public void setName(String name) { this.name = name; }

    public String getDescription() {
        return getDescription(I18n.getLanguage());
    }

    public String getDescription(String lang) {
        if (lang != null) {
            Translation t = translations.get(lang.toLowerCase());
            if (t != null && t.description != null && !t.description.trim().isEmpty()) {
                return t.description;
            }
        }
        Translation enT = translations.get("en");
        if (enT != null && enT.description != null && !enT.description.trim().isEmpty()) {
            return enT.description;
        }
        return description != null ? description : "";
    }

    public void setDescription(String description) { this.description = description; }

    public String getInstructions() {
        return getInstructions(I18n.getLanguage());
    }

    public String getInstructions(String lang) {
        if (lang != null) {
            Translation t = translations.get(lang.toLowerCase());
            if (t != null && t.instructions != null && !t.instructions.trim().isEmpty()) {
                return t.instructions;
            }
        }
        Translation enT = translations.get("en");
        if (enT != null && enT.instructions != null && !enT.instructions.trim().isEmpty()) {
            return enT.instructions;
        }
        return instructions != null ? instructions : "";
    }

    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public String getStageClass() { return stageClass; }
    public void setStageClass(String stageClass) { this.stageClass = stageClass; }

    public String getEditorClass() { return editorClass; }
    public void setEditorClass(String editorClass) { this.editorClass = editorClass; }

    public JsonNode getSetupData() { return setupData; }
    public void setSetupData(JsonNode setupData) { this.setupData = setupData; }

    public List<String> getAvailableModes() { return availableModes; }
    public void setAvailableModes(List<String> availableModes) { this.availableModes = availableModes; }

    public int getPlayOrder() { return playOrder; }
    public void setPlayOrder(int playOrder) { this.playOrder = playOrder; }

    public GamePluginClassLoader getPluginClassLoader() { return pluginClassLoader; }
    public void setPluginClassLoader(GamePluginClassLoader pluginClassLoader) { this.pluginClassLoader = pluginClassLoader; }

    public boolean isAvailableInMode(String mode) {
        return isModeSupported(mode);
    }

    public boolean isModeSupported(String mode) {
        if (availableModes == null || availableModes.isEmpty()) return true;
        String target = normalize(mode);

        for (String m : availableModes) {
            String norm = normalize(m);
            if (norm.equals(target)) return true;
            if ((norm.equals("1vs1") || norm.equals("1v1")) && (target.equals("1vs1") || target.equals("1v1"))) return true;
            if ((norm.equals("freeforall") || norm.equals("ffa")) && (target.equals("freeforall") || target.equals("ffa"))) return true;
            if (norm.equals("team") && target.equals("team")) return true;
        }
        return false;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replace("_", "").replace("-", "").replace(" ", "").trim();
    }
}
