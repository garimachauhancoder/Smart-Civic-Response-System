package com.civic.service;

import com.civic.model.*;
import com.civic.repository.IssueRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CivicIssueService {

    public static class IssueCreationResult {
        public final CivicIssue issue;
        public final boolean duplicate;
        IssueCreationResult(CivicIssue issue, boolean duplicate) { this.issue = issue; this.duplicate = duplicate; }
    }

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
        return createIssueWithResult(payload).issue;
    }

    public synchronized IssueCreationResult createIssueWithResult(Map<String, Object> payload) {
        String title = (String) payload.get("title");
        String description = (String) payload.get("description");
        String category = (String) payload.get("category");
        String priorityStr = (String) payload.get("priority");

        // Run smart AI triage if category or priority is missing or "AUTO"
        SmartTriageService.TriageResult triage = triageService.analyze(title, description);

        if (category == null || category.equalsIgnoreCase("AUTO") || category.trim().isEmpty()) {
            category = triage.category;
        }

        double latitude = number(payload.get("latitude"));
        double longitude = number(payload.get("longitude"));
        if (validCoordinate(latitude, longitude)) {
            for (CivicIssue candidate : repository.getAll()) {
                if (!isActive(candidate.getStatus()) || candidate.getLatitude() == 0 && candidate.getLongitude() == 0) continue;
                if (category.equalsIgnoreCase(candidate.getCategory()) &&
                        distanceMeters(latitude, longitude, candidate.getLatitude(), candidate.getLongitude()) <= 50) {
                    CivicIssue upvoted = upvote(candidate.getId());
                    return new IssueCreationResult(upvoted, true);
                }
            }
        }

        PriorityLevel priority;
        if (priorityStr != null && !priorityStr.equalsIgnoreCase("AUTO") && !priorityStr.trim().isEmpty()) {
            priority = PriorityLevel.fromString(priorityStr);
        } else {
            priority = triage.priority;
        }
        PriorityLevel calculatedPriority = triageService.priorityForScore(
                triageService.dynamicSeverityScore(category, 0, 0));
        if (severityRank(calculatedPriority) > severityRank(priority)) priority = calculatedPriority;

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
        issue.setFraudFlag(Boolean.TRUE.equals(payload.get("fraudFlag")));
        issue.setFraudReason((String) payload.get("fraudReason"));

        // Add timeline events
        String reporter = issue.getReporterName() != null && !issue.getReporterName().isEmpty()
                ? "Citizen " + issue.getReporterName() : "Anonymous Citizen";
        issue.addTimelineEvent("REPORTED", reporter, "Civic Issue Reported", "Ticket created and registered in municipal registry.");
        issue.addTimelineEvent("TRIAGED", "Smart Civic AI Dispatch", "Automated Triage Completed", triage.rationale);

        return new IssueCreationResult(repository.save(issue), false);
    }

    public List<CivicIssue> getIssuesForWard(String ward) {
        List<CivicIssue> result = new ArrayList<>();
        for (CivicIssue issue : repository.getAll()) {
            if (ward == null || ward.isBlank() || "ALL".equalsIgnoreCase(ward) || ward.equalsIgnoreCase(issue.getWard())) {
                if (isActive(issue.getStatus())) result.add(issue);
            }
        }
        return result;
    }

    public List<Map<String, Object>> getWardReport() {
        Map<String, int[]> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (CivicIssue issue : repository.getAll()) {
            String ward = issue.getWard() == null || issue.getWard().isBlank() ? "Unassigned" : issue.getWard();
            int[] count = counts.computeIfAbsent(ward, ignored -> new int[2]);
            if (issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED) count[0]++;
            else count[1]++;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        counts.forEach((ward, count) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ward", ward); row.put("totalResolved", count[0]); row.put("pendingCount", count[1]);
            rows.add(row);
        });
        return rows;
    }

    public synchronized int refreshDynamicSeverity() {
        int changed = 0;
        Instant now = Instant.now();
        for (CivicIssue issue : repository.getAll()) {
            if (!isActive(issue.getStatus())) continue;
            try {
                double age = Duration.between(Instant.parse(issue.getCreatedAt()), now).toMinutes() / 60.0;
                double score = triageService.dynamicSeverityScore(issue.getCategory(), issue.getUpvotes(), age);
                PriorityLevel calculated = triageService.priorityForScore(score);
                if (severityRank(calculated) > severityRank(issue.getPriority())) {
                    issue.setPriority(calculated); issue.setSlaHours(calculated.getSlaHours());
                    issue.addTimelineEvent(issue.getStatus().name(), "Dynamic Severity Engine",
                            "Priority increased to " + calculated.name(), String.format("Severity score %.1f (category + endorsements + age).", score));
                    repository.update(issue); changed++;
                }
            } catch (Exception ignored) { }
        }
        return changed;
    }

    private boolean isActive(IssueStatus status) {
        return status != null && status != IssueStatus.RESOLVED && status != IssueStatus.CLOSED;
    }
    private int severityRank(PriorityLevel p) {
        if (p == PriorityLevel.CRITICAL) return 4;
        if (p == PriorityLevel.HIGH) return 3;
        if (p == PriorityLevel.MEDIUM) return 2;
        return 1;
    }
    private double number(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return value == null ? 0 : Double.parseDouble(value.toString()); } catch (Exception e) { return 0; }
    }
    private boolean validCoordinate(double lat, double lon) { return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180 && (lat != 0 || lon != 0); }
    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double p1=Math.toRadians(lat1), p2=Math.toRadians(lat2), dp=Math.toRadians(lat2-lat1), dl=Math.toRadians(lon2-lon1);
        double a=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);
        return 6371000*2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    public synchronized int escalateOverdueIssues() {
        int escalated = 0;
        Instant now = Instant.now();
        for (CivicIssue issue : repository.getAll()) {
            IssueStatus status = issue.getStatus();
            if (status != IssueStatus.PENDING && status != IssueStatus.REPORTED &&
                    status != IssueStatus.TRIAGED && status != IssueStatus.IN_PROGRESS) continue;
            try {
                Instant created = Instant.parse(issue.getCreatedAt());
                int thresholdHours = issue.getSlaHours() > 0 ? issue.getSlaHours() : 48;
                if (Duration.between(created, now).toHours() < thresholdHours) continue;
                issue.setStatus(IssueStatus.ESCALATED);
                issue.addTimelineEvent(IssueStatus.ESCALATED.name(), "SLA Escalation Worker",
                        "Automatically escalated after SLA breach", "Issue exceeded its " + thresholdHours + " hour SLA.");
                repository.update(issue);
                escalated++;
            } catch (Exception ignored) { }
        }
        return escalated;
    }

    public synchronized CivicIssue updateStatus(String issueId, String newStatusStr, String actor, String notes, String officer, String resolutionPhotoUrl) {
        CivicIssue issue = repository.getById(issueId);
        if (issue == null) return null;

        IssueStatus newStatus = IssueStatus.fromString(newStatusStr);
        if ((newStatus == IssueStatus.RESOLVED || newStatus == IssueStatus.CLOSED) &&
                (resolutionPhotoUrl == null || resolutionPhotoUrl.isBlank()) &&
                (issue.getResolutionPhotoUrl() == null || issue.getResolutionPhotoUrl().isBlank())) {
            throw new IllegalArgumentException("An after-resolution photo is required before resolving this issue.");
        }
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

    public synchronized CivicIssue assignDepartment(String issueId, String department, String actor, String notes) {
        CivicIssue issue = repository.getById(issueId);
        if (issue == null) return null;

        issue.setDepartment(department);
        issue.addTimelineEvent(issue.getStatus().name(),
                actor != null ? actor : "Central Dispatch",
                "Reassigned to " + department,
                notes != null ? notes : "Department routing updated by municipal administrator.");

        return repository.update(issue);
    }

    public synchronized CivicIssue upvote(String issueId) {
        CivicIssue issue = repository.getById(issueId);
        if (issue == null) return null;

        int newVotes = issue.incrementUpvote();

        double ageHours = 0;
        try { ageHours = Duration.between(Instant.parse(issue.getCreatedAt()), Instant.now()).toMinutes() / 60.0; } catch (Exception ignored) { }
        PriorityLevel dynamic = triageService.priorityForScore(
                triageService.dynamicSeverityScore(issue.getCategory(), newVotes, ageHours));
        if (severityRank(dynamic) > severityRank(issue.getPriority())) {
            issue.setPriority(dynamic);
            issue.setSlaHours(dynamic.getSlaHours());
            issue.addTimelineEvent(issue.getStatus().name(), "Dynamic Severity Engine",
                    "Priority increased to " + dynamic.name(), "Severity score increased with community endorsements and issue age.");
        }

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
