package com.openchat4u.testutil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal .env loader for live API tests.
 * Looks for backend/.env then .env (relative to test working dir).
 */
public final class DotEnv {

    private DotEnv() {}

    private static Map<String, String> cache;

    public static synchronized Map<String, String> load() {
        if (cache != null) return cache;
        Map<String, String> m = new HashMap<>();
        Path p = Paths.get("backend/.env");
        if (!Files.exists(p)) p = Paths.get(".env");
        if (Files.exists(p)) {
            try {
                for (String line : Files.readAllLines(p)) {
                    String s = line.trim();
                    if (s.isEmpty() || s.startsWith("#")) continue;
                    int idx = s.indexOf('=');
                    if (idx <= 0) continue;
                    String key = s.substring(0, idx).trim();
                    String val = s.substring(idx + 1).trim();
                    if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                        val = val.substring(1, val.length() - 1);
                    }
                    m.put(key, val);
                }
            } catch (IOException ignored) {}
        }
        cache = m;
        return m;
    }

    public static String get(String key) {
        return load().get(key);
    }

    public static String getOr(String key, String def) {
        return load().getOrDefault(key, def);
    }

    public static boolean has(String key) {
        String v = load().get(key);
        return v != null && !v.isBlank();
    }
}
