package com.gameshowcenter.offline.util;

import com.gameshowcenter.offline.theme.ThemeManager;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal SVG Emoji and Vector Icon Loader for Game Show Center Offline.
 * Renders crisp, responsive, contrast-adaptive vector icons instead of platform emojis.
 *
 * Characteristics:
 * 1) Monochrome & dynamic contrast: Inherits and adjusts color from white to black based on background contrast.
 * 2) Dynamic text-size scaling: Automatically adjusts dimensions in sync with ThemeManager fontScale & accessibility.
 */
public class SvgEmoji {

    private static final Map<String, String> EMOJI_TO_NAME = new HashMap<>();
    private static final Map<String, String> ICON_ALIASES = new HashMap<>();
    private static final Map<String, List<PathDefinition>> SVG_CACHE = new HashMap<>();
    private static final Set<StackPane> ACTIVE_ICONS = Collections.newSetFromMap(new WeakHashMap<>());

    private static final Pattern PATH_PATTERN = Pattern.compile("<path\\b([^>]*?)(?:/>|>([\\s\\S]*?)</path>|>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR_D = Pattern.compile("d=\"([^\"]+)\"");
    private static final Pattern ATTR_FILL = Pattern.compile("fill=\"([^\"]+)\"");
    private static final Pattern ATTR_STROKE = Pattern.compile("stroke=\"([^\"]+)\"");
    private static final Pattern ATTR_STROKE_WIDTH = Pattern.compile("stroke-width=\"([^\"]+)\"");

