package com.gameshowcenter.offline.games;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameshowcenter.offline.games.views.*;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.model.GameDescriptor;
import com.gameshowcenter.offline.model.MatchConfig;
import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.scene.layout.Region;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GameStageRegistry {

    public interface StageFactory {
        Region create(List<Competitor> profiles, JsonNode setupData, Consumer<Competitor> winnerListener);
    }

    private static final Map<String, StageFactory> registry = new HashMap<>();
    private static final Map<String, com.gameshowcenter.offline.games.drivers.IGameArenaDriver> driverRegistry = new HashMap<>();

    static {
        // Register default native games
        register("Hangman", (profiles, setupData, listener) -> new HangmanFxStage(profiles, setupData));

        register("TicTacToe", (profiles, setupData, listener) -> new TicTacToeFxStage(profiles, setupData, listener));
        register("Tic_Tac_Toe", (profiles, setupData, listener) -> new TicTacToeFxStage(profiles, setupData, listener));

        register("Trivia Quiz", (profiles, setupData, listener) -> new TriviaQuizFxStage(profiles, setupData));
        register("Trivia_Quiz", (profiles, setupData, listener) -> new TriviaQuizFxStage(profiles, setupData));

        register("Zero Margin", (profiles, setupData, listener) -> new ZeroMarginFxStage(profiles, setupData, listener));
        register("Zero_Margin", (profiles, setupData, listener) -> new ZeroMarginFxStage(profiles, setupData, listener));

        register("GeoLocation", (profiles, setupData, listener) -> new GeoLocationFxStage(profiles, setupData));
        register("Geo_Location", (profiles, setupData, listener) -> new GeoLocationFxStage(profiles, setupData));

        register("Guess Character", (profiles, setupData, listener) -> new GuessCharacterFxStage(profiles, setupData));
        register("Guess_Character", (profiles, setupData, listener) -> new GuessCharacterFxStage(profiles, setupData));
        register("Guess The Character", (profiles, setupData, listener) -> new GuessCharacterFxStage(profiles, setupData));
        register("Guess_The_Character", (profiles, setupData, listener) -> new GuessCharacterFxStage(profiles, setupData));

        register("Rapid Rhythm", (profiles, setupData, listener) -> new RapidRhythmFxStage(profiles, setupData));
        register("Rapid_Rhythm", (profiles, setupData, listener) -> new RapidRhythmFxStage(profiles, setupData));

        register("Topic Takedown", (profiles, setupData, listener) -> new TopicTakedownFxStage(profiles, setupData));
        register("Topic_Takedown", (profiles, setupData, listener) -> new TopicTakedownFxStage(profiles, setupData));

        register("Snap Solve", (profiles, setupData, listener) -> new com.gameshowcenter.offline.games.views.SnapSolveFxStage(profiles, setupData));
        register("Snap_Solve", (profiles, setupData, listener) -> new com.gameshowcenter.offline.games.views.SnapSolveFxStage(profiles, setupData));

        register("TimeLine", (profiles, setupData, listener) -> new TimeLineFxStage(profiles, setupData, listener));
        register("Time_Line", (profiles, setupData, listener) -> new TimeLineFxStage(profiles, setupData, listener));
    }

    public static void register(String gameName, StageFactory factory) {
        if (gameName != null && factory != null) {
            String key = sanitizeKey(gameName);
            registry.put(key, factory);
            driverRegistry.put(key, new com.gameshowcenter.offline.games.drivers.DefaultGameArenaDriver(factory));
        }
    }

    public static void registerDriver(String gameName, com.gameshowcenter.offline.games.drivers.IGameArenaDriver driver) {
        if (gameName != null && driver != null) {
            driverRegistry.put(sanitizeKey(gameName), driver);
        }
    }

    public static com.gameshowcenter.offline.games.drivers.IGameArenaDriver getDriver(String gameName) {
        if (gameName == null) return null;
        return driverRegistry.get(sanitizeKey(gameName));
    }

    public static Region createStage(GameDescriptor game, MatchConfig config, Consumer<Competitor> winnerListener) {
        if (game == null) return null;
        String rawName = game.getName();
        String key = sanitizeKey(rawName);

        Region stage = null;

        // Check driver registry first
        com.gameshowcenter.offline.games.drivers.IGameArenaDriver driver = driverRegistry.get(key);
        if (driver != null) {
            stage = driver.createStage(game, config, winnerListener);
        }

        // 1. Check dynamic plugin classloader from game directory
        if (stage == null && game.getPluginClassLoader() != null) {
            String targetClassName = game.getStageClass();
            if (targetClassName == null || targetClassName.isBlank()) {
                targetClassName = "com.gameshowcenter.offline.games.views." + sanitizeClassName(rawName) + "FxStage";
            }
            stage = game.getPluginClassLoader().instantiateStage(
                targetClassName, config.getProfiles(), game.getSetupData(), winnerListener
            );
        }

        // 2. Check direct registry mapping
        if (stage == null) {
            StageFactory factory = registry.get(key);
            if (factory != null) {
                stage = factory.create(config.getProfiles(), game.getSetupData(), winnerListener);
            }
        }

        // 3. Reflection fallback for external downloaded games (Convention: <GameName>FxStage)
        if (stage == null) {
            try {
                String className = "com.gameshowcenter.offline.games.views." + sanitizeClassName(rawName) + "FxStage";
                Class<?> clazz = Class.forName(className);
                try {
                    stage = (Region) clazz.getDeclaredConstructor(List.class, JsonNode.class, Consumer.class)
                            .newInstance(config.getProfiles(), game.getSetupData(), winnerListener);
                } catch (Exception e) {
                    stage = (Region) clazz.getDeclaredConstructor(List.class, JsonNode.class)
                            .newInstance(config.getProfiles(), game.getSetupData());
                }
            } catch (Exception ignored) {}
        }

        // 4. Fallback generic view if stage class not found
        if (stage == null) {
            stage = new GenericGameFxStage(config.getProfiles(), game);
        }

        if (stage != null) {
            ThemeManager.applyTextScale(stage, ThemeManager.getFontScale());
        }

        return stage;
    }

    private static String sanitizeKey(String name) {
        return name == null ? "" : name.trim().toLowerCase().replace(" ", "_");
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
