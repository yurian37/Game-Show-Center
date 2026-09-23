package com.gameshowcenter.offline.games;

import com.gameshowcenter.offline.games.editors.*;
import com.gameshowcenter.offline.model.GameDescriptor;

import java.util.HashMap;
import java.util.Map;

public class GameEditorRegistry {

    private static final Map<String, Class<? extends IGameSetupEditor>> registry = new HashMap<>();

    static {
        register("Hangman", HangmanSetupEditor.class);
        register("TicTacToe", TicTacToeSetupEditor.class);
        register("Trivia Quiz", TriviaQuizSetupEditor.class);
        register("Trivia_Quiz", TriviaQuizSetupEditor.class);
        register("Zero Margin", ZeroMarginSetupEditor.class);
        register("Zero_Margin", ZeroMarginSetupEditor.class);
        register("GeoLocation", GeoLocationSetupEditor.class);
        register("Geo_Location", GeoLocationSetupEditor.class);
        register("Guess Character", GuessCharacterSetupEditor.class);
        register("Guess_Character", GuessCharacterSetupEditor.class);
        register("Guess The Character", GuessCharacterSetupEditor.class);
        register("Guess_The_Character", GuessCharacterSetupEditor.class);
        register("Rapid Rhythm", RapidRhythmSetupEditor.class);
        register("Rapid_Rhythm", RapidRhythmSetupEditor.class);
        register("Topic Takedown", TopicTakedownSetupEditor.class);
        register("Topic_Takedown", TopicTakedownSetupEditor.class);
        register("Snap Solve", SnapSolveSetupEditor.class);
        register("Snap_Solve", SnapSolveSetupEditor.class);
        register("TimeLine", TimeLineSetupEditor.class);
        register("Time_Line", TimeLineSetupEditor.class);
    }

    public static void register(String gameName, Class<? extends IGameSetupEditor> clazz) {
        if (gameName != null && clazz != null) {
            registry.put(gameName.toLowerCase().replace(" ", "_"), clazz);
            registry.put(gameName.toLowerCase().replace("_", " "), clazz);
        }
    }

    public static IGameSetupEditor getEditor(GameDescriptor game) {
        if (game == null) return new GenericGameSetupEditor();

        if (game.getPluginClassLoader() != null) {
            String targetClassName = game.getEditorClass();
            if (targetClassName == null || targetClassName.isBlank()) {
                targetClassName = "com.gameshowcenter.offline.games.editors." + sanitizeClassName(game.getName()) + "SetupEditor";
            }
            IGameSetupEditor pluginEditor = game.getPluginClassLoader().instantiateEditor(targetClassName);
            if (pluginEditor != null) return pluginEditor;
        }

        return getEditor(game.getName());
    }

    @SuppressWarnings("unchecked")
    public static IGameSetupEditor getEditor(String gameName) {
        if (gameName == null) return new GenericGameSetupEditor();
        String key = gameName.toLowerCase();
        Class<? extends IGameSetupEditor> clazz = registry.get(key);
        if (clazz == null) clazz = registry.get(key.replace(" ", "_"));
        if (clazz == null) clazz = registry.get(key.replace("_", " "));

        // Reflection lookup fallback for external games
        if (clazz == null) {
            try {
                String className = "com.gameshowcenter.offline.games.editors." + sanitizeClassName(gameName) + "SetupEditor";
                clazz = (Class<? extends IGameSetupEditor>) Class.forName(className);
            } catch (Exception ignored) {}
        }

        if (clazz != null) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.err.println("Could not instantiate setup editor for " + gameName + ": " + e.getMessage());
            }
        }

        return new GenericGameSetupEditor();
    }

    private static String sanitizeClassName(String name) {
        if (name == null) return "Generic";
        String[] parts = name.split("[\\s_]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isBlank()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }
}
