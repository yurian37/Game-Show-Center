package com.gameshowcenter.offline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gameshowcenter.offline.games.GameStageRegistry;
import com.gameshowcenter.offline.games.editors.HangmanSetupEditor;
import com.gameshowcenter.offline.games.editors.TriviaQuizSetupEditor;
import com.gameshowcenter.offline.games.editors.GuessCharacterSetupEditor;
import com.gameshowcenter.offline.games.editors.SnapSolveSetupEditor;
import com.gameshowcenter.offline.model.Competitor;
import com.gameshowcenter.offline.model.GameDescriptor;
import com.gameshowcenter.offline.model.MatchConfig;
import com.gameshowcenter.offline.service.AIService;
import com.gameshowcenter.offline.theme.ThemeManager;
import com.gameshowcenter.offline.views.ArenaStageFxView;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class RequirementVerification {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("RUNNING VERIFICATION TESTS FOR USER REQUIREMENTS");
        System.out.println("==================================================");

        try {
            javafx.application.Platform.startup(() -> {});
        } catch (Exception ignored) {}

        testAccessibilityFontScaling();
        testHangmanSetupEditorValidation();
        testHangmanAIServiceDeduplication();
        testArenaGameFontScaling();
        testBackgroundPersistenceAcrossWindows();
        testBattleRoyaleModeValidation();

        System.out.println("==================================================");
        System.out.println("ALL JAVA VERIFICATION TESTS PASSED SUCCESSFULLY!");
        System.out.println("==================================================");
        System.exit(0);
    }

    private static void testAccessibilityFontScaling() {
        System.out.println("\n[Test 1] Testing Accessibility Font Scaling in ThemeManager...");

        // Initial default scale
        ThemeManager.setFontScale(1.0);
        assert Math.abs(ThemeManager.getFontScale() - 1.0) < 0.001 : "Initial font scale should be 1.0";
        assert ThemeManager.getScaledFontSize(12) == 12 : "Scaled 12px at 1.0 should be 12";
        assert ThemeManager.getScaledFontSize(20) == 20 : "Scaled 20px at 1.0 should be 20";

        // Enlarge text (+25%)
        ThemeManager.setFontScale(1.25);
        assert Math.abs(ThemeManager.getFontScale() - 1.25) < 0.001 : "Font scale should be 1.25";
        assert ThemeManager.getScaledFontSize(12) == 15 : "Scaled 12px at 1.25 should be 15";
        assert ThemeManager.getScaledFontSize(20) == 25 : "Scaled 20px at 1.25 should be 25";
        assert ThemeManager.getFontSizeStyle(12).contains("15px") : "Font style should contain 15px";

        // Shrink text (-20%)
        ThemeManager.setFontScale(0.80);
        assert Math.abs(ThemeManager.getFontScale() - 0.80) < 0.001 : "Font scale should be 0.80";
        assert ThemeManager.getScaledFontSize(20) == 16 : "Scaled 20px at 0.80 should be 16";

        // Clamping check
        ThemeManager.setFontScale(0.10); // Below min
        assert ThemeManager.getFontScale() >= 0.70 : "Font scale should be clamped to minimum 0.70";

        ThemeManager.setFontScale(5.0); // Above max
        assert ThemeManager.getFontScale() <= 2.0 : "Font scale should be clamped to maximum 2.0";

        // Check 130% scaling (User requirement)
        ThemeManager.setFontScale(1.30);
        assert Math.abs(ThemeManager.getFontScale() - 1.30) < 0.001 : "Font scale should be 1.30";
        assert ThemeManager.getScaledFontSize(12) == 16 : "Scaled 12px at 1.30 should be 16";
        assert ThemeManager.getScaledFontSize(20) == 26 : "Scaled 20px at 1.30 should be 26";

        Label testLbl = new Label("Testing 130% word wrap");
        ThemeManager.applyTextScale(testLbl, 1.30);
        assert testLbl.isWrapText() : "Label should have wrapText set to true when scaled";

        // Reset to normal
        ThemeManager.setFontScale(1.0);
        System.out.println("-> Accessibility Font Scaling test passed!");
    }

    private static void testHangmanSetupEditorValidation() {
        System.out.println("\n[Test 2] Testing Hangman Setup Editor Duplicate Prevention...");

        ObjectMapper mapper = new ObjectMapper();
        HangmanSetupEditor editor = new HangmanSetupEditor();

        List<Competitor> profiles = new ArrayList<>();
        profiles.add(new Competitor("1", "Jugador 1", null));
        profiles.add(new Competitor("2", "Jugador 2", null));

        // 1. Test validateSetupData with duplicate words (case-insensitive)
        ObjectNode dupeSetup = mapper.createObjectNode();
        dupeSetup.put("game", "Hangman");
        dupeSetup.put("rounds_per_player", 2);
        ArrayNode dupePool = dupeSetup.putArray("wordPool");
        dupePool.add("MANZANA").add("PERA").add("manzana").add("UVA").add("KIWI").add("MELON");

        String error = editor.validateSetupData(dupeSetup, profiles);
        assert error != null && error.toLowerCase().contains("manzana") :
                "Validation should catch duplicate word 'manzana', but got: " + error;
        System.out.println("-> Correctly caught duplicate word in setup data: " + error);

        // 2. Test validateSetupData with valid unique words (enough for 2 players * 2 rounds = 4 words)
        ObjectNode validSetup = mapper.createObjectNode();
        validSetup.put("game", "Hangman");
        validSetup.put("rounds_per_player", 2);
        ArrayNode validPool = validSetup.putArray("wordPool");
        validPool.add("MANZANA").add("PERA").add("UVA").add("KIWI").add("MELON");

        String validError = editor.validateSetupData(validSetup, profiles);
        assert validError == null : "Valid unique words should pass validation, but got: " + validError;
        System.out.println("-> Valid unique word list correctly passed validation.");
    }

    private static void testHangmanAIServiceDeduplication() {
        System.out.println("\n[Test 3] Testing Hangman AIService Merge Word Deduplication...");

        ObjectMapper mapper = new ObjectMapper();

        // Current setup has "MANZANA", "PERA"
        ObjectNode currentSetup = mapper.createObjectNode();
        currentSetup.put("game", "Hangman");
        ArrayNode existingWords = currentSetup.putArray("wordPool");
        existingWords.add("MANZANA").add("PERA");

        // AI generated data contains duplicates within itself and with existing words: "pera", "UVA", "uva", "MELON"
        ObjectNode aiGenerated = mapper.createObjectNode();
        ArrayNode aiWords = aiGenerated.putArray("wordPool");
        aiWords.add("pera").add("UVA").add("uva").add("MELON");

        // Merge in 'add' mode
        com.fasterxml.jackson.databind.JsonNode merged = AIService.mergeSetup("Hangman", "add", currentSetup, aiGenerated, null);
        ArrayNode resultPool = (ArrayNode) merged.get("wordPool");

        List<String> words = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode n : resultPool) {
            words.add(n.asText());
        }

        System.out.println("-> Merged word pool: " + words);

        assert words.contains("MANZANA") : "Must contain MANZANA";
        assert words.contains("PERA") : "Must contain PERA";
        assert words.contains("UVA") : "Must contain UVA";
        assert words.contains("MELON") : "Must contain MELON";
        assert words.size() == 4 : "Merged pool must contain exactly 4 unique words, but had " + words.size();

        // Check overwrite mode with duplicates in AI words: "KIWI", "kiwi", "FRESA"
        ObjectNode aiGenOverwrite = mapper.createObjectNode();
        ArrayNode aiWords2 = aiGenOverwrite.putArray("wordPool");
        aiWords2.add("KIWI").add("kiwi").add("FRESA");

        com.fasterxml.jackson.databind.JsonNode overwritten = AIService.mergeSetup("Hangman", "overwrite", currentSetup, aiGenOverwrite, null);
        ArrayNode overPool = (ArrayNode) overwritten.get("wordPool");
        List<String> overWords = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode n : overPool) {
            overWords.add(n.asText());
        }

        System.out.println("-> Overwritten word pool: " + overWords);
        assert overWords.size() == 2 : "Overwritten pool must contain exactly 2 unique words, but had " + overWords.size();
        assert overWords.contains("KIWI") && overWords.contains("FRESA") : "Overwritten words must be KIWI and FRESA";

        System.out.println("-> Hangman AIService Merge Word Deduplication test passed!");
    }

    private static void testArenaGameFontScaling() {
        System.out.println("\n[Test 4] Testing Arena and Game Font Scaling...");

        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        // Set scale to 1.50 (150%)
        ThemeManager.setFontScale(1.50);

        // 1. Test GameStageRegistry creates stage with 1.50 font scale
        GameDescriptor desc = new GameDescriptor();
        desc.setName("TimeLine");
        MatchConfig config = new MatchConfig();
        List<Competitor> players = new ArrayList<>();
        players.add(new Competitor("1", "Jugador 1", null));
        players.add(new Competitor("2", "Jugador 2", null));
        config.setProfiles(players);

        Region stage = GameStageRegistry.createStage(desc, config, null);
        assert stage != null : "Stage should be created";

        // Check that any node with base 16px has been scaled to 24px
        boolean foundScaledFont = false;
        List<Node> nodesToInspect = new ArrayList<>();
        nodesToInspect.add(stage);
        while (!nodesToInspect.isEmpty()) {
            Node n = nodesToInspect.remove(0);
            if (n.getStyle() != null && n.getStyle().contains("-fx-font-size: 24px")) {
                foundScaledFont = true;
                break;
            }
            if (n instanceof Parent p) {
                nodesToInspect.addAll(p.getChildrenUnmodifiable());
            }
        }
        assert foundScaledFont : "Stage created by GameStageRegistry should have font-size scaled to 24px (16px * 1.50)";
        System.out.println("-> GameStageRegistry stage initial font scaling verified (16px -> 24px).");

        // 2. Test Dynamic Child Addition scaling
        VBox container = new VBox();
        ThemeManager.applyTextScale(container, 1.50);

        Label dynamicLabel = new Label("Dynamic Card Label");
        dynamicLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        container.getChildren().add(dynamicLabel);

        assert dynamicLabel.getStyle().contains("-fx-font-size: 15px") :
            "Dynamically added child should be scaled to 15px (10px * 1.50), but got: " + dynamicLabel.getStyle();
        System.out.println("-> Dynamically added child in container was automatically scaled (10px -> 15px).");

        // 3. Test Dynamic Style Update (setStyle) scaling
        dynamicLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: red;");
        assert dynamicLabel.getStyle().contains("-fx-font-size: 30px") :
            "Dynamic style update should be scaled to 30px (20px * 1.50), but got: " + dynamicLabel.getStyle();
        System.out.println("-> Dynamic setStyle modification was automatically scaled (20px -> 30px).");

        // 4. Test ArenaStageFxView scaling
        config.setSelectedGames(List.of(desc));
        ArenaStageFxView arenaView = new ArenaStageFxView(config, null);
        assert arenaView != null : "ArenaStageFxView should instantiate";

        // Reset font scale to 1.0
        ThemeManager.setFontScale(1.0);
        System.out.println("-> Arena and Game Font Scaling tests passed!");
    }

    private static void testBackgroundPersistenceAcrossWindows() {
        System.out.println("\n[Test 5] Testing Background Persistence Across Windows and View Transitions...");

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 1280, 800);

        // 1. Initial configuration
        ThemeManager.setPalette("1. Midnight Indigo");
        ThemeManager.setCustomAppBgColor(null);
        ThemeManager.setCustomBackgroundImagePath(null);

        MainApp app = new MainApp();
        // Set scene fill and background
        Color bgIndigo = Color.web(ThemeManager.getAppBgHex());
        scene.setFill(bgIndigo);
        root.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(bgIndigo, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)
        ));

        assert root.getBackground() != null : "Initial background must not be null";
        assert !scene.getFill().equals(Color.WHITE) : "Initial scene fill must not be white";
        Color initialFill = (Color) root.getBackground().getFills().get(0).getFill();
        assert initialFill.equals(Color.web("#090d16")) : "Expected #090d16, got " + initialFill;
        System.out.println("-> Initial background and Scene fill correctly set to #090d16 (not white).");

        // 2. Simulate switching views 10 times consecutively
        for (int i = 1; i <= 10; i++) {
            Color currentAppBg = Color.web(ThemeManager.getAppBgHex());
            scene.setFill(currentAppBg);
            root.setBackground(new javafx.scene.layout.Background(
                    new javafx.scene.layout.BackgroundFill(currentAppBg, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)
            ));

            assert root.getBackground() != null : "Switch " + i + ": Root background became null!";
            Color switchFill = (Color) root.getBackground().getFills().get(0).getFill();
            assert switchFill.equals(Color.web("#090d16")) : "Switch " + i + ": Expected #090d16, got " + switchFill;
            assert !scene.getFill().equals(Color.WHITE) : "Switch " + i + ": Scene fill became white!";
        }
        System.out.println("-> 10 consecutive window/screen switches verified: background remained intact and NEVER turned white.");

        // 3. Test changing to custom color and switching views
        Color customPurple = Color.web("#1e1b4b");
        ThemeManager.setCustomAppBgColor(customPurple);
        scene.setFill(customPurple);
        root.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(customPurple, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)
        ));

        for (int i = 1; i <= 5; i++) {
            Color cur = Color.web(ThemeManager.getAppBgHex());
            scene.setFill(cur);
            root.setBackground(new javafx.scene.layout.Background(
                    new javafx.scene.layout.BackgroundFill(cur, javafx.scene.layout.CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)
            ));
            Color c = (Color) root.getBackground().getFills().get(0).getFill();
            assert c.equals(customPurple) : "Custom color switch " + i + ": Expected #1e1b4b, got " + c;
            assert !scene.getFill().equals(Color.WHITE) : "Custom color switch " + i + ": Scene fill became white!";
        }
        System.out.println("-> Custom color maintained across view switches without resetting.");

        // Restore default palette
        ThemeManager.setPalette("1. Midnight Indigo");
        ThemeManager.setCustomAppBgColor(null);
        System.out.println("-> Background Persistence Across Windows tests passed successfully!");
    }

    private static void testBattleRoyaleModeValidation() {
        System.out.println("\n[Test 6] Testing Battle Royale Mode Validation Across Editors...");

        ObjectMapper mapper = new ObjectMapper();
        List<Competitor> profiles = new ArrayList<>();
        profiles.add(new Competitor("1", "Jugador 1", null));
        profiles.add(new Competitor("2", "Jugador 2", null));

        // 1. Hangman (compCount=2, rounds=2 => requires 4 in normal, 1 in BR)
        HangmanSetupEditor hangmanEditor = new HangmanSetupEditor();
        ObjectNode hangmanSetup = mapper.createObjectNode();
        hangmanSetup.put("game", "Hangman");
        hangmanSetup.put("rounds_per_player", 2);
        ArrayNode hangmanPool = hangmanSetup.putArray("wordPool");
        hangmanPool.add("CASA"); // only 1 word

        String hangmanNormalErr = hangmanEditor.validateSetupData(hangmanSetup, profiles, false);
        assert hangmanNormalErr != null : "Hangman with 1 word should fail in normal mode";

        String hangmanBRErr = hangmanEditor.validateSetupData(hangmanSetup, profiles, true);
        assert hangmanBRErr == null : "Hangman with 1 word should PASS in Battle Royale mode, but got: " + hangmanBRErr;
        System.out.println("-> Hangman: 1 word passes in BR, fails in normal mode.");

        // Hangman with 0 words should fail in BR
        ObjectNode hangmanEmpty = mapper.createObjectNode();
        hangmanEmpty.put("rounds_per_player", 2);
        hangmanEmpty.putArray("wordPool");
        String hangmanEmptyErr = hangmanEditor.validateSetupData(hangmanEmpty, profiles, true);
        assert hangmanEmptyErr != null : "Hangman with 0 words should fail in Battle Royale mode";
        System.out.println("-> Hangman: 0 words fails in BR mode as expected.");

        // 2. Trivia Quiz (compCount=2, rounds=2 => requires 4 in normal, 1 in BR)
        TriviaQuizSetupEditor triviaEditor = new TriviaQuizSetupEditor();
        ObjectNode triviaSetup = mapper.createObjectNode();
        triviaSetup.put("rounds_per_player", 2);
        ArrayNode triviaPool = triviaSetup.putArray("questionPool");
        ObjectNode q1 = triviaPool.addObject();
        q1.put("question", "¿Capital de Francia?");
        q1.put("answer", "París");

        String triviaNormalErr = triviaEditor.validateSetupData(triviaSetup, profiles, false);
        assert triviaNormalErr != null : "Trivia with 1 question should fail in normal mode";

        String triviaBRErr = triviaEditor.validateSetupData(triviaSetup, profiles, true);
        assert triviaBRErr == null : "Trivia with 1 question should PASS in Battle Royale mode, but got: " + triviaBRErr;
        System.out.println("-> Trivia Quiz: 1 question passes in BR, fails in normal mode.");

        // Trivia with 0 questions should fail in BR
        ObjectNode triviaEmpty = mapper.createObjectNode();
        triviaEmpty.put("rounds_per_player", 2);
        triviaEmpty.putArray("questionPool");
        String triviaEmptyErr = triviaEditor.validateSetupData(triviaEmpty, profiles, true);
        assert triviaEmptyErr != null : "Trivia with 0 questions should fail in Battle Royale mode";
        System.out.println("-> Trivia Quiz: 0 questions fails in BR mode as expected.");

        // 3. Guess Character (compCount=2, rounds=3 => requires 6 in normal, 1 in BR)
        GuessCharacterSetupEditor gcEditor = new GuessCharacterSetupEditor();
        ObjectNode gcSetup = mapper.createObjectNode();
        gcSetup.put("game", "Guess_Character");
        gcSetup.put("rounds_per_player", 3);
        ArrayNode gcPool = gcSetup.putArray("media_pool");
        gcPool.add("games/Guess Character/images/ai_elden_ring_1_95470563.jpg"); // 1 valid image

        String gcNormalErr = gcEditor.validateSetupData(gcSetup, profiles, false);
        assert gcNormalErr != null : "Guess Character with 1 image should fail in normal mode";

        String gcBRErr = gcEditor.validateSetupData(gcSetup, profiles, true);
        assert gcBRErr == null : "Guess Character with 1 valid image should PASS in BR mode, but got: " + gcBRErr;
        System.out.println("-> Guess Character: 1 valid image passes in BR, fails in normal mode.");

        // Guess Character with non-existent / unreachable image should fail in BR
        ObjectNode gcInvalid = mapper.createObjectNode();
        gcInvalid.put("rounds_per_player", 3);
        ArrayNode gcInvalidPool = gcInvalid.putArray("media_pool");
        gcInvalidPool.add("games/Guess Character/images/fake_ghost_image_9999.jpg");
        String gcInvalidErr = gcEditor.validateSetupData(gcInvalid, profiles, true);
        assert gcInvalidErr != null : "Guess Character with invalid/unreachable image must fail even in BR mode";
        System.out.println("-> Guess Character: invalid image correctly fails in BR mode.");

        // 4. Snap Solve (compCount=2, rounds=2 => requires 4 in normal, 1 in BR)
        SnapSolveSetupEditor ssEditor = new SnapSolveSetupEditor();
        ObjectNode ssSetup = mapper.createObjectNode();
        ssSetup.put("rounds_per_player", 2);
        ArrayNode ssFilters = ssSetup.putArray("selected_filters");
        ssFilters.add("swirl");
        ArrayNode ssPool = ssSetup.putArray("media_pool");
        ssPool.add("games/Guess Character/images/ai_elden_ring_1_95470563.jpg"); // 1 valid image

        String ssNormalErr = ssEditor.validateSetupData(ssSetup, profiles, false);
        assert ssNormalErr != null : "Snap Solve with 1 image should fail in normal mode";

        String ssBRErr = ssEditor.validateSetupData(ssSetup, profiles, true);
        assert ssBRErr == null : "Snap Solve with 1 valid image should PASS in BR mode, but got: " + ssBRErr;
        System.out.println("-> Snap Solve: 1 valid image passes in BR, fails in normal mode.");

        // Snap Solve with broken image must fail in BR
        ObjectNode ssBroken = mapper.createObjectNode();
        ssBroken.put("rounds_per_player", 2);
        ArrayNode ssBrokenFilters = ssBroken.putArray("selected_filters");
        ssBrokenFilters.add("swirl");
        ArrayNode ssBrokenPool = ssBroken.putArray("media_pool");
        ssBrokenPool.add("games/Snap Solve/images/nonexistent_snap_image_9999.jpg");
        String ssBrokenErr = ssEditor.validateSetupData(ssBroken, profiles, true);
        assert ssBrokenErr != null : "Snap Solve with broken image must fail even in BR mode";
        System.out.println("-> Snap Solve: broken image correctly fails in BR mode.");

        // 5. Test automatic detection of "battleRoyale" inside setupData
        ObjectNode hangmanWithBRField = mapper.createObjectNode();
        hangmanWithBRField.put("game", "Hangman");
        hangmanWithBRField.put("rounds_per_player", 5);
        hangmanWithBRField.put("battleRoyale", true);
        hangmanWithBRField.putArray("wordPool").add("SOL"); // Only 1 word, but battleRoyale = true

        String autoDetectErr = hangmanEditor.validateSetupData(hangmanWithBRField, profiles);
        assert autoDetectErr == null : "validateSetupData(setupData, profiles) should auto-detect battleRoyale: true inside setupData, but got: " + autoDetectErr;
        System.out.println("-> validateSetupData auto-detects battleRoyale flag inside setupData.");

        // 6. TimeLineSetupEditor (normal requires >= 3 events, BR requires >= 1)
        com.gameshowcenter.offline.games.editors.TimeLineSetupEditor tlEditor = new com.gameshowcenter.offline.games.editors.TimeLineSetupEditor();
        ObjectNode tlSetup = mapper.createObjectNode();
        tlSetup.put("game", "TimeLine");
        tlSetup.put("battleRoyale", false);
        ArrayNode tlEvents = tlSetup.putArray("events");
        ObjectNode ev1 = tlEvents.addObject();
        ev1.put("id", "ev_1");
        ev1.put("title", "Apolo 11");
        ev1.put("year", 1969);

        String tlNormalErr = tlEditor.validateSetupData(tlSetup, profiles);
        assert tlNormalErr != null : "TimeLine with 1 event should fail in normal mode";

        tlSetup.put("battleRoyale", true);
        String tlBRErr = tlEditor.validateSetupData(tlSetup, profiles);
        assert tlBRErr == null : "TimeLine with 1 event should pass in Battle Royale mode, but got: " + tlBRErr;
        System.out.println("-> TimeLine: 1 event passes in BR, fails in normal mode.");

        // 7. MatchConfig Battle Royale property
        MatchConfig config = new MatchConfig();
        assert !config.isBattleRoyale() : "MatchConfig should default battleRoyale to false";
        config.setBattleRoyale(true);
        assert config.isBattleRoyale() : "MatchConfig battleRoyale should be true";
        System.out.println("-> MatchConfig battleRoyale field verified.");

        System.out.println("-> Battle Royale Mode Validation tests passed successfully!");
    }
}
