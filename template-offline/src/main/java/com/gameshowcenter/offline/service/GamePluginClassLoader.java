package com.gameshowcenter.offline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.games.IGameSetupEditor;
import com.gameshowcenter.offline.model.Competitor;
import javafx.scene.layout.Region;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GamePluginClassLoader {

    private final File gameDir;
    private URLClassLoader classLoader;

    public GamePluginClassLoader(File gameDir) {
        this.gameDir = gameDir;
        initClassLoader();
    }

    private void initClassLoader() {
        try {
            List<URL> urls = new ArrayList<>();

            // 1. Add game directory root
            urls.add(gameDir.toURI().toURL());

            // 2. Add classes/ subfolder if present
            File classesDir = new File(gameDir, "classes");
            if (classesDir.exists() && classesDir.isDirectory()) {
                urls.add(classesDir.toURI().toURL());
            }

            // 3. Add game.jar or any .jar file in the game folder
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

    public Class<?> loadClass(String className) {
        if (className == null || className.isBlank()) return null;
        try {
            return Class.forName(className, true, getClassLoader());
        } catch (Throwable t) {
            try {
                return Class.forName(className);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    public Region instantiateStage(String className, List<Competitor> profiles, JsonNode setupData, Consumer<Competitor> winnerListener) {
        Class<?> clazz = loadClass(className);
        if (clazz == null) return null;

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

        try {
            return (IGameSetupEditor) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Error instantiating setup editor class " + className + " for " + gameDir.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
