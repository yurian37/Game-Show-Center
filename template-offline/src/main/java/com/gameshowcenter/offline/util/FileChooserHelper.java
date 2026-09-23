package com.gameshowcenter.offline.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/**
 * Utility helper to remember and persist the last used directory across all FileChoosers in the app.
 */
public class FileChooserHelper {

    private static final File PREFS_FILE = new File("preferences.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static File lastDirectory = null;

    static {
        loadLastDirectory();
    }

    private static void loadLastDirectory() {
        if (PREFS_FILE.exists()) {
            try {
                JsonNode root = MAPPER.readTree(PREFS_FILE);
                if (root.has("last_directory")) {
                    String path = root.get("last_directory").asText();
                    File dir = new File(path);
                    if (dir.exists() && dir.isDirectory()) {
                        lastDirectory = dir;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (lastDirectory == null) {
            // Default to current working directory or desktop
            File currentDir = new File(System.getProperty("user.dir"));
            if (currentDir.exists() && currentDir.isDirectory()) {
                lastDirectory = currentDir;
            }
        }
    }

    public static synchronized void updateLastDirectory(File fileOrDir) {
        if (fileOrDir == null) return;
        File dir = fileOrDir.isDirectory() ? fileOrDir : fileOrDir.getParentFile();
        if (dir != null && dir.exists() && dir.isDirectory()) {
            lastDirectory = dir;
            saveLastDirectory(dir.getAbsolutePath());
        }
    }

    private static void saveLastDirectory(String path) {
        try {
            ObjectNode root = PREFS_FILE.exists() ? (ObjectNode) MAPPER.readTree(PREFS_FILE) : MAPPER.createObjectNode();
            root.put("last_directory", path);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(PREFS_FILE, root);
        } catch (Exception ignored) {
        }
    }

    public static File getLastDirectory() {
        if (lastDirectory != null && lastDirectory.exists() && lastDirectory.isDirectory()) {
            return lastDirectory;
        }
        return new File(System.getProperty("user.dir"));
    }

    public static FileChooser createChooser(String title) {
        FileChooser chooser = new FileChooser();
        if (title != null) {
            chooser.setTitle(title);
        }
        File initial = getLastDirectory();
        if (initial != null && initial.exists() && initial.isDirectory()) {
            try {
                chooser.setInitialDirectory(initial);
            } catch (Exception ignored) {
            }
        }
        return chooser;
    }

    public static File showOpenDialog(FileChooser chooser, Window window) {
        if (chooser.getInitialDirectory() == null || !chooser.getInitialDirectory().exists()) {
            try {
                chooser.setInitialDirectory(getLastDirectory());
            } catch (Exception ignored) {}
        }
        File selected = chooser.showOpenDialog(window);
        if (selected != null) {
            updateLastDirectory(selected);
        }
        return selected;
    }

    public static File showSaveDialog(FileChooser chooser, Window window) {
        if (chooser.getInitialDirectory() == null || !chooser.getInitialDirectory().exists()) {
            try {
                chooser.setInitialDirectory(getLastDirectory());
            } catch (Exception ignored) {}
        }
        File selected = chooser.showSaveDialog(window);
        if (selected != null) {
            updateLastDirectory(selected);
        }
        return selected;
    }
}
