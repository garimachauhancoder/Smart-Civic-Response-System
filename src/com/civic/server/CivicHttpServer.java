package com.civic.server;

import com.civic.service.CivicIssueService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;

public class CivicHttpServer {

    private final int port;
    private final CivicIssueService issueService;
    private final String webRoot;
    private HttpServer server;
    private final ScheduledExecutorService slaWorker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "civic-sla-escalation"); t.setDaemon(true); return t;
    });

    public CivicHttpServer(int port, CivicIssueService issueService, String webRoot) {
        this.port = port;
        this.issueService = issueService;
        this.webRoot = webRoot;
    }

    public void start() throws IOException {
        int actualPort = port;
        try {
            server = HttpServer.create(new InetSocketAddress(actualPort), 0);
        } catch (IOException e) {
            actualPort = port + 1;
            System.out.println("[CivicHttpServer] Port " + port + " busy, falling back to port " + actualPort);
            server = HttpServer.create(new InetSocketAddress(actualPort), 0);
        }

        // Register handlers
        Path uploadDir = Path.of("uploads");
        server.createContext("/api/", new RestApiHandler(issueService, uploadDir));
        server.createContext("/api", new RestApiHandler(issueService, uploadDir));
        server.createContext("/uploads/", new UploadFileHandler(uploadDir));
        server.createContext("/", new StaticFileHandler(webRoot));

        // Multi-threaded executor
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.start();
        slaWorker.scheduleAtFixedRate(() -> {
            try {
                issueService.refreshDynamicSeverity();
                int count = issueService.escalateOverdueIssues();
                if (count > 0) System.out.println("[SLA] Automatically escalated " + count + " overdue issue(s).");
            } catch (RuntimeException e) { System.err.println("[SLA] Escalation scan failed: " + e.getMessage()); }
        }, 10, 10, TimeUnit.MINUTES);

        System.out.println("=======================================================================");
        System.out.println("   SMART CIVIC RESPONSE SYSTEM - PURE JAVA 22 BACKEND INITIALIZED     ");
        System.out.println("=======================================================================");
        System.out.println("   Local Web Portal:  http://localhost:" + actualPort);
        System.out.println("   REST API Base:     http://localhost:" + actualPort + "/api/issues");
        System.out.println("   API Health Check:  http://localhost:" + actualPort + "/api/health");
        System.out.println("   Analytics Stats:   http://localhost:" + actualPort + "/api/stats");
        System.out.println("   Department List:   http://localhost:" + actualPort + "/api/departments");
        System.out.println("=======================================================================");
    }

    public void stop() {
        slaWorker.shutdownNow();
        if (server != null) {
            server.stop(1);
            System.out.println("[CivicHttpServer] Server stopped.");
        }
    }
}
