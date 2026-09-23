package com.gameshowcenter.offline.util;

import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Robust image loader utility for Game Show Center (Template Offline).
 * 
 * Key capabilities:
 * 1. Resolves local relative paths ("games/...", "template-offline/games/...") to valid file URIs.
 * 2. Fetches remote online images using a standard browser User-Agent to bypass CDN/Wikimedia 403 Forbidden blocks.
 * 3. Provides asynchronous image loading that never freezes the JavaFX UI thread.
 * 4. Provides robust isReachable() validation with redirect following and HEAD/GET fallback.
 */
public final class ImageLoaderHelper {

    private static final String DEFAULT_USER_AGENT = 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 GameShowCenter/1.0";

    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "ImageLoaderHelper-Worker");
        t.setDaemon(true);
        return t;
    });

    // In-memory cache for thumbnails to avoid duplicate network fetches
    private static final Map<String, byte[]> MEMORY_CACHE = new ConcurrentHashMap<>();

    private ImageLoaderHelper() {}

    /**
     * Resolves a relative or absolute path to an existing File on disk.
     * Checks multiple standard base directories used in the project.
     */
    public static File resolveLocalFile(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        String clean = path.trim();

        // Handle file: URI
        if (clean.startsWith("file:")) {
            try {
                return new File(new URI(clean));
            } catch (Exception ex) {
                try {
                    return new File(clean.substring(5));
                } catch (Exception ignored) {}
            }
        }

        // 1. Direct path relative to current working directory
        File f1 = new File(clean);
        if (f1.exists()) {
            return f1;
        }

        // 2. Relative to template-offline if running from workspace root
        File f2 = new File("template-offline", clean);
        if (f2.exists()) {
            return f2;
        }

        // 3. If path started with "template-offline/", try stripping it if running inside template-offline
        if (clean.startsWith("template-offline/") || clean.startsWith("template-offline\\")) {
            String stripped = clean.substring("template-offline/".length());
            File f3 = new File(stripped);
            if (f3.exists()) {
                return f3;
            }
        }

        // 4. Return initial File as fallback
        return f1;
    }

    /**
     * Converts a string (URL, local path, or file: URI) into a safe URI string that JavaFX Image can load.
     */
    public static String resolveImageUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return null;
        }

        String clean = pathOrUrl.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            return clean;
        }

        if (clean.startsWith("file:")) {
            return clean;
        }

        File local = resolveLocalFile(clean);
        if (local != null && local.exists()) {
            return local.toURI().toString();
        }

        // Fallback: convert file path to URI
        return new File(clean).toURI().toString();
    }

    /**
     * Loads a JavaFX Image synchronously with custom User-Agent for remote URLs.
     */
    public static Image loadImage(String src, double width, double height, boolean preserveRatio, boolean smooth) {
        if (src == null || src.isBlank()) {
            return null;
        }

        String clean = src.trim();

        // Remote HTTP / HTTPS URL
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            byte[] cached = MEMORY_CACHE.get(clean);
            if (cached != null) {
                try {
                    return new Image(new ByteArrayInputStream(cached), width, height, preserveRatio, smooth);
                } catch (Exception ignored) {}
            }

            try {
                byte[] data = downloadBytes(clean);
                if (data != null && data.length > 0) {
                    if (data.length <= 4 * 1024 * 1024) { // Cache images <= 4MB
                        MEMORY_CACHE.put(clean, data);
                    }
                    return new Image(new ByteArrayInputStream(data), width, height, preserveRatio, smooth);
                }
            } catch (Exception ex) {
                System.err.println("ImageLoaderHelper: Fallback loading for " + clean + ": " + ex.getMessage());
            }

            // Fallback to JavaFX native image loading
            return new Image(clean, width, height, preserveRatio, smooth, true);
        }

        // Local file or file: URI
        String fileUri = resolveImageUrl(clean);
        return new Image(fileUri, width, height, preserveRatio, smooth, true);
    }

    /**
     * Loads an image asynchronously in a worker thread and delivers it via JavaFX Platform.runLater.
     */
    public static void loadImageAsync(String src, double width, double height, boolean preserveRatio, boolean smooth,
                                      Consumer<Image> onSuccess, Runnable onError) {
        if (src == null || src.isBlank()) {
            if (onError != null) Platform.runLater(onError);
            return;
        }

        IMAGE_EXECUTOR.submit(() -> {
            try {
                Image img = loadImage(src, width, height, preserveRatio, smooth);
                if (img != null) {
                    if (img.isBackgroundLoading()) {
                        img.progressProperty().addListener((obs, oldV, newV) -> {
                            if (newV.doubleValue() >= 1.0) {
                                if (img.isError()) {
                                    if (onError != null) Platform.runLater(onError);
                                } else {
                                    if (onSuccess != null) Platform.runLater(() -> onSuccess.accept(img));
                                }
                            }
                        });
                        img.errorProperty().addListener((obs, oldV, isErr) -> {
                            if (isErr && onError != null) {
                                Platform.runLater(onError);
                            }
                        });
                    } else if (img.isError()) {
                        if (onError != null) Platform.runLater(onError);
                    } else {
                        if (onSuccess != null) Platform.runLater(() -> onSuccess.accept(img));
                    }
                } else {
                    if (onError != null) Platform.runLater(onError);
                }
            } catch (Exception ex) {
                if (onError != null) Platform.runLater(onError);
            }
        });
    }

    /**
     * Checks if an image reference (local or remote) is accessible and valid.
     */
    public static boolean isUrlReachable(String src) {
        if (src == null || src.isBlank()) {
            return false;
        }

        String clean = src.trim();

        // 1. Local file or file: URI
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            File f = resolveLocalFile(clean);
            return f != null && f.exists() && f.isFile() && f.length() > 0;
        }

        // 2. Remote HTTP/HTTPS URL
        try {
            return checkRemoteReachable(clean);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkRemoteReachable(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                return true;
            }

            // Fallback for servers that reject HEAD (403, 405, 429) or require GET
            if (code == 403 || code == 405 || code == 429 || code == 501) {
                HttpURLConnection getConn = (HttpURLConnection) url.openConnection();
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(4000);
                getConn.setReadTimeout(4000);
                getConn.setInstanceFollowRedirects(true);
                getConn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
                getConn.setRequestProperty("Range", "bytes=0-1024");

                int getCode = getConn.getResponseCode();
                return (getCode >= 200 && getCode < 400);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] downloadBytes(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(12000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

            int code = conn.getResponseCode();

            // Handle manual redirects across HTTP <-> HTTPS if needed
            if (code == 301 || code == 302 || code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                if (loc != null && !loc.isBlank()) {
                    return downloadBytes(loc);
                }
            }

            if (code < 200 || code >= 400) {
                return null;
            }

            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                int total = 0;
                while ((n = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, n);
                    total += n;
                    if (total > 20 * 1024 * 1024) { // Cap at 20MB
                        break;
                    }
                }
                return baos.toByteArray();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
