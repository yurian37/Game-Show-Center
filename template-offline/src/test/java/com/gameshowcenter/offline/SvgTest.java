package com.gameshowcenter.offline;

import com.gameshowcenter.offline.util.SvgEmoji;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class SvgTest {
    public static void main(String[] args) {
        try {
            Platform.startup(() -> {});
        } catch (Exception ignored) {}

        File dir = new File("assets/emojis");
        if (!dir.exists()) {
            dir = new File("template-offline/assets/emojis");
        }

        System.out.println("Testing emojis directory: " + dir.getAbsolutePath());
        File[] files = dir.listFiles((d, name) -> name.endsWith(".svg"));
        if (files == null) {
            System.err.println("No files found!");
            System.exit(1);
        }

        int issues = 0;
        for (File f : files) {
            String iconName = f.getName().replace(".svg", "");
            try {
                Node node = SvgEmoji.create(iconName);
                @SuppressWarnings("unchecked")
                List<SvgEmoji.PathDefinition> pathDefs = (List<SvgEmoji.PathDefinition>) node.getProperties().get("gsc_path_defs");
                if (pathDefs == null || pathDefs.isEmpty()) {
                    System.err.println("[EMPTY PATHS] " + f.getName());
                    issues++;
                } else if (pathDefs.size() == 1 && pathDefs.get(0).d.equals("M 12 15 a 3 3 0 1 0 0 -6 a 3 3 0 0 0 0 6 Z") && !iconName.equals("circle-filled")) {
                    System.err.println("[FALLBACK GEAR USED] " + f.getName());
                    issues++;
                } else {
                    // Check if SVGPath can parse without throwing
                    for (SvgEmoji.PathDefinition pd : pathDefs) {
                        SVGPath sp = new SVGPath();
                        sp.setContent(pd.d);
                    }
                }
            } catch (Exception e) {
                System.err.println("[EXCEPTION] " + f.getName() + ": " + e.getMessage());
                issues++;
            }
        }

        System.out.println("SVG Direct Test Completed with " + issues + " issues.");
        if (issues > 0) {
            System.exit(1);
        }
        System.exit(0);
    }
}
