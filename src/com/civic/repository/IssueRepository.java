package com.civic.repository;

import com.civic.model.*;
import com.civic.util.SimpleJson;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class IssueRepository {

    private final Map<String, CivicIssue> issues = new ConcurrentHashMap<>();
    private final Path storagePath;
    private final AtomicInteger idCounter = new AtomicInteger(1040);

    public IssueRepository() {
        this("data/civic_issues.json");
    }

    public IssueRepository(String filePath) {
        this.storagePath = Paths.get(filePath);
        loadOrInitialize();
    }

    public synchronized List<CivicIssue> getAll() {
        List<CivicIssue> list = new ArrayList<>(issues.values());
        // Sort newest first
        list.sort((a, b) -> {
            String timeA = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String timeB = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return timeB.compareTo(timeA);
        });
        return list;
    }

    public synchronized CivicIssue getById(String id) {
        if (id == null) return null;
        return issues.get(id.toUpperCase().trim());
    }

    public synchronized CivicIssue save(CivicIssue issue) {
        if (issue.getId() == null || issue.getId().trim().isEmpty()) {
            issue.setId("CIV-2026-" + idCounter.incrementAndGet());
        }
        if (issue.getCreatedAt() == null) {
            issue.setCreatedAt(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        }
        issue.setUpdatedAt(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        issues.put(issue.getId(), issue);
        persist();
        return issue;
    }

    public synchronized CivicIssue update(CivicIssue issue) {
        if (issue.getId() == null || !issues.containsKey(issue.getId())) {
            return null;
        }
        issue.setUpdatedAt(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        issues.put(issue.getId(), issue);
        persist();
        return issue;
    }

    public synchronized boolean delete(String id) {
        if (id != null && issues.remove(id.toUpperCase().trim()) != null) {
            persist();
            return true;
        }
        return false;
    }

    public synchronized String generateNextId() {
        return "CIV-2026-" + idCounter.incrementAndGet();
    }

    private void loadOrInitialize() {
        File file = storagePath.toFile();
        if (file.exists()) {
            try {
                String content = Files.readString(storagePath, StandardCharsets.UTF_8);
                List<Object> rawList = SimpleJson.parseArray(content);
                if (rawList != null && !rawList.isEmpty()) {
                    for (Object item : rawList) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            CivicIssue issue = parseIssueFromMap((Map<String, Object>) item);
                            if (issue != null && issue.getId() != null) {
                                issues.put(issue.getId(), issue);
                                try {
                                    String idNum = issue.getId().replaceAll("\\D+", "");
                                    if (!idNum.isEmpty()) {
                                        int num = Integer.parseInt(idNum);
                                        if (num > idCounter.get()) {
                                            idCounter.set(num);
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    System.out.println("[IssueRepository] Loaded " + issues.size() + " civic issues from disk.");
                    return;
                }
            } catch (Exception e) {
                System.err.println("[IssueRepository] Error loading storage file: " + e.getMessage());
            }
        }

        // Initialize with realistic seed data
        initSeedData();
        persist();
    }

    private void persist() {
        try {
            if (storagePath.getParent() != null && !Files.exists(storagePath.getParent())) {
                Files.createDirectories(storagePath.getParent());
            }
            List<CivicIssue> all = getAll();
            String json = SimpleJson.toJson(all);
            Files.writeString(storagePath, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[IssueRepository] Failed to persist data: " + e.getMessage());
        }
    }

    private void initSeedData() {
        System.out.println("[IssueRepository] Seeding initial municipal civic issues...");

        Instant now = Instant.now();

        // 1. Critical Water Pipe Burst
        CivicIssue issue1 = new CivicIssue();
        issue1.setId("CIV-2026-1001");
        issue1.setTitle("Major Underground Water Main Burst on 5th Avenue");
        issue1.setDescription("Underground potable water transmission pipe has ruptured causing severe waterlogging, street cave-in risk, and loss of drinking water supply to entire block 4.");
        issue1.setCategory("WATER_DRAINAGE");
        issue1.setPriority(PriorityLevel.CRITICAL);
        issue1.setStatus(IssueStatus.IN_PROGRESS);
        issue1.setDepartment("Water Supply & Sewerage Board");
        issue1.setAssignedOfficer("Eng. Ramesh Narang (Rapid Response Unit 2)");
        issue1.setAddress("5th Avenue, Near Apollo Central Clinic, Ward 12");
        issue1.setWard("Ward 12 - Midtown Sector");
        issue1.setLatitude(28.6139);
        issue1.setLongitude(77.2090);
        issue1.setReporterName("Sunil Chopra");
        issue1.setReporterPhone("+91 98112 45890");
        issue1.setReporterEmail("sunil.chopra@example.com");
        issue1.setPhotoUrl("https://images.unsplash.com/photo-1541888946425-d0fbb18086f6?auto=format&fit=crop&w=800&q=80");
        issue1.setUpvotes(48);
        issue1.setCreatedAt(DateTimeFormatter.ISO_INSTANT.format(now.minus(4, ChronoUnit.HOURS)));
        issue1.setSlaHours(12);
        issue1.addTimelineEvent("REPORTED", "Citizen Sunil Chopra", "Complaint Submitted", "Reported heavy water gushing from under the asphalt.");
        issue1.addTimelineEvent("TRIAGED", "Smart Civic AI Dispatch", "Priority Triaged to CRITICAL", "Assigned SLA of 12 hours due to drinking water line damage.");
        issue1.addTimelineEvent("IN_PROGRESS", "Water Supply Board", "Excavation Team Dispatched", "Isolation valves shut off. Replacement steel sleeve underway.");
        issues.put(issue1.getId(), issue1);

        // 2. Deep Crater / Pothole
        CivicIssue issue2 = new CivicIssue();
        issue2.setId("CIV-2026-1002");
        issue2.setTitle("Dangerous 3-Foot Pothole Near High School Crossing");
        issue2.setDescription("Large sinkhole/pothole right outside St. Jude School main pedestrian gate. Multiple two-wheelers have skidded in rainy conditions.");
        issue2.setCategory("ROADS");
        issue2.setPriority(PriorityLevel.HIGH);
        issue2.setStatus(IssueStatus.TRIAGED);
        issue2.setDepartment("Roads & Highway Infrastructure");
        issue2.setAssignedOfficer("Officer Vikram Rathore");
        issue2.setAddress("Opposite St. Jude High School, Ring Road North, Ward 7");
        issue2.setWard("Ward 7 - North Enclave");
        issue2.setLatitude(28.6250);
        issue2.setLongitude(77.2180);
        issue2.setReporterName("Pooja Singhal (School PTA)");
        issue2.setReporterPhone("+91 98730 11223");
        issue2.setReporterEmail("pta.singhal@example.com");
        issue2.setPhotoUrl("https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=800&q=80");
        issue2.setUpvotes(31);
        issue2.setCreatedAt(DateTimeFormatter.ISO_INSTANT.format(now.minus(14, ChronoUnit.HOURS)));
        issue2.setSlaHours(24);
        issue2.addTimelineEvent("REPORTED", "Citizen Pooja Singhal", "Complaint Submitted", "Immediate barricading requested before morning school bell.");
        issue2.addTimelineEvent("TRIAGED", "Public Works Dept", "Inspection Assigned", "Nodal engineer assigned; cold mix asphalt patching vehicle queued.");
        issues.put(issue2.getId(), issue2);

        // 3. Overflowing Community Garbage Dump
        CivicIssue issue3 = new CivicIssue();
        issue3.setId("CIV-2026-1003");
        issue3.setTitle("Commercial Garbage Bin Overflowing for 4 Days");
        issue3.setDescription("Municipal compactor bin overflowing across sidewalk with hazardous decomposition smell and stray animal interference.");
        issue3.setCategory("SANITATION");
        issue3.setPriority(PriorityLevel.MEDIUM);
        issue3.setStatus(IssueStatus.RESOLVED);
        issue3.setDepartment("Solid Waste Management & Sanitation");
        issue3.setAssignedOfficer("Supervisor Deepak Rawat");
        issue3.setAddress("Market Lane, Sector 15 B-Block, Ward 15");
        issue3.setWard("Ward 15 - West Market");
        issue3.setLatitude(28.6080);
        issue3.setLongitude(77.1950);
        issue3.setReporterName("Kavita Merchant");
        issue3.setReporterPhone("+91 99105 88231");
        issue3.setReporterEmail("kavita.m@example.com");
        issue3.setPhotoUrl("https://images.unsplash.com/photo-1605600659908-0ef719419d41?auto=format&fit=crop&w=800&q=80");
        issue3.setUpvotes(19);
        issue3.setCreatedAt(DateTimeFormatter.ISO_INSTANT.format(now.minus(32, ChronoUnit.HOURS)));
        issue3.setResolvedAt(DateTimeFormatter.ISO_INSTANT.format(now.minus(6, ChronoUnit.HOURS)));
        issue3.setSlaHours(48);
        issue3.setResolutionNotes("Sanitation tipper truck collected 3.2 tons of waste. Bin sanitization and lime dusting completed.");
        issue3.addTimelineEvent("REPORTED", "Citizen Kavita Merchant", "Complaint Submitted", "Trash spilled onto street curb.");
        issue3.addTimelineEvent("IN_PROGRESS", "Sanitation Dept", "Sanitation Tipper Dispatched", "Route 4 compactor truck on site.");
        issue3.addTimelineEvent("RESOLVED", "Supervisor Deepak Rawat", "Waste Evacuated & Area Disinfected", "Site photographed and marked clear.");
        issues.put(issue3.getId(), issue3);

        // 4. Sparking Overhead Transformer
        CivicIssue issue4 = new CivicIssue();
        issue4.setId("CIV-2026-1004");
        issue4.setTitle("Sparking Electrical Transformer Pole with Hanging Wire");
        issue4.setDescription("Low hanging insulated line broke loose and is sparking intermittently near residential balcony during wind gusts.");
        issue4.setCategory("ELECTRICITY");
        issue4.setPriority(PriorityLevel.CRITICAL);
        issue4.setStatus(IssueStatus.IN_PROGRESS);
        issue4.setDepartment("Public Lighting & Power Grid");
        issue4.setAssignedOfficer("Lineman Hitesh Joshi (Grid Squad A)");
        issue4.setAddress("Lane 8, Krishna Colony, Ward 4");
        issue4.setWard("Ward 4 - East Colony");
        issue4.setLatitude(28.6300);
        issue4.setLongitude(77.2350);
        issue4.setReporterName("Arun Saxena");
        issue4.setReporterPhone("+91 98101 22944");
        issue4.setReporterEmail("arun.saxena@example.com");
        issue4.setPhotoUrl("https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?auto=format&fit=crop&w=800&q=80");
        issue4.setUpvotes(54);
        issue4.setCreatedAt(DateTimeFormatter.ISO_INSTANT.format(now.minus(2, ChronoUnit.HOURS)));
        issue4.setSlaHours(12);
        issue4.addTimelineEvent("REPORTED", "Citizen Arun Saxena", "Emergency SOS Reported", "High risk of fire and electrocution.");
        issue4.addTimelineEvent("IN_PROGRESS", "Grid Power Squad", "Emergency Feeder Tripped", "Feeder 2 isolated. Bucket truck replacing jumper connection.");
        issues.put(issue4.getId(), issue4);

        // 5. Open Uncovered Manhole on Promenade
        CivicIssue issue5 = new CivicIssue();
        issue5.setId("CIV-2026-1005");
        issue5.setTitle("Uncovered Sewer Manhole on Evening Jogging Track");
        issue5.setDescription("Cast iron manhole lid was displaced or stolen. Heavy foot traffic and elderly walkers at risk of falling in.");
        issue5.setCategory("PUBLIC_SAFETY");
        issue5.setPriority(PriorityLevel.HIGH);
        issue5.setStatus(IssueStatus.REPORTED);
        issue5.setDepartment("Public Safety & Disaster Relief");
        issue5.setAddress("Green Park Promenade, Ward 9");
        issue5.setWard("Ward 9 - South Green");
        issue5.setLatitude(28.5950);
        issue5.setLongitude(77.2050);
        issue5.setReporterName("Dr. Alok Sen");
        issue5.setReporterPhone("+91 98230 45678");
        issue5.setReporterEmail("alok.sen@example.com");
        issue5.setPhotoUrl("https://images.unsplash.com/photo-1574068468668-a05a11f871da?auto=format&fit=crop&w=800&q=80");
        issue5.setUpvotes(22);
        issue5.setCreatedAt(DateTimeFormatter.ISO_INSTANT.format(now.minus(1, ChronoUnit.HOURS)));
        issue5.setSlaHours(24);
        issue5.addTimelineEvent("REPORTED", "Citizen Dr. Alok Sen", "Complaint Submitted", "Immediate warning cone installed by citizens.");
        issues.put(issue5.getId(), issue5);

        // 6. Broken Children Swings in Municipal Park
        CivicIssue issue6 = new CivicIssue();
        issue6.setId("CIV-2026-1006");
        issue6.setTitle("Broken Steel Chain on Children Swings");
        issue6.setDescription("Two swing chains have rusted and snapped. Sharp edges exposed in children play area.");
        issue6.setCategory("PARKS");
        issue6.setPriority(PriorityLevel.LOW);
        issue6.setStatus(IssueStatus.REPORTED);
        issue6.setDepartment("Urban Parks & Green Environment");
        issue6.setAddress("Children Park, Block C, Ward 11");
        issue6.setWard("Ward 11 - Garden Town");
        issue6.setLatitude(28.6010);
        issue6.setLongitude(77.2200);
        issue6.setReporterName("Rohit Bansal");
        issue6.setReporterPhone("+91 98765 43210");
        issue6.setReporterEmail("rohit.bansal@example.com");
        issue6.setPhotoUrl("https://images.unsplash.com/photo-1519331379826-f10be5486c6f?auto=format&fit=crop&w=800&q=80");
        issue6.setUpvotes(8);
        issue6.setCreatedAt(DateTimeFormatter.ISO_INSTANT.format(now.minus(18, ChronoUnit.HOURS)));
        issue6.setSlaHours(72);
        issue6.addTimelineEvent("REPORTED", "Citizen Rohit Bansal", "Complaint Submitted", "Routine playground maintenance requested.");
        issues.put(issue6.getId(), issue6);
    }

    private CivicIssue parseIssueFromMap(Map<String, Object> map) {
        CivicIssue issue = new CivicIssue();
        issue.setId(getString(map, "id"));
        issue.setTitle(getString(map, "title"));
        issue.setDescription(getString(map, "description"));
        issue.setCategory(getString(map, "category"));
        issue.setPriority(PriorityLevel.fromString(getString(map, "priority")));
        issue.setStatus(IssueStatus.fromString(getString(map, "status")));
        issue.setDepartment(getString(map, "department"));
        issue.setAssignedOfficer(getString(map, "assignedOfficer"));
        issue.setAddress(getString(map, "address"));
        issue.setWard(getString(map, "ward"));
        issue.setLatitude(getDouble(map, "latitude"));
        issue.setLongitude(getDouble(map, "longitude"));
        issue.setReporterName(getString(map, "reporterName"));
        issue.setReporterPhone(getString(map, "reporterPhone"));
        issue.setReporterEmail(getString(map, "reporterEmail"));
        issue.setPhotoUrl(getString(map, "photoUrl"));
        issue.setResolutionPhotoUrl(getString(map, "resolutionPhotoUrl"));
        issue.setUpvotes(getInt(map, "upvotes"));
        issue.setCreatedAt(getString(map, "createdAt"));
        issue.setUpdatedAt(getString(map, "updatedAt"));
        issue.setResolvedAt(getString(map, "resolvedAt"));
        issue.setSlaHours(getInt(map, "slaHours", 48));
        issue.setResolutionNotes(getString(map, "resolutionNotes"));

        Object rawTimeline = map.get("timeline");
        if (rawTimeline instanceof List) {
            List<TimelineEvent> timeline = new ArrayList<>();
            for (Object evtObj : (List<?>) rawTimeline) {
                if (evtObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> evtMap = (Map<String, Object>) evtObj;
                    TimelineEvent evt = new TimelineEvent(
                        getString(evtMap, "timestamp"),
                        getString(evtMap, "status"),
                        getString(evtMap, "actor"),
                        getString(evtMap, "action"),
                        getString(evtMap, "note")
                    );
                    timeline.add(evt);
                }
            }
            issue.setTimeline(timeline);
        }

        return issue;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key) {
        return getInt(map, key, 0);
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (Exception ignored) {}
        }
        return def;
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val != null) {
            try { return Double.parseDouble(val.toString()); } catch (Exception ignored) {}
        }
        return 0.0;
    }
}
