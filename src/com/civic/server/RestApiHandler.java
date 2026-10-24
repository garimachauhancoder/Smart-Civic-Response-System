package com.civic.server;

import com.civic.model.CivicIssue;
import com.civic.service.CivicIssueService;
import com.civic.service.SmartTriageService;
import com.civic.util.SimpleJson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RestApiHandler implements HttpHandler {

    private final CivicIssueService issueService;

    public RestApiHandler(CivicIssueService issueService) {
        this.issueService = issueService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHelper.handlePreflight(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if (path.equals("/api/health") && method.equals("GET")) {
                handleHealth(exchange);
            } else if (path.equals("/api/stats") && method.equals("GET")) {
                handleStats(exchange);
            } else if (path.equals("/api/departments") && method.equals("GET")) {
                handleDepartments(exchange);
            } else if (path.equals("/api/triage") && method.equals("POST")) {
                handleTriage(exchange);
            } else if (path.equals("/api/issues") || path.equals("/api/issues/")) {
                if (method.equals("GET")) {
                    handleListIssues(exchange);
                } else if (method.equals("POST")) {
                    handleCreateIssue(exchange);
                } else {
                    CorsHelper.sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } else if (path.startsWith("/api/issues/")) {
                handleSpecificIssue(exchange, path, method);
            } else {
                CorsHelper.sendJsonResponse(exchange, 404, "{\"error\":\"API endpoint not found: " + path + "\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            CorsHelper.sendJsonResponse(exchange, 500, "{\"error\":\"Internal Server Error: " + SimpleJson.escapeString(e.getMessage()) + "\"}");
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("service", "Smart Civic Response System");
        health.put("version", "2.0.0");
        health.put("engine", "Pure Java 22 HTTP Server");
        health.put("timestamp", System.currentTimeMillis());
        CorsHelper.sendJsonResponse(exchange, 200, SimpleJson.toJson(health));
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = issueService.getStatistics();
        CorsHelper.sendJsonResponse(exchange, 200, SimpleJson.toJson(stats));
    }

    private void handleDepartments(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> depts = issueService.getDepartmentsWithWorkload();
        CorsHelper.sendJsonResponse(exchange, 200, SimpleJson.toJson(depts));
    }

    private void handleTriage(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> payload = SimpleJson.parseObject(body);
        String title = (String) payload.get("title");
        String description = (String) payload.get("description");

        SmartTriageService.TriageResult result = issueService.testTriage(title, description);
        CorsHelper.sendJsonResponse(exchange, 200, SimpleJson.toJson(result.toMap()));
    }

    private void handleListIssues(HttpExchange exchange) throws IOException {
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
        String category = queryParams.get("category");
        String status = queryParams.get("status");
        String priority = queryParams.get("priority");
        String q = queryParams.get("q");

        List<CivicIssue> issues = issueService.getAllIssues(category, status, priority, q);
        CorsHelper.sendJsonResponse(exchange, 200, SimpleJson.toJson(issues));
    }

    private void handleCreateIssue(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> payload = SimpleJson.parseObject(body);

        if (payload == null || !payload.containsKey("title")) {
            CorsHelper.sendJsonResponse(exchange, 400, "{\"error\":\"Missing required field: title\"}");
            return;
        }

        CivicIssue created = issueService.createIssue(payload);
        CorsHelper.sendJsonResponse(exchange, 201, SimpleJson.toJson(created));
    }

    private void handleSpecificIssue(HttpExchange exchange, String path, String method) throws IOException {
        // Path format: /api/issues/{id} or /api/issues/{id}/status or /api/issues/{id}/assign or /api/issues/{id}/upvote
        String sub = path.substring("/api/issues/".length());
        String[] parts = sub.split("/");
        if (parts.length == 0 || parts[0].isEmpty()) {
            CorsHelper.sendJsonResponse(exchange, 404, "{\"error\":\"Issue ID missing\"}");
            return;
        }

        String issueId = parts[0];

        if (parts.length == 1) {
            // GET /api/issues/{id}
            if (method.equals("GET")) {
                CivicIssue issue = issueService.getIssueById(issueId);
                if (issue == null) {
                    CorsHelper.sendJsonResponse(exchange, 404, "{\"error\":\"Issue not found: " + issueId + "\"}");
                } else {
                    CorsHelper.sendJsonResponse(exchange, 200, SimpleJson.toJson(issue));
                }
            } else {
                CorsHelper.sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
            return;
        }

        String action = parts[1];
        if (action.equals("status") && (method.equals("PUT") || method.equals("POST"))) {
            String body = readBody(exchange);
            Map<String, Object> payload = SimpleJson.parseObject(body);
            String status = (String) payload.get("status");
            String actor = (String) payload.get("actor");
            String notes = (String) payload.get("notes");
            String officer = (String) payload.get("officer");
            String resolutionPhotoUrl = (String) payload.get("resolutionPhotoUrl");

            if (status == null || status.trim().isEmpty()) {
                CorsHelper.sendJsonResponse(exchange, 400, "{\"error\":\"Missing status parameter\"}");
                return;
            }

            CivicIssue updated = issueService.updateStatus(issueId, status, actor, notes, officer, resolutionPhotoUrl);
            if (updated == null) {
                CorsHelper.sendJsonResponse(exchange, 404, "{\"error\":\"Issue not found: " + issueId + "\"}");
            } else {
                CorsHelper.sendJsonResponse(exchange, 200, SimpleJson.toJson(updated));
            }
        } else if (action.equals("assign") && (method.equals("PUT") || method.equals("POST"))) {
            String body = readBody(exchange);
            Map<String, Object> payload = SimpleJson.parseObject(body);
            String department = (String) payload.get("department");
            String actor = (String) payload.get("actor");
            String notes = (String) payload.get("notes");

            CivicIssue updated = issueService.assignDepartment(issueId, department, actor, notes);
            if (updated == null) {
                CorsHelper.sendJsonResponse(exchange, 404, "{\"error\":\"Issue not found: " + issueId + "\"}");
            } else {
                CorsHelper.sendJsonResponse(exchange, 200, SimpleJson.toJson(updated));
            }
        } else if (action.equals("upvote") && method.equals("POST")) {
            CivicIssue updated = issueService.upvote(issueId);
            if (updated == null) {
                CorsHelper.sendJsonResponse(exchange, 404, "{\"error\":\"Issue not found: " + issueId + "\"}");
            } else {
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("id", updated.getId());
                resp.put("upvotes", updated.getUpvotes());
                resp.put("priority", updated.getPriority().name());
                resp.put("priorityLabel", updated.getPriority().getDisplayName());
                resp.put("message", "Upvoted successfully. Community escalation updated.");
                CorsHelper.sendJsonResponse(exchange, 200, SimpleJson.toJson(resp));
            }
        } else {
            CorsHelper.sendJsonResponse(exchange, 404, "{\"error\":\"Unknown action: " + action + "\"}");
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, value);
            }
        }
        return map;
    }
}
