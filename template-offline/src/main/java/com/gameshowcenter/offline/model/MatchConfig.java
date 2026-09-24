package com.gameshowcenter.offline.model;

import java.util.*;

public class MatchConfig {
    private String gameMode = "1vs1";
    private List<Competitor> profiles = new ArrayList<>();
    private List<GameDescriptor> selectedGames = new ArrayList<>();
    private List<String> scorePresets = Arrays.asList("+10", "+50", "+100", "-10", "-50");
    private Map<String, Integer> scores = new HashMap<>();

    private boolean battleRoyale = false;

    public MatchConfig() {}

    public boolean isBattleRoyale() { return battleRoyale; }
    public void setBattleRoyale(boolean battleRoyale) { this.battleRoyale = battleRoyale; }

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }

    public List<Competitor> getProfiles() { return profiles; }
    public void setProfiles(List<Competitor> profiles) { 
        this.profiles = profiles; 
        if (profiles != null) {
            for (Competitor c : profiles) {
                scores.putIfAbsent(c.getId(), 0);
            }
        }
    }

    public List<GameDescriptor> getSelectedGames() { return selectedGames; }
    public void setSelectedGames(List<GameDescriptor> selectedGames) { this.selectedGames = selectedGames; }

    public List<String> getScorePresets() { return scorePresets; }
    public void setScorePresets(List<String> scorePresets) { this.scorePresets = scorePresets; }

    public Map<String, Integer> getScores() { return scores; }
    public void setScores(Map<String, Integer> scores) { this.scores = scores; }

    public int getScore(String profileId) {
        return scores.getOrDefault(profileId, 0);
    }

    public void adjustScore(String profileId, int amount) {
        int current = getScore(profileId);
        scores.put(profileId, current + amount);
    }

    public void resetScore(String profileId) {
        scores.put(profileId, 0);
    }
}
