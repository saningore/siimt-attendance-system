package attendance;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// Small helpers for query strings, JSON, and HTTP responses.
public class Api {
    static Map<String, String> query(HttpExchange ex) {
        Map<String, String> map = new HashMap<String, String>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        String[] parts = raw.split("&");
        for (int i = 0; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = URLDecoder.decode(parts[i].substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(parts[i].substring(eq + 1), StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    static JSONObject body(HttpExchange ex) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int total = 0;
        try (InputStream in = ex.getRequestBody()) {
            int n;
            while ((n = in.read(chunk)) != -1) {
                total += n;
                if (total > 1000000) {
                    throw new IllegalArgumentException("Request is too large.");
                }
                buffer.write(chunk, 0, n);
            }
        }
        String text = buffer.toString(StandardCharsets.UTF_8).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Missing request data.");
        }
        try {
            return new JSONObject(text);
        } catch (JSONException e) {
            throw new IllegalArgumentException("The data sent was not valid JSON.");
        }
    }

    static int pathId(String path, String prefix) {
        String rest = path.substring(prefix.length());
        if (rest.endsWith("/")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        if (rest.isEmpty() || rest.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Missing student id.");
        }
        try {
            int id = Integer.parseInt(rest);
            if (id < 1) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Student id must be a number.");
        }
    }

    static String text(JSONObject obj, String key) {
        if (!obj.has(key) || obj.isNull(key)) {
            return "";
        }
        return obj.optString(key, "").trim();
    }

    static void json(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    static String errorBody(String message) {
        JSONObject obj = new JSONObject();
        obj.put("error", message);
        return obj.toString();
    }
}
