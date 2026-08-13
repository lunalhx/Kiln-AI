package cn.lunalhx.ai.kilnai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class DotEnv {

    static Map<String, Object> read() {
        return readFrom(Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize());
    }

    static Map<String, Object> readFrom(Path start) {
        Map<String, Object> values = new LinkedHashMap<>();
        Path example = locate(start, "env.example");
        if (example != null) {
            values.putAll(read(example));
        }
        Path local = locate(start, ".env");
        if (local != null) {
            values.putAll(read(local));
        }
        return values;
    }

    static Map<String, Object> read(Path file) {
        try {
            Map<String, Object> values = new LinkedHashMap<>();
            parse(Files.readString(file, StandardCharsets.UTF_8)).forEach(values::put);
            return values;
        } catch (IOException ignored) {
            return Map.of();
        }
    }

    static Path locate(Path start, String name) {
        Path dir = start;
        for (int depth = 0; depth < 6 && dir != null; depth++) {
            Path candidate = dir.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    static Map<String, String> parse(String text) {
        String body = text.startsWith("\uFEFF") ? text.substring(1) : text;
        Map<String, String> values = new LinkedHashMap<>();
        for (String raw : body.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            values.put(line.substring(0, eq).trim(), unquote(line.substring(eq + 1).trim()));
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        int comment = value.indexOf(" #");
        if (comment >= 0) {
            return value.substring(0, comment).trim();
        }
        return value;
    }

    private DotEnv() {
    }
}
