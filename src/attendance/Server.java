package attendance;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

// Serves the HTML pages and the /api addresses the pages call with fetch().
public class Server {
    static final int PORT = 8080;
    private static final Path WEB = Path.of("web").toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {
        Db.init();
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
        } catch (BindException e) {
            System.out.println("Port " + PORT + " is already in use.");
            System.out.println("Close the other ClassRoll window, or change PORT in Server.java.");
            return;
        }
        server.createContext("/api/", Server::api);
        server.createContext("/", Server::page);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("ClassRoll is running at http://localhost:" + PORT);
        System.out.println("Leave this window open. Close it when the lesson ends.");
    }

    private static void api(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("GET".equals(method) && "/api/students".equals(path)) {
                StudentsApi.list(ex);
            } else if ("POST".equals(method) && "/api/students".equals(path)) {
                StudentsApi.create(ex);
            } else if ("PUT".equals(method) && path.startsWith("/api/students/")) {
                StudentsApi.update(ex, Api.pathId(path, "/api/students/"));
            } else if ("DELETE".equals(method) && path.startsWith("/api/students/")) {
                StudentsApi.delete(ex, Api.pathId(path, "/api/students/"));
            } else if ("GET".equals(method) && "/api/summary".equals(path)) {
                RegisterApi.summary(ex);
            } else if ("GET".equals(method) && "/api/register".equals(path)) {
                RegisterApi.load(ex);
            } else if ("POST".equals(method) && "/api/register".equals(path)) {
                RegisterApi.save(ex);
            } else if ("GET".equals(method) && "/api/report".equals(path)) {
                RegisterApi.report(ex);
            } else {
                Api.json(ex, 404, Api.errorBody("Unknown address."));
            }
        } catch (IllegalArgumentException e) {
            Api.json(ex, 400, Api.errorBody(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            String message = "Something went wrong while talking to the database.";
            if (sqlMissingTable(e)) {
                message = "The tables are missing. Run database/schema.sql in MySQL Workbench.";
            }
            Api.json(ex, 500, Api.errorBody(message));
        }
    }

    private static boolean sqlMissingTable(Throwable error) {
        while (error != null) {
            if (error instanceof java.sql.SQLException
                    && ((java.sql.SQLException) error).getErrorCode() == 1146) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }

    private static void page(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if ("/favicon.ico".equals(path)) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }
        if (!"GET".equals(ex.getRequestMethod())) {
            Api.json(ex, 405, Api.errorBody("Method not allowed."));
            return;
        }
        if ("/".equals(path)) {
            path = "/index.html";
        }
        Path file = WEB.resolve(path.substring(1)).normalize();
        if (!file.startsWith(WEB) || !Files.isRegularFile(file)) {
            byte[] missing = "Page not found".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(404, missing.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(missing);
            }
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType(file));
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        return "application/octet-stream";
    }
}