    private static final Pattern CIRCLE_PATTERN = Pattern.compile("<circle\\b([^>]*?)(?:/>|>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE_PATTERN = Pattern.compile("<line\\b([^>]*?)(?:/>|>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RECT_PATTERN = Pattern.compile("<rect\\b([^>]*?)(?:/>|>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern POLYGON_PATTERN = Pattern.compile("<polygon\\b([^>]*?)(?:/>|>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern POLYLINE_PATTERN = Pattern.compile("<polyline\\b([^>]*?)(?:/>|>)", Pattern.CASE_INSENSITIVE);

    private static final Pattern EMOJI_REGEX = Pattern.compile(
        "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\u2300-\\u23FF\\u2B50-\\u2B55\\u203C-\\u2049\\u2194-\\u21AA\\u25AA-\\u25FE\\u2700-\\u27BF\\u21A9\\u2705\\u274C\\u2716\\u2795\\u2139]"
    );

    public static class PathDefinition {
        public String d;
        public boolean isFill;
        public boolean isStroke;
        public double strokeWidth = 2.0;

        public PathDefinition(String d, boolean isFill, boolean isStroke, double strokeWidth) {
            this.d = d;
            this.isFill = isFill;
            this.isStroke = isStroke;
            this.strokeWidth = strokeWidth;
        }
    }

    static {
        // Map common emojis to their SVG asset names
        map("🏆", "trophy");
        map("⚙", "gear");
        map("⚙️", "gear");
        map("🎯", "target");
        map("⚠", "warning");
        map("⚠️", "warning");
        map("✨", "sparkles");
        map("🔒", "lock");
        map("🔄", "refresh");
        map("🏁", "flag");
        map("⚡", "lightning");
        map("🚀", "rocket");
        map("⚔", "swords");
        map("⚔️", "swords");
        map("👑", "crown");
        map("💡", "bulb");
        map("⏱", "stopwatch");
        map("👁", "eye");
        map("👁️", "eye");
        map("⏳", "hourglass");
        map("⌛", "hourglass");
        map("🖼", "image");
        map("🖼️", "image");
        map("✓", "check");
        map("✅", "check-circle");
        map("💾", "save");
        map("⏸", "pause");
        map("▶", "play");
        map("🎮", "gamepad");
        map("🌐", "globe");
        map("🌍", "globe");
        map("📁", "folder");
        map("📂", "folder-open");
        map("🙈", "monkey-hide");
        map("🎭", "masks");
        map("🔍", "search");
        map("🔎", "search-plus");
        map("➕", "plus");
        map("✕", "close");
        map("❌", "close");
        map("✖", "close");
        map("⏹", "stop");
        map("📖", "book");
        map("🗑", "trash");
        map("🗑️", "trash");
        map("🎵", "music");
        map("🌪", "vortex");
        map("🌀", "vortex");
        map("🚧", "construction");
        map("🚫", "prohibited");
        map("📊", "chart");
        map("🔊", "speaker");
        map("🔇", "speaker-mute");
        map("🎰", "slot-machine");
        map("👤", "user");
        map("👥", "users");
        map("🥇", "medal-gold");
        map("🥈", "medal-silver");
        map("🥉", "medal-bronze");
        map("🔤", "letters-abc");
        map("🔠", "letters-case");
        map("🎲", "dice");
        map("📸", "camera-flash");
        map("📷", "camera");
        map("⏮", "previous");
        map("↩", "back");
        map("🠔", "back");
        map("📋", "clipboard");
        map("🔗", "link");
        map("📄", "document");
        map("🤝", "handshake");
        map("🌫", "fog");
        map("🎨", "palette");
        map("⭐", "star");
        map("★", "star");
        map("🔥", "fire");
        map("💀", "skull");
        map("📜", "scroll");
        map("❓", "question");
        map("🧭", "compass");
        map("🏛", "monument");
        map("🧱", "bricks");
        map("😢", "sad");
        map("📢", "megaphone");
        map("🎉", "party");
        map("📥", "inbox");
        map("🏠", "home");
        map("🖥", "desktop");
        map("❤", "heart");
        map("🖤", "heart");
        map("💳", "credit-card");
        map("🔑", "key");
        map("🧩", "puzzle");
        map("🔴", "circle-dot");
        map("🔵", "circle-dot");
        map("🟡", "circle-dot");
        map("⚪", "circle-outline");
        map("●", "circle-filled");
        map("○", "circle-outline");
        map("▼", "caret-down");
        map("➔", "arrow-right");
        map("👇", "arrow-down");
        map("🧊", "ice");
        map("🍊", "citrus");
        map("🧪", "flask");
        map("🇪🇸", "flag-es");
        map("🇺🇸", "flag-us");
        map("🇫🇷", "flag-fr");
        map("🇧🇷", "flag-br");
        map("ℹ", "info");
        map("ℹ️", "info");

        // Common UI icon aliases
        ICON_ALIASES.put("monitor", "desktop");
        ICON_ALIASES.put("settings", "gear");
        ICON_ALIASES.put("musical-notes", "music");
        ICON_ALIASES.put("timer", "stopwatch");
        ICON_ALIASES.put("volume", "speaker");
        ICON_ALIASES.put("volume-mute", "speaker-mute");
        ICON_ALIASES.put("lightbulb", "bulb");
        ICON_ALIASES.put("help", "question");
        ICON_ALIASES.put("eye-off", "monkey-hide");
        ICON_ALIASES.put("cross", "close");
        ICON_ALIASES.put("delete", "trash");
    }

    private static void map(String emoji, String iconName) {
        EMOJI_TO_NAME.put(emoji, iconName);
    }

    public static String resolveIconName(String emojiOrName) {
        if (emojiOrName == null || emojiOrName.trim().isEmpty()) return "gear";
        String trimmed = emojiOrName.trim();
        if (EMOJI_TO_NAME.containsKey(trimmed)) {
            return EMOJI_TO_NAME.get(trimmed);
        }
        for (Map.Entry<String, String> e : EMOJI_TO_NAME.entrySet()) {
            if (trimmed.contains(e.getKey())) {
                return e.getValue();
            }
        }
        String clean = trimmed.toLowerCase().replaceAll("[^a-z0-9_-]", "");
        if (ICON_ALIASES.containsKey(clean)) {
            return ICON_ALIASES.get(clean);
        }
        return clean;
    }

    public static String stripEmojis(String text) {
        if (text == null) return "";
        return EMOJI_REGEX.matcher(text).replaceAll("").trim();
    }

    public static boolean hasEmoji(String text) {
        if (text == null) return false;
        return EMOJI_REGEX.matcher(text).find();
    }

    public static String extractFirstEmoji(String text) {
        if (text == null) return null;
        Matcher m = EMOJI_REGEX.matcher(text);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    public static Node create(String emojiOrName) {
        return create(emojiOrName, 16.0);
    }

    public static Node create(String emojiOrName, double baseSize) {
        return create(emojiOrName, baseSize, null);
    }

    public static Node create(String emojiOrName, double baseSize, Color explicitColor) {
        String iconName = resolveIconName(emojiOrName);
        List<PathDefinition> paths = loadSvgPaths(iconName);

        Group group = new Group();
        List<SVGPath> fxPaths = new ArrayList<>();

        for (PathDefinition pd : paths) {
            SVGPath p = new SVGPath();
            p.setContent(pd.d);
            if (pd.isStroke) {
                p.setFill(null);
                p.setStrokeWidth(pd.strokeWidth);
                p.setStrokeLineCap(StrokeLineCap.ROUND);
                p.setStrokeLineJoin(StrokeLineJoin.ROUND);
            }
            if (pd.isFill && !pd.isStroke) {
                p.setStroke(null);
            }
            fxPaths.add(p);
            group.getChildren().add(p);
        }

        StackPane container = new StackPane(group);
        container.setAlignment(Pos.CENTER);
        container.getProperties().put("gsc_base_icon_size", baseSize);
        container.getProperties().put("gsc_explicit_color", explicitColor);
        container.getProperties().put("gsc_paths", fxPaths);
        container.getProperties().put("gsc_path_defs", paths);
        container.getProperties().put("gsc_group", group);

        ACTIVE_ICONS.add(container);

        applyScale(container, ThemeManager.getFontScale());
        updateIconColor(container, explicitColor);

        return container;
    }

    public static Label createLabel(String emojiOrName, double baseSize) {
        Label lbl = new Label();
        lbl.setGraphic(create(emojiOrName, baseSize));
        lbl.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        return lbl;
    }

    public static void setGraphic(Labeled labeled, String emojiOrName) {
        setGraphic(labeled, emojiOrName, 16.0);
    }

    public static void setGraphic(Labeled labeled, String emojiOrName, double baseSize) {
        if (labeled == null) return;
        Node icon = create(emojiOrName, baseSize);
        labeled.setGraphic(icon);
        labeled.setGraphicTextGap(6);
    }

    public static void configureLabeled(Labeled labeled, String textWithEmoji) {
        configureLabeled(labeled, textWithEmoji, 16.0);
    }

    public static void configureLabeled(Labeled labeled, String textWithEmoji, double baseSize) {
        if (labeled == null) return;
        String emoji = extractFirstEmoji(textWithEmoji);
        String clean = stripEmojis(textWithEmoji);
        labeled.setText(clean);
        if (emoji != null) {
            setGraphic(labeled, emoji, baseSize);
        }
    }

    public static Button createButton(String textWithEmoji) {
        Button btn = new Button();
        configureLabeled(btn, textWithEmoji, 16.0);
        return btn;
    }

    public static Label createTextLabel(String textWithEmoji) {
        Label lbl = new Label();
        configureLabeled(lbl, textWithEmoji, 16.0);
        return lbl;
    }

    public static void applyScale(Node node, double fontScale) {
        if (node instanceof StackPane container && container.getProperties().containsKey("gsc_base_icon_size")) {
            Double baseSize = (Double) container.getProperties().get("gsc_base_icon_size");
            if (baseSize == null) baseSize = 16.0;

            double targetSize = Math.max(8.0, baseSize * fontScale);
            double scaleFactor = targetSize / 24.0;

            Group group = (Group) container.getProperties().get("gsc_group");
            if (group != null) {
                group.setScaleX(scaleFactor);
                group.setScaleY(scaleFactor);
            }

            container.setMinSize(targetSize, targetSize);
            container.setPrefSize(targetSize, targetSize);
            container.setMaxSize(targetSize, targetSize);
        }
    }

    public static void updateContrastColors() {
        Color currentContrast = Color.web(ThemeManager.getTextPrimaryHex());
        for (StackPane container : ACTIVE_ICONS) {
            if (container != null) {
                Color explicit = (Color) container.getProperties().get("gsc_explicit_color");
                updateIconColor(container, explicit != null ? explicit : currentContrast);
            }
        }
    }

    private static void updateIconColor(StackPane container, Color color) {
        if (color == null) {
            color = Color.web(ThemeManager.getTextPrimaryHex());
        }
        @SuppressWarnings("unchecked")
        List<SVGPath> fxPaths = (List<SVGPath>) container.getProperties().get("gsc_paths");
        @SuppressWarnings("unchecked")
        List<PathDefinition> pathDefs = (List<PathDefinition>) container.getProperties().get("gsc_path_defs");

        if (fxPaths != null && pathDefs != null) {
            for (int i = 0; i < fxPaths.size() && i < pathDefs.size(); i++) {
                SVGPath p = fxPaths.get(i);
                PathDefinition pd = pathDefs.get(i);
                if (pd.isStroke) {
                    p.setStroke(color);
                }
                if (pd.isFill) {
                    p.setFill(color);
                }
            }
        }
    }

    private static List<PathDefinition> loadSvgPaths(String iconName) {
        if (SVG_CACHE.containsKey(iconName)) {
            return SVG_CACHE.get(iconName);
        }

        List<PathDefinition> list = new ArrayList<>();
        String content = readSvgContent(iconName);

        if (content != null && !content.isEmpty()) {
            try {
                // 1. Extract <path> elements
                Matcher m = PATH_PATTERN.matcher(content);
                while (m.find()) {
                    String attrs = m.group(1);
                    Matcher dMatcher = ATTR_D.matcher(attrs);
                    if (dMatcher.find()) {
                        String d = sanitizePathD(dMatcher.group(1));
                        boolean isFill = true;
                        boolean isStroke = false;
                        double strokeWidth = 2.0;

                        if (content.contains("fill=\"none\"") || attrs.contains("fill=\"none\"")) {
                            isFill = false;
                        }
                        if (content.contains("stroke=\"currentColor\"") || attrs.contains("stroke=\"currentColor\"")) {
                            isStroke = true;
                        }
                        if (attrs.contains("fill=\"currentColor\"")) {
                            isFill = true;
                        }

                        Matcher swMatcher = ATTR_STROKE_WIDTH.matcher(attrs);
                        if (!swMatcher.find()) {
                            swMatcher = ATTR_STROKE_WIDTH.matcher(content);
                        }
                        if (swMatcher.find()) {
                            try { strokeWidth = Double.parseDouble(swMatcher.group(1)); } catch (Exception ignored) {}
                        }

                        list.add(new PathDefinition(d, isFill, isStroke, strokeWidth));
                    }
                }

                // 2. If additional SVG shapes exist (circle, line, rect), parse them as well
                parseShapeTags(content, list);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (list.isEmpty()) {
            // Fallback gear path
            list.add(new PathDefinition("M 12 15 a 3 3 0 1 0 0 -6 a 3 3 0 0 0 0 6 Z", true, false, 2.0));
        }

        SVG_CACHE.put(iconName, list);
        return list;
    }

    private static String readSvgContent(String iconName) {
        // 1. Filesystem candidates
        File[] candidates = new File[] {
            new File("assets/emojis/" + iconName + ".svg"),
            new File("template-offline/assets/emojis/" + iconName + ".svg")
        };
        for (File f : candidates) {
            if (f.exists()) {
                try {
                    return Files.readString(f.toPath());
                } catch (Exception ignored) {}
            }
        }

        // 2. Relative to code source (JAR or target/classes)
        try {
            java.net.URL location = SvgEmoji.class.getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                File codeDir = new File(location.toURI()).getParentFile();
                if (codeDir != null) {
                    File f = new File(codeDir, "assets/emojis/" + iconName + ".svg");
                    if (f.exists()) {
                        return Files.readString(f.toPath());
                    }
                    File f2 = new File(codeDir.getParentFile(), "assets/emojis/" + iconName + ".svg");
                    if (f2.exists()) {
                        return Files.readString(f2.toPath());
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Classpath resource
        try (java.io.InputStream is = SvgEmoji.class.getResourceAsStream("/assets/emojis/" + iconName + ".svg")) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        try (java.io.InputStream is = SvgEmoji.class.getResourceAsStream("/emojis/" + iconName + ".svg")) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        return null;
    }

    public static String sanitizePathD(String d) {
        if (d == null) return "";
        // Auto-fix legacy malformed polyline/polygon path with L before every single number: e.g. "M 13 L 2 L 3 L 14"
        if (d.matches("(?i)\\s*M\\s+[0-9.]+(?:\\s+L\\s+[0-9.]+){3,}.*")) {
            Matcher numMatcher = Pattern.compile("[-+]?(?:\\d*\\.\\d+|\\d+)").matcher(d);
            List<String> nums = new ArrayList<>();
            while (numMatcher.find()) {
                nums.add(numMatcher.group());
            }
            if (nums.size() >= 2 && nums.size() % 2 == 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("M ").append(nums.get(0)).append(",").append(nums.get(1));
                for (int i = 2; i < nums.size(); i += 2) {
                    sb.append(" L ").append(nums.get(i)).append(",").append(nums.get(i + 1));
                }
                if (d.trim().toUpperCase().endsWith("Z")) {
                    sb.append(" Z");
                }
                return sb.toString();
            }
        }
        return d;
    }

    private static void parseShapeTags(String content, List<PathDefinition> list) {
        // Circles
        Matcher cM = CIRCLE_PATTERN.matcher(content);
        while (cM.find()) {
            String attrs = cM.group(1);
            Matcher cxM = Pattern.compile("\\bcx=\"([0-9.]+)\"").matcher(attrs);
            Matcher cyM = Pattern.compile("\\bcy=\"([0-9.]+)\"").matcher(attrs);
            Matcher rM = Pattern.compile("\\br=\"([0-9.]+)\"").matcher(attrs);
            if (cxM.find() && cyM.find() && rM.find()) {
                double cx = Double.parseDouble(cxM.group(1));
                double cy = Double.parseDouble(cyM.group(1));
                double r = Double.parseDouble(rM.group(1));
                String d = String.format(Locale.US, "M %.2f,%.2f a %.2f,%.2f 0 1,0 %.2f,0 a %.2f,%.2f 0 1,0 %.2f,0 Z",
                    cx - r, cy, r, r, r * 2, 0.0, r, r, -r * 2, 0.0);
                boolean isFill = attrs.contains("fill=\"currentColor\"") || !content.contains("fill=\"none\"");
                boolean isStroke = attrs.contains("stroke=\"currentColor\"") || content.contains("stroke=\"currentColor\"");
                list.add(new PathDefinition(d, isFill, isStroke, 2.0));
            }
        }

        // Lines
        Matcher lM = LINE_PATTERN.matcher(content);
        while (lM.find()) {
            String attrs = lM.group(1);
            Matcher x1M = Pattern.compile("\\bx1=\"([0-9.]+)\"").matcher(attrs);
            Matcher y1M = Pattern.compile("\\by1=\"([0-9.]+)\"").matcher(attrs);
            Matcher x2M = Pattern.compile("\\bx2=\"([0-9.]+)\"").matcher(attrs);
            Matcher y2M = Pattern.compile("\\by2=\"([0-9.]+)\"").matcher(attrs);
            if (x1M.find() && y1M.find() && x2M.find() && y2M.find()) {
                String d = "M " + x1M.group(1) + "," + y1M.group(1) + " L " + x2M.group(1) + "," + y2M.group(1);
                list.add(new PathDefinition(d, false, true, 2.0));
            }
        }
    }
}
