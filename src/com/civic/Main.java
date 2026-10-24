package com.civic;

import com.civic.repository.IssueRepository;
import com.civic.server.CivicHttpServer;
import com.civic.service.CivicIssueService;
import com.civic.service.SmartTriageService;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        // Ensure data directory exists
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        String webRoot = "web";
        File webDir = new File(webRoot);
        if (!webDir.exists()) {
            System.err.println("[Warning] Web directory '" + webRoot + "' not found. Creating it.");
            webDir.mkdirs();
        }

        IssueRepository repository = new IssueRepository("data/civic_issues.json");
        SmartTriageService triageService = new SmartTriageService();
        CivicIssueService issueService = new CivicIssueService(repository, triageService);

        CivicHttpServer server = new CivicHttpServer(port, issueService, webRoot);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown] Shutting down Smart Civic Response Server...");
            server.stop();
        }));

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("[Error] Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
