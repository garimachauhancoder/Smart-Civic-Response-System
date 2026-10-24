package com.civic.service;

import com.civic.model.*;
import com.civic.repository.IssueRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CivicIssueService {

    private final IssueRepository repository;
    private final SmartTriageService triageService;
    private final List<Department> departments;

    public CivicIssueService(IssueRepository repository, SmartTriageService triageService) {
        this.repository = repository;
        this.triageService = triageService;
        this.departments = Department.getDefaultDepartments();
    }

    public List<CivicIssue> getAllIssues(String categoryFilter, String statusFilter, String priorityFilter, String query) {
        List<CivicIssue> list = repository.getAll();
        if (categoryFilter == null && statusFilter == null && priorityFilter == null && (query == null || query.trim().isEmpty())) {
            return list;
        }

        List<CivicIssue> filtered = new ArrayList<>();
        String q = query != null ? query.trim().toLowerCase() : "";

        for (CivicIssue issue : list) {
            if (categoryFilter != null && !categoryFilter.equalsIgnoreCase("ALL") && !categoryFilter.equalsIgnoreCase(issue.getCategory())) {
                continue;
            }
            if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL") &&
                (issue.getStatus() == null || !statusFilter.equalsIgnoreCase(issue.getStatus().name()))) {
                continue;
            }
            if (priorityFilter != null && !priorityFilter.equalsIgnoreCase("ALL") &&
                (issue.getPriority() == null || !priorityFilter.equalsIgnoreCase(issue.getPriority().name()))) {
                continue;
            }
            if (!q.isEmpty()) {
                String fullSearchable = (issue.getId() + " " + issue.getTitle() + " " + issue.getDescription() + " " +
                        issue.getAddress() + " " + issue.getWard() + " " + issue.getDepartment()).toLowerCase();
                if (!fullSearchable.contains(q)) {
                    continue;
                }
            }
            filtered.add(issue);
        }
        return filtered;
    }

    public CivicIssue getIssueById(String id) {
        return repository.getById(id);
    }

    public CivicIssue createIssue(Map<String, Object> payload) {
        String title = (String) payload.get("title");
        String description = (String) payload.get("description");
        String category = (String) payload.get("category");
        String priorityStr = (String) payload.get("priority");

        // Run smart AI triage if category or priority is missing or "AUTO"
        SmartTriageService.TriageResult triage = triageService.analyze(title, description);

        if (category == null || category.equalsIgnoreCase("AUTO") || category.trim().isEmpty()) {
            category = triage.category;
        }

        PriorityLevel priority;
        if (priorityStr != null && !priorityStr.equalsIgnoreCase("AUTO") && !priorityStr.trim().isEmpty()) {
            priority = PriorityLevel.fromString(priorityStr);
        } else {
            priority = triage.priority;
        }

        String department = (String) payload.get("department");
        if (department == null || department.trim().isEmpty()) {
            department = triage.department;
        }

        CivicIssue issue = new CivicIssue();
        issue.setTitle(title != null ? title.trim() : "Civic Incident");
        issue.setDescription(description != null ? description.trim() : "");
        issue.setCategory(category);
        issue.setPriority(priority);
        issue.setDepartment(department);
        issue.setSlaHours(priority.getSlaHours());

        issue.setAddress((String) payload.get("address"));
        issue.setWard((String) payload.get("ward"));

        if (payload.get("latitude") instanceof Number) {
            issue.setLatitude(((Number) payload.get("latitude")).doubleValue());
        }
        if (payload.get("longitude") instanceof Number) {
            issue.setLongitude(((Number) payload.get("longitude")).doubleValue());
        }

        issue.setReporterName((String) payload.get("reporterName"));
        issue.setReporterPhone((String) payload.get("reporterPhone"));
        issue.setReporterEmail((String) payload.get("reporterEmail"));
        issue.setPhotoUrl((String) payload.get("photoUrl"));

        // Add timeline events
        String reporter = issue.getReporterName() != null && !issue.getReporterName().isEmpty()
                ? "Citizen " + issue.getReporterName() : "Anonymous Citizen";
        issue.addTimelineEvent("REPORTED", reporter, "Civic Issue Reported", "Ticket created and registered in municipal registry.");
        issue.addTimelineEvent("TRIAGED", "Smart Civic AI Dispatch", "Automated Triage Completed", triage.rationale);

        return repository.save(issue);
    }

    public CivicIssue updateStatus(String issueId, String newStatusStr, String actor, String notes, String officer, String resolutionPhotoUrl) {
        CivicIssue issue = repository.getById(issueId);
        if (issue == null) return null;

        IssueStatus newStatus = IssueStatus.fromString(newStatusStr);
        issue.setStatus(newStatus);

        if (officer != null && !officer.trim().isEmpty()) {
            issue.setAssignedOfficer(officer.trim());
        }

        if (notes != null && !notes.trim().isEmpty()) {
            issue.setResolutionNotes(notes.trim());
        }

        if (resolutionPhotoUrl != null && !resolutionPhotoUrl.trim().isEmpty()) {
            issue.setResolutionPhotoUrl(resolutionPhotoUrl.trim());
        }

        if (newStatus == IssueStatus.RESOLVED) {
            issue.setResolvedAt(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        }

        String actualActor = (actor != null && !actor.trim().isEmpty()) ? actor : "Municipal Officer";
        String actionName = "Status updated to " + newStatus.getLabel();
        String eventNote = (notes != null && !notes.trim().isEmpty()) ? notes : "Workflow state transition logged.";

        issue.addTimelineEvent(newStatus.name(), actualActor, actionName, eventNote);

        return repository.update(issue);
    }

    public CivicIssue assignDepartment(String issueId, String department, String actor, String notes) {
        CivicIssue issue = repository.getById(issueId);
        if (issue == null) return null;

        issue.setDepartment(department);
        issue.addTimelineEvent(issue.getStatus().name(),
                actor != null ? actor : "Central Dispatch",
                "Reassigned to " + department,
                notes != null ? notes : "Department routing updated by municipal administrator.");

        return repository.update(issue);
    }

    public CivicIssue upvote(String issueId) {
        CivicIssue issue = repository.getById(issueId);
        if (issue == null) return null;

        int newVotes = issue.incrementUpvote();

        // Community escalation threshold
        if (newVotes >= 25 && issue.getPriority() == PriorityLevel.MEDIUM) {
            issue.setPriority(PriorityLevel.HIGH);
            issue.setSlaHours(PriorityLevel.HIGH.getSlaHours());
            issue.addTimelineEvent(issue.getStatus().name(), "Community Escalation Engine",
                    "Elevated to HIGH Priority", "Community urgency threshold crossed (" + newVotes + " citizens confirmed this issue).");
        } else if (newVotes >= 50 && (issue.getPriority() == PriorityLevel.HIGH || issue.getPriority() == PriorityLevel.MEDIUM)) {
            issue.setPriority(PriorityLevel.CRITICAL);
            issue.setSlaHours(PriorityLevel.CRITICAL.getSlaHours());
            issue.addTimelineEvent(issue.getStatus().name(), "Community Escalation Engine",
                    "Elevated to CRITICAL Priority", "Viral community impact triggered emergency protocol (" + newVotes + " verified citizen endorsements).");
        }

        return repository.update(issue);
    }

    public Map<String, Object> getStatistics() {
        List<CivicIssue> all = repository.getAll();

        int total = all.size();
        int reported = 0;
        int triaged = 0;
        int inProgress = 0;
        int resolved = 0;
        int closed = 0;
        int slaBreached = 0;
        int criticalCount = 0;

        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        Map<String, Integer> wardCounts = new LinkedHashMap<>();

        long totalResolvedHours = 0;
        int resolvedWithDurationCount = 0;

        for (CivicIssue issue : all) {
            IssueStatus s = issue.getStatus();
            if (s == IssueStatus.REPORTED) reported++;
            else if (s == IssueStatus.TRIAGED) triaged++;
            else if (s == IssueStatus.IN_PROGRESS) inProgress++;
            else if (s == IssueStatus.RESOLVED) resolved++;
            else if (s == IssueStatus.CLOSED) closed++;

            if (issue.getPriority() == PriorityLevel.CRITICAL) {
                criticalCount++;
            }

            if (issue.isSlaBreached()) {
                slaBreached++;
            }

            // Category counts
            String cat = issue.getCategory() != null ? issue.getCategory() : "OTHER";
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);

            // Ward counts
            String ward = issue.getWard() != null ? issue.getWard() : "General";
            wardCounts.put(ward, wardCounts.getOrDefault(ward, 0) + 1);

            // Calculate resolution time
            if (issue.getResolvedAt() != null && issue.getCreatedAt() != null) {
                try {
                    Instant c = Instant.parse(issue.getCreatedAt());
                    Instant r = Instant.parse(issue.getResolvedAt());
                    long hours = Duration.between(c, r).toHours();
                    totalResolvedHours += Math.max(1, hours);
                    resolvedWithDurationCount++;
                } catch (Exception ignored) {}
            }
        }

        double avgResolutionHours = resolvedWithDurationCount > 0 ? (double) totalResolvedHours / resolvedWithDurationCount : 22.5;
        double resolutionRate = total > 0 ? ((double) (resolved + closed) / total) * 100.0 : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalIssues", total);
        stats.put("activePending", reported + triaged + inProgress);
        stats.put("reportedCount", reported);
        stats.put("triagedCount", triaged);
        stats.put("inProgressCount", inProgress);
        stats.put("resolvedCount", resolved);
        stats.put("closedCount", closed);
        stats.put("criticalCount", criticalCount);
        stats.put("slaBreachedCount", slaBreached);
        stats.put("resolutionRatePercent", Math.round(resolutionRate * 10.0) / 10.0);
        stats.put("avgResolutionHours", Math.round(avgResolutionHours * 10.0) / 10.0);
        stats.put("categoryBreakdown", categoryCounts);
        stats.put("wardBreakdown", wardCounts);

        return stats;
    }

    public List<Map<String, Object>> getDepartmentsWithWorkload() {
        List<CivicIssue> all = repository.getAll();
        Map<String, Integer> deptPending = new HashMap<>();
        Map<String, Integer> deptResolved = new HashMap<>();

        for (CivicIssue issue : all) {
            String d = issue.getDepartment();
            if (d == null) continue;
            if (issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED) {
                deptResolved.put(d, deptResolved.getOrDefault(d, 0) + 1);
            } else {
                deptPending.put(d, deptPending.getOrDefault(d, 0) + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Department dept : departments) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", dept.getId());
            map.put("name", dept.getName());
            map.put("code", dept.getCode());
            map.put("categoryKey", dept.getCategoryKey());
            map.put("icon", dept.getIcon());
            map.put("headOfficer", dept.getHeadOfficer());
            map.put("contactEmail", dept.getContactEmail());
            map.put("phone", dept.getPhone());
            map.put("activeOfficers", dept.getActiveOfficers());
            map.put("pendingIssues", deptPending.getOrDefault(dept.getName(), 0));
            map.put("resolvedIssues", deptResolved.getOrDefault(dept.getName(), 0));
            result.add(map);
        }
        return result;
    }

    public SmartTriageService.TriageResult testTriage(String title, String description) {
        return triageService.analyze(title, description);
    }
}
