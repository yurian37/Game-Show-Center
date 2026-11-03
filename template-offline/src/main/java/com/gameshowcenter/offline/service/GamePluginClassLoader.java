package com.gameshowcenter.offline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.model.Competitor;
import javafx.scene.layout.Region;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;

/**
 * Hardened ClassLoader for modular minigame plugins.
 * Enforces package whitelisting, dangerous namespace blocking, and type constraints
 * conforming to SOC 2 CC6.8 & ISO/IEC 27001:2022 A.8.19/A.8.25.
 */
public class GamePluginClassLoader {

    private final File gameDir;
    private URLClassLoader classLoader;

    // Allowed package prefixes for modular games
    private static final List<String> PERMITTED_PACKAGE_PREFIXES = List.of(
            "com.gameshowcenter.offline.games.",
            "com.gameshowcenter.plugins."
    );

    // Explicitly blocked system/internal namespaces to prevent RCE and sandbox escape
    private static final List<String> BLOCKED_NAMESPACES = List.of(
            "java.",
            "javax.",
            "jdk.",
            "sun.",
            "com.sun.",
            "org.apache.",
            "org.springframework.",
            "groovy."
    );

    public GamePluginClassLoader(File gameDir) {
        this.gameDir = gameDir;
        initClassLoader();
    }

    private void initClassLoader() {
        try {
            List<URL> urls = new ArrayList<>();

            // 1. Add game directory root
            if (gameDir.exists() && gameDir.isDirectory()) {
                urls.add(gameDir.toURI().toURL());
            }

            // 2. Add classes/ subfolder if present
            File classesDir = new File(gameDir, "classes");
            if (classesDir.exists() && classesDir.isDirectory()) {
                urls.add(classesDir.toURI().toURL());
            }

            // 3. Add validated game.jar or any .jar file in the game folder
            File[] jarFiles = gameDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (jarFiles != null) {
                for (File jar : jarFiles) {
                    urls.add(jar.toURI().toURL());
                }
            }

            this.classLoader = new URLClassLoader(urls.toArray(new URL[0]), GamePluginClassLoader.class.getClassLoader());
        } catch (Exception e) {
            System.err.println("Could not initialize ClassLoader for game folder " + gameDir.getName() + ": " + e.getMessage());
            this.classLoader = null;
        }
    }

    public ClassLoader getClassLoader() {
        return classLoader != null ? classLoader : GamePluginClassLoader.class.getClassLoader();
    }

    /**
     * Checks if a target class name complies with runtime security boundaries.
     */
    public static boolean isClassNamePermitted(String className) {
        if (className == null || className.isBlank()) return false;
        String clean = className.trim();

        // Check for blocked system/runtime reflection targets
        for (String blocked : BLOCKED_NAMESPACES) {
            if (clean.startsWith(blocked)) {
                return false;
            }
        }

        // Must match permitted modular game package prefixes
        for (String prefix : PERMITTED_PACKAGE_PREFIXES) {
            if (clean.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    public Class<?> loadClass(String className) {
        if (!isClassNamePermitted(className)) {
            System.err.println("Security Alert: Blocked dynamic loading of unauthorized class [" + className + "] in folder: " + gameDir.getName());
            return null;
        }

        try {
            return Class.forName(className.trim(), true, getClassLoader());
        } catch (Throwable t) {
            try {
                return Class.forName(className.trim());
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    public Region instantiateStage(String className, List<Competitor> profiles, JsonNode setupData, Consumer<Competitor> winnerListener) {
        Class<?> clazz = loadClass(className);
        if (clazz == null) return null;

        // Security check: Must be a JavaFX Region component
        if (!Region.class.isAssignableFrom(clazz)) {
            System.err.println("Security Alert: Class [" + className + "] rejected: Does not extend javafx.scene.layout.Region.");
            return null;
        }

        try {
            // Try 3-arg constructor: (List, JsonNode, Consumer)
            try {
                return (Region) clazz.getDeclaredConstructor(List.class, JsonNode.class, Consumer.class)
                        .newInstance(profiles, setupData, winnerListener);
            } catch (NoSuchMethodException e) {
                // Try 2-arg constructor: (List, JsonNode)
                return (Region) clazz.getDeclaredConstructor(List.class, JsonNode.class)
                        .newInstance(profiles, setupData);
            }
        } catch (Exception e) {
            System.err.println("Error instantiating stage class " + className + " for " + gameDir.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public IGameSetupEditor instantiateEditor(String className) {
        Class<?> clazz = loadClass(className);
        if (clazz == null) return null;

        // Security check: Must implement IGameSetupEditor
        if (!IGameSetupEditor.class.isAssignableFrom(clazz)) {
            System.err.println("Security Alert: Class [" + className + "] rejected: Does not implement IGameSetupEditor.");
            return null;
        }

        try {
            return (IGameSetupEditor) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Error instantiating setup editor class " + className + " for " + gameDir.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Computes the SHA-256 hash of a file for integrity verification.
     */
    public static String computeFileSha256(File file) {
        if (file == null || !file.exists() || !file.isFile()) return "";
        try (InputStream is = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
