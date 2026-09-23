package com.gameshowcenter.backend;

import com.gameshowcenter.backend.dto.SnapSolveSetup;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnapSolveSetupTest {

    @Test
    void testValidSetup() {
        SnapSolveSetup setup = new SnapSolveSetup();
        setup.setRoundsPerPlayer(2);
        setup.setSelectedFilters(Arrays.asList("displacement", "swirl", "pixelate", "blur"));
        setup.setMediaPool(Arrays.asList("img1.png", "img2.png", "img3.png", "img4.png"));

        assertDoesNotThrow(() -> setup.validate(2));
    }

    @Test
    void testInsufficientMediaPoolThrowsException() {
        SnapSolveSetup setup = new SnapSolveSetup();
        setup.setRoundsPerPlayer(3);
        setup.setSelectedFilters(List.of("swirl"));
        // 2 players * 3 rounds = 6 required, only 3 provided
        setup.setMediaPool(Arrays.asList("img1.png", "img2.png", "img3.png"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setup.validate(2));
        assertTrue(ex.getMessage().contains("At least 6 media files required in pool"));
    }

    @Test
    void testNullMediaPoolThrowsException() {
        SnapSolveSetup setup = new SnapSolveSetup();
        setup.setRoundsPerPlayer(1);
        setup.setSelectedFilters(List.of("pixelate"));
        setup.setMediaPool(null);

        assertThrows(IllegalArgumentException.class, () -> setup.validate(2));
    }

    @Test
    void testEmptySelectedFiltersThrowsException() {
        SnapSolveSetup setup = new SnapSolveSetup();
        setup.setRoundsPerPlayer(2);
        setup.setSelectedFilters(Collections.emptyList());
        setup.setMediaPool(Arrays.asList("img1.png", "img2.png"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setup.validate(1));
        assertTrue(ex.getMessage().contains("At least one distortion filter must be selected"));
    }

    @Test
    void testInvalidRoundsPerPlayerThrowsException() {
        SnapSolveSetup setup = new SnapSolveSetup();
        setup.setRoundsPerPlayer(0);
        setup.setSelectedFilters(List.of("blur"));
        setup.setMediaPool(List.of("img1.png"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> setup.validate(1));
        assertTrue(ex.getMessage().contains("Rounds per player must be at least 1"));
    }
}
