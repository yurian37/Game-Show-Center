package com.gameshowcenter.offline.service;

public class FxSoundManager {

    private static final FxSoundManager instance = new FxSoundManager();

    public static FxSoundManager getInstance() {
        return instance;
    }

    private FxSoundManager() {}

    public void playBuzzer() {
        // Completely silent as per user request
    }

    public void playBeep() {
        // Completely silent as per user request
    }

    public void playApplause() {
        // Completely silent as per user request
    }

    public void playVictory() {
        // Completely silent as per user request
    }

    public void playClick() {
        // Completely silent as per user request
    }
}
