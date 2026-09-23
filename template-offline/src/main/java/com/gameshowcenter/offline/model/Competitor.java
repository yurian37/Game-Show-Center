package com.gameshowcenter.offline.model;

public class Competitor {
    private String id;
    private String name;
    private String avatarPath;
    private int score;

    public Competitor() {}

    public Competitor(String id, String name, String avatarPath) {
        this.id = id;
        this.name = name;
        this.avatarPath = avatarPath;
        this.score = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
