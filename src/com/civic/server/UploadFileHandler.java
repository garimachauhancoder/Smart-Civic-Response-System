package com.civic.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class UploadFileHandler implements HttpHandler {
    private final Path root;
    UploadFileHandler(Path root) { this.root = root.toAbsolutePath().normalize(); }
    @Override public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.enableCors(exchange);
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); exchange.close(); return;
        }
        String name = exchange.getRequestURI().getPath().substring("/uploads/".length());
        if (!name.matches("[a-f0-9-]{36}\\.(jpg|png|webp)")) {
            exchange.sendResponseHeaders(404, -1); exchange.close(); return;
        }
        Path file = root.resolve(name).normalize();
        if (!file.getParent().equals(root) || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1); exchange.close(); return;
        }
        String type = name.endsWith(".jpg") ? "image/jpeg" : name.endsWith(".png") ? "image/png" : "image/webp";
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); exchange.close(); return; }
        long length = Files.size(file);
        exchange.sendResponseHeaders(200, length);
        try (var out = exchange.getResponseBody()) { Files.copy(file, out); }
    }
}
