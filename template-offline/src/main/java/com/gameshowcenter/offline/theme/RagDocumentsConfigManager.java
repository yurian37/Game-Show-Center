package com.gameshowcenter.offline.theme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages configuration and persistence of additional knowledge documents (PDF only, max 10)
 * used to augment context for all system AIs (text generation & image/visual analysis).
 */
public class RagDocumentsConfigManager {

    public static final int MAX_RAG_DOCUMENTS = 10;

    private static final List<String> ragDocuments = new ArrayList<>();
    private static final File PREFS_FILE = new File("preferences.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        loadPreferences();
    }

    public static synchronized void loadPreferences() {
        File f = PREFS_FILE;
        if (!f.exists()) {
            f = new File("template-offline/preferences.json");
        }
        if (!f.exists()) {
            return;
        }
        try {
            JsonNode root = MAPPER.readTree(f);
            if (root.has("rag_documents") && root.get("rag_documents").isArray()) {
                ragDocuments.clear();
                for (JsonNode item : root.get("rag_documents")) {
                    if (item.isTextual() && ragDocuments.size() < MAX_RAG_DOCUMENTS) {
                        String docPath = item.asText().trim();
                        if (!docPath.isEmpty() && docPath.toLowerCase().endsWith(".pdf") && !ragDocuments.contains(docPath)) {
                            ragDocuments.add(docPath);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static synchronized void savePreferences() {
        try {
            File f = PREFS_FILE;
            if (!f.exists() && new File("template-offline/preferences.json").exists()) {
                f = new File("template-offline/preferences.json");
            }
            ObjectNode root = f.exists() ? (ObjectNode) MAPPER.readTree(f) : MAPPER.createObjectNode();

            ArrayNode arr = MAPPER.createArrayNode();
            for (String doc : ragDocuments) {
                arr.add(doc);
            }
            root.set("rag_documents", arr);

            MAPPER.writerWithDefaultPrettyPrinter().writeValue(f, root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static synchronized List<String> getRagDocuments() {
        return Collections.unmodifiableList(new ArrayList<>(ragDocuments));
    }

    public static synchronized boolean addRagDocument(String path) {
        if (path == null) return false;
        String trimmed = path.trim();
        if (trimmed.isEmpty() || !trimmed.toLowerCase().endsWith(".pdf")) {
            return false;
        }
        if (ragDocuments.size() >= MAX_RAG_DOCUMENTS) {
            return false;
        }
        if (ragDocuments.contains(trimmed)) {
            return false;
        }
        ragDocuments.add(trimmed);
        savePreferences();
        return true;
    }

    public static synchronized boolean removeRagDocument(String path) {
        if (path == null) return false;
        boolean removed = ragDocuments.remove(path.trim());
        if (removed) {
            savePreferences();
        }
        return removed;
    }

    public static synchronized void clearRagDocuments() {
        ragDocuments.clear();
        savePreferences();
    }

    public static synchronized int getDocumentCount() {
        return ragDocuments.size();
    }
}
