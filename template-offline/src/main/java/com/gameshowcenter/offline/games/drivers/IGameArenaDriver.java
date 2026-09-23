package com.gameshowcenter.offline.games.drivers;

import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.model.GameDescriptor;
import com.gameshowcenter.offline.model.MatchConfig;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * Driver interface for connecting minigames to the Game Show Center Arena.
 * Provides modularity, scalability and lifecycle management (pause, resume, cleanup).
 */
public interface IGameArenaDriver {

    /**
     * Instantiates and connects the JavaFX game stage for the Arena.
     */
    Region createStage(GameDescriptor game, MatchConfig config, Consumer<Competitor> winnerListener);

    /**
     * Optional lifecycle hook when pausing the game.
     */
    default void onGamePaused() {}
    default void onGamePaused(GameDescriptor game, MatchConfig config) {
        onGamePaused();
    }

    /**
     * Optional lifecycle hook when resuming the game.
     */
    default void onGameResumed() {}
    default void onGameResumed(GameDescriptor game, MatchConfig config) {
        onGameResumed();
    }

    /**
     * Clean up resources (stopping media players, background threads, timers) when exiting the game.
     */
    default void onGameExited() {}
    default void onGameExited(GameDescriptor game, MatchConfig config) {
        onGameExited();
    }
}
