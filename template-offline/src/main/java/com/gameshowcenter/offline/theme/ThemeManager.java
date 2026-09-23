package com.gameshowcenter.offline.theme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Window;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThemeManager {

    public static class Palette {
        public String name;
        public Color bgApp;       // Fondo exterior
        public Color bgMainBox;   // Marco principal
        public Color bgCard;      // Cuadros de texto y tarjetas
        public Color accent;      // Botones principales / destacados
        public Color textPrimary;
        public Color textSecondary;
        public boolean isCustom;  // Flag si fue creado por el usuario

        public Palette(String name, Color bgApp, Color bgMainBox, Color bgCard, Color accent, Color textPrimary, Color textSecondary, boolean isCustom) {
            this.name = name;
            this.bgApp = bgApp;
            this.bgMainBox = bgMainBox;
            this.bgCard = bgCard;
            this.accent = accent;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.isCustom = isCustom;
        }
    }

    private static final Map<String, Palette> PALETTES = new LinkedHashMap<>();
    private static Palette currentPalette;
    
    private static String customBackgroundImagePath = null;
    private static Color customAppBgColor = null;
    private static Color customMainBoxColor = null;
    private static Color customCardColor = null;
    private static Color customAccentColor = null;

    // Accessibility font scale (1.0 = 100% default)
    private static double fontScale = 1.0;
    private static final Pattern FONT_SIZE_PATTERN = Pattern.compile("-fx-font-size\\s*:\\s*([0-9.]+)\\s*(px|pt)?", Pattern.CASE_INSENSITIVE);

    private static final File PREFS_FILE = new File("preferences.json");
    private static final File CUSTOM_THEMES_FILE = new File("custom_themes.json");

    static {
        // =========================================================================
        // 15 PALETAS CONSERVADORAS (Sobrias, elegantes y armónicas)
        // =========================================================================
        addPalette("1. Midnight Indigo", "#090d16", "#141929", "#1e253b", "#6366f1", "#f8fafc", "#94a3b8");
        addPalette("2. Studio Slate", "#0b1220", "#162033", "#22314d", "#818cf8", "#f8fafc", "#cbd5e1");
        addPalette("3. Deep Marine", "#02131e", "#072438", "#0d3754", "#06b6d4", "#ecfeff", "#67e8f9");
        addPalette("4. Dark Obsidian", "#0a0a0d", "#14141a", "#21212b", "#64748b", "#f8fafc", "#94a3b8");
        addPalette("5. Emerald Luxury", "#041410", "#0a261e", "#123d31", "#10b981", "#ecfdf5", "#6ee7b7");
        addPalette("6. Mocha Coffee", "#140c0a", "#241612", "#38231c", "#d97706", "#fffbeb", "#fde68a");
        addPalette("7. Titanium Silver", "#0d1117", "#161b22", "#21262d", "#94a3b8", "#f8fafc", "#e2e8f0");
        addPalette("8. Royal Amethyst", "#0f081c", "#1d1233", "#2c1b4d", "#a855f7", "#faf5ff", "#c084fc");
        addPalette("9. Sunset Amber", "#140a04", "#261408", "#3b1f0c", "#f59e0b", "#fef3c7", "#fcd34d");
        addPalette("10. Forest Mist", "#061711", "#0e291f", "#183e30", "#34d399", "#ecfdf5", "#a7f3d0");
        addPalette("11. Blizzard Frost", "#061221", "#0d2038", "#143154", "#38bdf8", "#f0f9ff", "#bae6fd");
        addPalette("12. Autumn Copper", "#170c04", "#291608", "#3d220d", "#f97316", "#fff7ed", "#ffedd5");
        addPalette("13. Cosmic Onyx", "#040914", "#0b152e", "#13234a", "#3b82f6", "#eff6ff", "#93c5fd");
        addPalette("14. Charcoal Rose", "#170a10", "#29131d", "#3d1e2c", "#fb7185", "#fff1f2", "#fecdd3");
        addPalette("15. Steel Graphite", "#11141a", "#1a1f29", "#272e3d", "#71717a", "#f4f4f5", "#a1a1aa");

        // =========================================================================
        // 5 PALETAS ALOCADAS / CONTRAPUESTAS (Alto Contraste y Colores Opuestos)
        // =========================================================================
        addPalette("16. Cyberpunk Neon 🔥", "#090014", "#2e0036", "#002b36", "#d946ef", "#00ffff", "#ff007f");
        addPalette("17. Acid Lime & Purple ⚡", "#0f001c", "#240046", "#80ff00", "#cc00ff", "#ffffff", "#80ff00");
        addPalette("18. Fire & Ice 🧊🔥", "#021024", "#052659", "#ff3366", "#00f0ff", "#ffffff", "#ff99aa");
        addPalette("19. Electric Tangerine 🍊", "#030c1d", "#081d42", "#ff6600", "#00ccff", "#ffffff", "#ffaa66");
        addPalette("20. Toxic Matrix 🧪", "#021206", "#063813", "#ff007f", "#00ff66", "#ffffff", "#ff80bf");

        currentPalette = PALETTES.get("1. Midnight Indigo");

        // Cargar temas personalizados guardados previamente
        loadCustomThemes();

        // Cargar preferencia de tema guardada en preferences.json
        loadSavedThemePreference();
    }

    public static void loadSavedThemePreference() {
        File prefFile = PREFS_FILE;
        if (!prefFile.exists()) prefFile = new File("template-offline/preferences.json");
        if (!prefFile.exists()) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(prefFile);
            if (root.has("theme") && root.get("theme").isTextual()) {
                setPaletteInternal(root.get("theme").asText());
            }
            if (root.has("custom_app_bg") && root.get("custom_app_bg").isTextual()) {
                customAppBgColor = Color.web(root.get("custom_app_bg").asText());
            }
            if (root.has("custom_main_box") && root.get("custom_main_box").isTextual()) {
                customMainBoxColor = Color.web(root.get("custom_main_box").asText());
            }
            if (root.has("custom_card") && root.get("custom_card").isTextual()) {
                customCardColor = Color.web(root.get("custom_card").asText());
            }
            if (root.has("custom_accent") && root.get("custom_accent").isTextual()) {
                customAccentColor = Color.web(root.get("custom_accent").asText());
            }
            if (root.has("custom_bg_img") && root.get("custom_bg_img").isTextual()) {
                customBackgroundImagePath = root.get("custom_bg_img").asText();
            }
            if (root.has("font_scale") && root.get("font_scale").isNumber()) {
                fontScale = Math.max(0.70, Math.min(2.0, root.get("font_scale").asDouble()));
            }
            updateDynamicTextColors();
        } catch (Exception ignored) {}
    }

    public static void saveThemePreference() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            File file = PREFS_FILE;
            if (!file.exists() && new File("template-offline/preferences.json").exists()) {
                file = new File("template-offline/preferences.json");
            }
            ObjectNode root = file.exists() ? (ObjectNode) mapper.readTree(file) : mapper.createObjectNode();
            if (currentPalette != null) {
                root.put("theme", currentPalette.name);
            }
            if (customAppBgColor != null) {
                root.put("custom_app_bg", toHex(customAppBgColor));
            } else {
                root.remove("custom_app_bg");
            }
            if (customMainBoxColor != null) {
                root.put("custom_main_box", toHex(customMainBoxColor));
            } else {
                root.remove("custom_main_box");
            }
            if (customCardColor != null) {
                root.put("custom_card", toHex(customCardColor));
            } else {
                root.remove("custom_card");
            }
            if (customAccentColor != null) {
                root.put("custom_accent", toHex(customAccentColor));
            } else {
                root.remove("custom_accent");
            }
            if (customBackgroundImagePath != null) {
                root.put("custom_bg_img", customBackgroundImagePath);
            } else {
                root.remove("custom_bg_img");
            }
            root.put("font_scale", fontScale);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception ignored) {}
    }

    private static void addPalette(String name, String bgApp, String bgMainBox, String bgCard, String accent, String textPrimary, String textSecondary) {
        addPalette(name, bgApp, bgMainBox, bgCard, accent, textPrimary, textSecondary, false);
    }

    private static void addPalette(String name, String bgApp, String bgMainBox, String bgCard, String accent, String textPrimary, String textSecondary, boolean isCustom) {
        PALETTES.put(name, new Palette(
            name,
            Color.web(bgApp),
            Color.web(bgMainBox),
            Color.web(bgCard),
            Color.web(accent),
            Color.web(textPrimary),
            Color.web(textSecondary),
            isCustom
        ));
    }

    public static Palette getCurrentPalette() {
        updateDynamicTextColors();
        return currentPalette;
    }

    public static Map<String, Palette> getPalettesMap() {
        return PALETTES;
    }

    public static String[] getAvailableThemes() {
        return PALETTES.keySet().toArray(new String[0]);
    }

    public static void setPalette(String name) {
        setPaletteInternal(name);
        saveThemePreference();
    }

    public static void setPaletteInternal(String name) {
        if (name == null) return;
        if (PALETTES.containsKey(name)) {
            currentPalette = PALETTES.get(name);
            updateDynamicTextColors();
            return;
        }
        for (Map.Entry<String, Palette> entry : PALETTES.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name) || entry.getKey().toLowerCase().contains(name.toLowerCase().trim())) {
                currentPalette = entry.getValue();
                updateDynamicTextColors();
                return;
            }
        }
    }

    public static boolean saveCustomPalette(String rawName, Color bgApp, Color bgMainBox, Color bgCard, Color accent) {
        if (rawName == null || rawName.trim().isEmpty()) return false;
        String name = "✨ " + rawName.trim().replace("✨ ", "");

        Color finalBgApp = bgApp != null ? bgApp : (customAppBgColor != null ? customAppBgColor : currentPalette.bgApp);
        Color finalMainBox = bgMainBox != null ? bgMainBox : (customMainBoxColor != null ? customMainBoxColor : currentPalette.bgMainBox);
        Color finalCard = bgCard != null ? bgCard : (customCardColor != null ? customCardColor : currentPalette.bgCard);
        Color finalAccent = accent != null ? accent : (customAccentColor != null ? customAccentColor : currentPalette.accent);

        Color finalTxtPri = isLight(finalMainBox) ? Color.web("#0f172a") : Color.web("#f8fafc");
        Color finalTxtSec = isLight(finalMainBox) ? Color.web("#475569") : Color.web("#94a3b8");

        Palette pal = new Palette(
            name,
            finalBgApp,
            finalMainBox,
            finalCard,
            finalAccent,
            finalTxtPri,
            finalTxtSec,
            true
        );

        PALETTES.put(name, pal);
        currentPalette = pal;

        // Limpiar overrides temporales para que rija la paleta recién guardada
        customAppBgColor = null;
        customMainBoxColor = null;
        customCardColor = null;
        customAccentColor = null;

        updateDynamicTextColors();
        saveCustomThemesToFile();
        saveThemePreference();
        return true;
    }

    public static boolean deleteCustomPalette(String name) {
        if (name != null && PALETTES.containsKey(name)) {
            Palette p = PALETTES.get(name);
            if (p.isCustom) {
                PALETTES.remove(name);
                if (currentPalette == p) {
                    currentPalette = PALETTES.values().iterator().next();
                }
                updateDynamicTextColors();
                saveCustomThemesToFile();
                saveThemePreference();
                return true;
            }
        }
        return false;
    }

    public static void loadCustomThemes() {
        if (!CUSTOM_THEMES_FILE.exists()) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode array = mapper.readTree(CUSTOM_THEMES_FILE);
            if (array != null && array.isArray()) {
                for (JsonNode node : array) {
                    if (node.has("name") && node.has("bgApp") && node.has("bgMainBox") && node.has("bgCard") && node.has("accent")) {
                        String name = node.get("name").asText();
                        String bgApp = node.get("bgApp").asText();
                        String bgMainBox = node.get("bgMainBox").asText();
                        String bgCard = node.get("bgCard").asText();
                        String accent = node.get("accent").asText();
                        String textPrimary = node.has("textPrimary") ? node.get("textPrimary").asText() : "#f8fafc";
                        String textSecondary = node.has("textSecondary") ? node.get("textSecondary").asText() : "#94a3b8";

                        addPalette(name, bgApp, bgMainBox, bgCard, accent, textPrimary, textSecondary, true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveCustomThemesToFile() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode array = mapper.createArrayNode();
            for (Map.Entry<String, Palette> entry : PALETTES.entrySet()) {
                Palette p = entry.getValue();
                if (p.isCustom) {
                    ObjectNode obj = mapper.createObjectNode();
                    obj.put("name", p.name);
                    obj.put("bgApp", toHex(p.bgApp));
                    obj.put("bgMainBox", toHex(p.bgMainBox));
                    obj.put("bgCard", toHex(p.bgCard));
                    obj.put("accent", toHex(p.accent));
                    obj.put("textPrimary", toHex(p.textPrimary));
                    obj.put("textSecondary", toHex(p.textSecondary));
                    array.add(obj);
                }
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(CUSTOM_THEMES_FILE, array);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCustomBackgroundImagePath() {
        return customBackgroundImagePath;
    }

    public static void setCustomBackgroundImagePath(String path) {
        customBackgroundImagePath = path;
        saveThemePreference();
    }

    public static Color getCustomAppBgColor() {
        return customAppBgColor;
    }

    public static void setCustomAppBgColor(Color color) {
        customAppBgColor = color;
        updateDynamicTextColors();
        saveThemePreference();
    }

    public static Color getCustomMainBoxColor() {
        return customMainBoxColor;
    }

    public static void setCustomMainBoxColor(Color color) {
        customMainBoxColor = color;
        updateDynamicTextColors();
        saveThemePreference();
    }

    public static Color getCustomCardColor() {
        return customCardColor;
    }

    public static void setCustomCardColor(Color color) {
        customCardColor = color;
        updateDynamicTextColors();
        saveThemePreference();
    }

    public static Color getCustomAccentColor() {
        return customAccentColor;
    }

    public static void setCustomAccentColor(Color color) {
        customAccentColor = color;
        updateDynamicTextColors();
        saveThemePreference();
    }

    public static void updateDynamicTextColors() {
        if (currentPalette == null) return;
        Color mainBoxBg = customMainBoxColor != null ? customMainBoxColor : currentPalette.bgMainBox;
        if (mainBoxBg == null) {
            mainBoxBg = customAppBgColor != null ? customAppBgColor : currentPalette.bgApp;
        }

        if (isLight(mainBoxBg)) {
            currentPalette.textPrimary = Color.web("#0f172a");
            currentPalette.textSecondary = Color.web("#475569");
        } else {
            currentPalette.textPrimary = Color.web("#f8fafc");
            currentPalette.textSecondary = Color.web("#94a3b8");
        }
    }

    // 1. FONDO (Outer Window Background)
    public static String getAppBgHex() {
        Color base = customAppBgColor != null ? customAppBgColor : currentPalette.bgApp;
        return toHex(base);
    }

    // 2. MARCO (Central Main Container Panel)
    public static String getMainBoxHex() {
        Color base = customMainBoxColor != null ? customMainBoxColor : currentPalette.bgMainBox;
        return toHex(base);
    }

    // 3. CUADROS DE TEXTO Y TARJETAS (Inner Cards & Text Input Boxes)
    public static String getCardHex() {
        Color base = customCardColor != null ? customCardColor : currentPalette.bgCard;
        return toHex(base);
    }

    // 4. BOTONES ESTÁNDAR (Con contraste distintivo respecto al marco y cuadros)
    public static String getButtonHex() {
        Color base = customCardColor != null ? customCardColor : currentPalette.bgCard;
        return toHex(brighten(base, 0.12));
    }

    // 5. BOTONES PRINCIPALES / DESTACADOS (START GAME, ACCIONES DESTACADAS)
    public static String getAccentHex() {
        Color base = customAccentColor != null ? customAccentColor : currentPalette.accent;
        return toHex(base);
    }

    public static String getCardRgbaString() {
        return String.format("rgba(%d, %d, %d, 1.00)",
            (int) (Color.web(getCardHex()).getRed() * 255),
            (int) (Color.web(getCardHex()).getGreen() * 255),
            (int) (Color.web(getCardHex()).getBlue() * 255));
    }

    private static Color brighten(Color color, double factor) {
        if (color == null) return Color.web("#263248");
        double r = Math.min(1.0, color.getRed() + factor);
        double g = Math.min(1.0, color.getGreen() + factor);
        double b = Math.min(1.0, color.getBlue() + factor);
        return new Color(r, g, b, color.getOpacity());
    }

    // =========================================================================
    // DECISIÓN INTELIGENTE DE CONTRASTE SEGÚN CONTEXTO (FONDO vs CUADROS)
    // =========================================================================

    /**
     * Calcula la luminosidad relativa percibida según la fórmula estándar YIQ / W3C:
     * L = 0.299*R + 0.587*G + 0.114*B (rango 0.0 a 1.0)
     */
    public static double getLuminance(Color color) {
        if (color == null) return 0.0;
        return (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
    }

    /**
     * Determina si un color de fondo es claro (luminancia > 0.55).
     */
    public static boolean isLight(Color color) {
        return getLuminance(color) > 0.55;
    }

    public static boolean isLight(String hexColor) {
        if (hexColor == null) return false;
        try {
            return isLight(Color.web(hexColor));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Devuelve el color de texto óptimo (Negro para fondos claros, Blanco para oscuros).
     */
    public static String getContrastTextColor(Color bg) {
        return isLight(bg) ? "#0f172a" : "#f8fafc";
    }

    public static String getContrastTextColor(String bgHex) {
        return isLight(bgHex) ? "#0f172a" : "#f8fafc";
    }

    /**
     * Devuelve el color de texto secundario/subtítulos con contraste óptimo.
     */
    public static String getContrastSecondaryTextColor(Color bg) {
        return isLight(bg) ? "#475569" : "#94a3b8";
    }

    public static String getContrastSecondaryTextColor(String bgHex) {
        return isLight(bgHex) ? "#475569" : "#94a3b8";
    }

    // --- CONTEXTO 1: TEXTO DIRECTAMENTE SOBRE EL MARCO PRINCIPAL (FONDO/MAIN BOX) ---
    public static String getTextOnMainBoxPrimaryHex() {
        return getContrastTextColor(getMainBoxHex());
    }

    public static String getTextOnMainBoxSecondaryHex() {
        return getContrastSecondaryTextColor(getMainBoxHex());
    }

    // --- CONTEXTO 2: TEXTO SOBRE CUADROS, TARJETAS Y CONTENEDORES INTERNOS (CARDS) ---
    public static String getTextOnCardPrimaryHex() {
        return getContrastTextColor(getCardHex());
    }

    public static String getTextOnCardSecondaryHex() {
        return getContrastSecondaryTextColor(getCardHex());
    }

    // --- CONTEXTO 3: TEXTO SOBRE EL FONDO EXTERIOR DE LA VENTANA (APP BG) ---
    public static String getTextOnAppBgPrimaryHex() {
        return getContrastTextColor(getAppBgHex());
    }

    public static String getTextOnAppBgSecondaryHex() {
        return getContrastSecondaryTextColor(getAppBgHex());
    }

    // --- CONTEXTO 4: TEXTO SOBRE BOTONES ESTÁNDAR ---
    public static String getTextOnButtonPrimaryHex() {
        return getContrastTextColor(getButtonHex());
    }

    // --- CONTEXTO 5: TEXTO SOBRE BOTÓN DESTACADO / ACCENT ---
    public static String getTextOnAccentPrimaryHex() {
        return getContrastTextColor(getAccentHex());
    }

    public static String getTextPrimaryHex() {
        return getTextOnMainBoxPrimaryHex();
    }

    public static String getTextSecondaryHex() {
        return getTextOnMainBoxSecondaryHex();
    }

    public static String toHex(Color color) {
        if (color == null) return "#141929";
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    // =========================================================================
    // ACCESIBILIDAD Y ESCALADO GLOBAL DE TEXTO
    // =========================================================================

    public static double getFontScale() {
        return fontScale;
    }

    public static void setFontScale(double scale) {
        fontScale = Math.max(0.70, Math.min(2.0, scale));
        saveThemePreference();
        try {
            for (Window window : Window.getWindows()) {
                if (window.getScene() != null && window.getScene().getRoot() != null) {
                    applyTextScale(window.getScene().getRoot(), fontScale);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static int getScaledFontSize(int basePx) {
        return Math.max(8, (int) Math.round(basePx * fontScale));
    }

    public static String getFontSizeStyle(int basePx) {
        return String.format("-fx-font-size: %dpx;", getScaledFontSize(basePx));
    }

    /**
     * Aplica el escalado de texto recursivamente a todos los nodos de la interfaz
     * y registra listeners reactivos para escalar automáticamente elementos y estilos
     * agregados o modificados dinámicamente en tiempo de ejecución.
     */
    public static void applyTextScale(Node rootNode, double scale) {
        if (rootNode == null) return;
        scaleNode(rootNode, scale);
        attachDynamicListeners(rootNode);
        if (rootNode instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyTextScale(child, scale);
            }
        }
    }

    private static void attachDynamicListeners(Node node) {
        if (node == null) return;

        // 1. Si el nodo es un contenedor (Parent), escuchar la adición dinámica de hijos
        if (node instanceof Parent parent) {
            if (!Boolean.TRUE.equals(parent.getProperties().get("gsc_children_scale_listener"))) {
                parent.getProperties().put("gsc_children_scale_listener", Boolean.TRUE);
                parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (Node addedChild : change.getAddedSubList()) {
                                applyTextScale(addedChild, fontScale);
                            }
                        }
                    }
                });
            }
        }

        // 2. Escuchar cambios dinámicos de estilo (setStyle) para aplicar la escala si se define font-size
        if (!Boolean.TRUE.equals(node.getProperties().get("gsc_style_scale_listener"))) {
            node.getProperties().put("gsc_style_scale_listener", Boolean.TRUE);
            node.styleProperty().addListener((obs, oldVal, newVal) -> {
                String lastApplied = (String) node.getProperties().get("gsc_last_applied_style");
                if (newVal != null && !newVal.equals(lastApplied)) {
                    scaleNode(node, fontScale);
                }
            });
        }
    }

    private static void scaleNode(Node node, double scale) {
        if (node == null) return;

        if (node instanceof Labeled labeled) {
            labeled.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            labeled.setEllipsisString("");
            if (labeled instanceof Label label) {
                label.setWrapText(true);
            }
            if (scale > 1.0) {
                if (labeled.getMinHeight() > 0 && labeled.getMinHeight() != Region.USE_COMPUTED_SIZE) {
                    Double baseMinH = (Double) labeled.getProperties().get("gsc_base_min_h");
                    if (baseMinH == null) {
                        baseMinH = labeled.getMinHeight();
                        labeled.getProperties().put("gsc_base_min_h", baseMinH);
                    }
                    labeled.setMinHeight(Math.ceil(baseMinH * scale));
                }
                if (labeled.getPrefHeight() > 0 && labeled.getPrefHeight() != Region.USE_COMPUTED_SIZE) {
                    Double basePrefH = (Double) labeled.getProperties().get("gsc_base_pref_h");
                    if (basePrefH == null) {
                        basePrefH = labeled.getPrefHeight();
                        labeled.getProperties().put("gsc_base_pref_h", basePrefH);
                    }
                    labeled.setPrefHeight(Math.ceil(basePrefH * scale));
                }
            } else if (Math.abs(scale - 1.0) < 0.01) {
                Double baseMinH = (Double) labeled.getProperties().get("gsc_base_min_h");
                if (baseMinH != null) labeled.setMinHeight(baseMinH);
                Double basePrefH = (Double) labeled.getProperties().get("gsc_base_pref_h");
                if (basePrefH != null) labeled.setPrefHeight(basePrefH);
            }
        }

        if (node instanceof Region region && !(node instanceof Labeled)) {
            if (scale > 1.0) {
                if (region.getMinHeight() > 0 && region.getMinHeight() != Region.USE_COMPUTED_SIZE) {
                    Double baseMinH = (Double) region.getProperties().get("gsc_reg_base_min_h");
                    if (baseMinH == null) {
                        baseMinH = region.getMinHeight();
                        region.getProperties().put("gsc_reg_base_min_h", baseMinH);
                    }
                    region.setMinHeight(Math.ceil(baseMinH * scale));
                }
            } else if (Math.abs(scale - 1.0) < 0.01) {
                Double baseMinH = (Double) region.getProperties().get("gsc_reg_base_min_h");
                if (baseMinH != null) region.setMinHeight(baseMinH);
            }
        }

        String currentStyle = node.getStyle();
        String lastApplied = (String) node.getProperties().get("gsc_last_applied_style");

        if (currentStyle != null && !currentStyle.trim().isEmpty()) {
            String baseStyle = (String) node.getProperties().get("gsc_base_style");
            // Si el estilo actual proviene de una invocación externa y difiere de lo que ThemeManager aplicó
            if (baseStyle == null || (lastApplied != null && !lastApplied.equals(currentStyle))) {
                baseStyle = currentStyle;
                node.getProperties().put("gsc_base_style", baseStyle);
            }

            Matcher matcher = FONT_SIZE_PATTERN.matcher(baseStyle);
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                matcher.reset();
                while (matcher.find()) {
                    try {
                        double origSize = Double.parseDouble(matcher.group(1));
                        int newSize = Math.max(8, (int) Math.round(origSize * scale));
                        matcher.appendReplacement(sb, "-fx-font-size: " + newSize + "px");
                    } catch (Exception e) {
                        matcher.appendReplacement(sb, matcher.group(0));
                    }
                }
                matcher.appendTail(sb);
                String scaled = sb.toString();
                node.getProperties().put("gsc_last_applied_style", scaled);
                node.setStyle(scaled);
                return;
            }
        }

        // Si el nodo no tiene -fx-font-size en línea, escalar si es Labeled, TextInputControl o Text
        if (node instanceof Labeled labeled) {
            Double baseSize = (Double) labeled.getProperties().get("gsc_base_font_size");
            if (baseSize == null) {
                baseSize = labeled.getFont() != null ? labeled.getFont().getSize() : 13.0;
                labeled.getProperties().put("gsc_base_font_size", baseSize);
            }
            int newSize = Math.max(8, (int) Math.round(baseSize * scale));
            String baseStyle = (String) node.getProperties().get("gsc_base_style");
            if (baseStyle == null || (lastApplied != null && !lastApplied.equals(currentStyle))) {
                baseStyle = currentStyle != null ? FONT_SIZE_PATTERN.matcher(currentStyle).replaceAll("").trim() : "";
                node.getProperties().put("gsc_base_style", baseStyle);
            }
            String clean = baseStyle.trim();
            if (!clean.isEmpty() && !clean.endsWith(";")) clean += ";";
            String scaled = clean + " -fx-font-size: " + newSize + "px;";
            node.getProperties().put("gsc_last_applied_style", scaled);
            node.setStyle(scaled);
        } else if (node instanceof TextInputControl tic) {
            Double baseSize = (Double) tic.getProperties().get("gsc_base_font_size");
            if (baseSize == null) {
                baseSize = tic.getFont() != null ? tic.getFont().getSize() : 13.0;
                tic.getProperties().put("gsc_base_font_size", baseSize);
            }
            int newSize = Math.max(8, (int) Math.round(baseSize * scale));
            String baseStyle = (String) node.getProperties().get("gsc_base_style");
            if (baseStyle == null || (lastApplied != null && !lastApplied.equals(currentStyle))) {
                baseStyle = currentStyle != null ? FONT_SIZE_PATTERN.matcher(currentStyle).replaceAll("").trim() : "";
                node.getProperties().put("gsc_base_style", baseStyle);
            }
            String clean = baseStyle.trim();
            if (!clean.isEmpty() && !clean.endsWith(";")) clean += ";";
            String scaled = clean + " -fx-font-size: " + newSize + "px;";
            node.getProperties().put("gsc_last_applied_style", scaled);
            node.setStyle(scaled);
        } else if (node instanceof Text text) {
            Double baseSize = (Double) text.getProperties().get("gsc_base_font_size");
            if (baseSize == null) {
                baseSize = text.getFont() != null ? text.getFont().getSize() : 13.0;
                text.getProperties().put("gsc_base_font_size", baseSize);
            }
            int newSize = Math.max(8, (int) Math.round(baseSize * scale));
            String baseStyle = (String) node.getProperties().get("gsc_base_style");
            if (baseStyle == null || (lastApplied != null && !lastApplied.equals(currentStyle))) {
                baseStyle = currentStyle != null ? FONT_SIZE_PATTERN.matcher(currentStyle).replaceAll("").trim() : "";
                node.getProperties().put("gsc_base_style", baseStyle);
            }
            String clean = baseStyle.trim();
            if (!clean.isEmpty() && !clean.endsWith(";")) clean += ";";
            String scaled = clean + " -fx-font-size: " + newSize + "px;";
            node.getProperties().put("gsc_last_applied_style", scaled);
            node.setStyle(scaled);
        }
    }
}
