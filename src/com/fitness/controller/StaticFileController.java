package com.fitness.controller;

import com.fitness.config.AppConfig;
import com.fitness.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileController implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtil.setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        Path target = Paths.get(AppConfig.WEB_DIR, path).normalize();
        Path base = Paths.get(AppConfig.WEB_DIR).normalize();

        // Prevent directory traversal
        if (!target.startsWith(base) || !Files.exists(target) || Files.isDirectory(target)) {
            // Check if fallback to index.html for client-side routing
            Path fallback = Paths.get(AppConfig.WEB_DIR, "index.html");
            if (Files.exists(fallback)) {
                serveFile(exchange, fallback, "text/html; charset=UTF-8");
                return;
            }
            HttpUtil.sendErrorResponse(exchange, 404, "File not found: " + path);
            return;
        }

        String mimeType = getMimeType(target.toString());
        serveFile(exchange, target, mimeType);
    }

    private void serveFile(HttpExchange exchange, Path file, String mimeType) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
