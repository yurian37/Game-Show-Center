package com.gameshowcenter.offline.sound;

import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dedicated Sound Manager for Offline Mode.
 * Runs on a single dedicated background thread ("Sound-Dedicated-Thread")
 * to guarantee strict mutual exclusion: no two sounds can ever play simultaneously.
 * Whenever a new sound/song request arrives, any currently active sound is immediately stopped.
 * When switching games or navigating screens, stopAll() halts all audio immediately.
 */
public class SoundManager {

    private static final SoundManager INSTANCE = new SoundManager();

    public static SoundManager getInstance() {
        return INSTANCE;
    }

    private final ExecutorService soundExecutor;
    private AudioClip activeClip;
    private MediaPlayer activeMediaPlayer;

    private SoundManager() {
        this.soundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Sound-Dedicated-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Stops any currently playing audio immediately.
     */
    public void stopAll() {
        soundExecutor.submit(this::stopAllInternal);
    }

    /**
     * Internal stop method executed on the dedicated sound thread.
     */
    private void stopAllInternal() {
        Platform.runLater(() -> {
            if (activeClip != null) {
                try {
                    activeClip.stop();
                } catch (Exception ignored) {}
                activeClip = null;
            }
            if (activeMediaPlayer != null) {
                try {
                    activeMediaPlayer.stop();
                } catch (Exception ignored) {}
                activeMediaPlayer = null;
            }
        });
    }

    /**
     * Plays an AudioClip, stopping any currently playing sound or song beforehand.
     *
     * @param clip The AudioClip to play
     */
    public void playClip(AudioClip clip) {
        if (clip == null) return;
        soundExecutor.submit(() -> {
            // Stop previous audio first
            Platform.runLater(() -> {
                if (activeClip != null && activeClip != clip) {
                    try {
                        activeClip.stop();
                    } catch (Exception ignored) {}
                }
                if (activeMediaPlayer != null) {
                    try {
                        activeMediaPlayer.stop();
                    } catch (Exception ignored) {}
                    activeMediaPlayer = null;
                }

                activeClip = clip;
                try {
                    clip.play();
                } catch (Exception e) {
                    System.err.println("[SoundManager] Error playing AudioClip: " + e.getMessage());
                }
            });
        });
    }

    /**
     * Plays a sound effect file, stopping any currently playing sound or song beforehand.
     *
     * @param soundFile The audio file to play
     */
    public void playClip(File soundFile) {
        if (soundFile == null || !soundFile.exists()) return;
        try {
            AudioClip clip = new AudioClip(soundFile.toURI().toString());
            playClip(clip);
        } catch (Exception e) {
            System.err.println("[SoundManager] Could not load audio clip from " + soundFile.getPath() + ": " + e.getMessage());
        }
    }

    /**
     * Registers and plays a MediaPlayer track, stopping any currently playing sound or song beforehand.
     *
     * @param player The MediaPlayer to play
     */
    public void playMedia(MediaPlayer player) {
        if (player == null) return;
        soundExecutor.submit(() -> {
            Platform.runLater(() -> {
                if (activeClip != null) {
                    try {
                        activeClip.stop();
                    } catch (Exception ignored) {}
                    activeClip = null;
                }
                if (activeMediaPlayer != null && activeMediaPlayer != player) {
                    try {
                        activeMediaPlayer.stop();
                    } catch (Exception ignored) {}
                }

                activeMediaPlayer = player;
                try {
                    player.play();
                } catch (Exception e) {
                    System.err.println("[SoundManager] Error playing MediaPlayer: " + e.getMessage());
                }
            });
        });
    }

    /**
     * Registers a MediaPlayer as the active player (if played elsewhere like playPause toggle).
     */
    public void setActiveMediaPlayer(MediaPlayer player) {
        soundExecutor.submit(() -> {
            Platform.runLater(() -> {
                if (activeMediaPlayer != null && activeMediaPlayer != player) {
                    try {
                        activeMediaPlayer.stop();
                    } catch (Exception ignored) {}
                }
                activeMediaPlayer = player;
            });
        });
    }

    /**
     * Pauses the active MediaPlayer if one is playing.
     */
    public void pauseActiveMedia() {
        soundExecutor.submit(() -> {
            Platform.runLater(() -> {
                if (activeMediaPlayer != null) {
                    try {
                        activeMediaPlayer.pause();
                    } catch (Exception ignored) {}
                }
            });
        });
    }

    /**
     * Shuts down the dedicated sound thread executor.
     */
    public void shutdown() {
        stopAllInternal();
        soundExecutor.shutdownNow();
    }
}
