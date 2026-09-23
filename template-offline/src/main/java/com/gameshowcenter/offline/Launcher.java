package com.gameshowcenter.offline;

public class Launcher {
    public static void main(String[] args) {
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 GameShowCenter/1.0");
        MainApp.main(args);
    }
}
