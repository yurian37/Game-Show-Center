package com.gameshowcenter.offline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameshowcenter.offline.model.GameDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GameScannerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<GameDescriptor> scanGamesFolder(File gamesDir) {
        List<GameDescriptor> discoveredGames = new ArrayList<>();

        if (gamesDir == null || !gamesDir.exists() || !gamesDir.isDirectory()) {
            return discoveredGames;
        }

        File[] subDirs = gamesDir.listFiles(File::isDirectory);
        if (subDirs == null) return discoveredGames;

        for (File dir : subDirs) {
            File descFile = new File(dir, "description.json");
            File setupFile = new File(dir, "setup.json");

            GameDescriptor descriptor = new GameDescriptor();
            descriptor.setFolderPath(dir.getAbsolutePath());
            descriptor.setName(dir.getName());

            // Initialize Plugin ClassLoader for this game directory
            GamePluginClassLoader pluginLoader = new GamePluginClassLoader(dir);
            descriptor.setPluginClassLoader(pluginLoader);

            if (descFile.exists()) {
                try {
                    JsonNode descNode = objectMapper.readTree(descFile);
                    if (descNode.has("name")) descriptor.setName(descNode.get("name").asText());
                    if (descNode.has("author")) descriptor.setAuthor(descNode.get("author").asText());
                    if (descNode.has("description")) descriptor.setDescription(descNode.get("description").asText());
                    if (descNode.has("stageClass")) descriptor.setStageClass(descNode.get("stageClass").asText());
                    if (descNode.has("editorClass")) descriptor.setEditorClass(descNode.get("editorClass").asText());

                    if (descNode.has("available") && descNode.get("available").isArray()) {
                        List<String> modes = new ArrayList<>();
                        for (JsonNode m : descNode.get("available")) {
                            modes.add(m.asText());
                        }
                        descriptor.setAvailableModes(modes);
                    }

                    String topInstructions = "";
                    if (descNode.has("instructions")) {
                        if (descNode.get("instructions").isArray()) {
                            List<String> lines = new ArrayList<>();
                            for (JsonNode l : descNode.get("instructions")) lines.add(l.asText());
                            topInstructions = String.join("\n", lines);
                        } else {
                            topInstructions = descNode.get("instructions").asText();
                        }
                    }
                    descriptor.setInstructions(topInstructions);

                    if (descNode.has("translations") && descNode.get("translations").isObject()) {
                        JsonNode transNode = descNode.get("translations");
                        transNode.fieldNames().forEachRemaining(lang -> {
                            JsonNode item = transNode.get(lang);
                            String transName = item.has("name") ? item.get("name").asText() : (descNode.has("name") ? descNode.get("name").asText() : dir.getName());
                            String transDesc = item.has("description") ? item.get("description").asText() : (descNode.has("description") ? descNode.get("description").asText() : "");
                            String transInstr = "";
                            if (item.has("instructions")) {
                                if (item.get("instructions").isArray()) {
                                    List<String> lines = new ArrayList<>();
                                    for (JsonNode l : item.get("instructions")) lines.add(l.asText());
                                    transInstr = String.join("\n", lines);
                                } else {
                                    transInstr = item.get("instructions").asText();
                                }
                            }
                            descriptor.addTranslation(lang, transName, transDesc, transInstr);
                        });
                    }

                    // Check for auxiliary instructions files (e.g. instructions.json or instructions_<lang>.txt)
                    scanAuxiliaryInstructions(dir, descriptor);
                } catch (Exception e) {
                    System.err.println("Could not parse description.json in " + dir.getName() + ": " + e.getMessage());
                }
            } else {
                descriptor.setAuthor("YuyiStudio");
                descriptor.setDescription("Custom offline minigame module.");
            }

            if (setupFile.exists()) {
                try {
                    JsonNode setupNode = objectMapper.readTree(setupFile);
                    descriptor.setSetupData(setupNode);
                } catch (Exception e) {
                    System.err.println("Could not parse setup.json in " + dir.getName() + ": " + e.getMessage());
                }
            }

            discoveredGames.add(descriptor);
        }

        return discoveredGames;
    }

    private void scanAuxiliaryInstructions(File dir, GameDescriptor descriptor) {
        if (dir == null || !dir.exists() || descriptor == null) return;

        // 1. Check for instructions.json
        File instructionsJsonFile = new File(dir, "instructions.json");
        if (instructionsJsonFile.exists()) {
            try {
                JsonNode root = objectMapper.readTree(instructionsJsonFile);
                if (root.isObject()) {
                    root.fieldNames().forEachRemaining(lang -> {
                        JsonNode val = root.get(lang);
                        String text = "";
                        if (val.isArray()) {
                            List<String> lines = new ArrayList<>();
                            for (JsonNode l : val) lines.add(l.asText());
                            text = String.join("\n", lines);
                        } else {
                            text = val.asText();
                        }
                        GameDescriptor.Translation existing = descriptor.getTranslations().get(lang.toLowerCase());
                        if (existing != null) {
                            existing.instructions = text;
                        } else {
                            descriptor.addTranslation(lang, descriptor.getName(lang), descriptor.getDescription(lang), text);
                        }
                    });
                }
            } catch (Exception ignored) {}
        }

        // 2. Check for instructions_<lang>.txt and instructions.txt
        File[] txtFiles = dir.listFiles((d, name) -> name.toLowerCase().startsWith("instructions") && name.toLowerCase().endsWith(".txt"));
        if (txtFiles != null) {
            for (File tf : txtFiles) {
                try {
                    String content = java.nio.file.Files.readString(tf.toPath(), java.nio.charset.StandardCharsets.UTF_8).trim();
                    String fname = tf.getName().toLowerCase();
                    String lang = "en";
                    if (fname.contains("_")) {
                        lang = fname.substring(fname.indexOf('_') + 1, fname.lastIndexOf('.'));
                    }
                    GameDescriptor.Translation existing = descriptor.getTranslations().get(lang.toLowerCase());
                    if (existing != null) {
                        existing.instructions = content;
                    } else {
                        descriptor.addTranslation(lang, descriptor.getName(lang), descriptor.getDescription(lang), content);
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
