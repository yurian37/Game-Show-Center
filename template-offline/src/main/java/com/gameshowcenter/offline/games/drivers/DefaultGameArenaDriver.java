package com.gameshowcenter.offline.games.drivers;

import com.gameshowcenter.offline.games.GameStageRegistry;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.model.GameDescriptor;
import com.gameshowcenter.offline.model.MatchConfig;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * Standard implementation of IGameArenaDriver wrapping a StageFactory.
 */
public class DefaultGameArenaDriver implements IGameArenaDriver {

    private final GameStageRegistry.StageFactory factory;

    public DefaultGameArenaDriver(GameStageRegistry.StageFactory factory) {
        this.factory = factory;
    }

    @Override
    public Region createStage(GameDescriptor game, MatchConfig config, Consumer<Competitor> winnerListener) {
        if (factory != null) {
            return factory.create(config != null ? config.getProfiles() : null, game != null ? game.getSetupData() : null, winnerListener);
        }
        return null;
    }
}
