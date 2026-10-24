package com.civic.service;

import com.civic.model.PriorityLevel;

import java.util.*;

public class SmartTriageService {

    public static class TriageResult {
        public String category;
        public String department;
        public PriorityLevel priority;
        public int slaHours;
        public int urgencyScore; // 0 - 100
        public List<String> detectedKeywords = new ArrayList<>();
        public String rationale;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("category", category);
            map.put("department", department);
            map.put("priority", priority.name());
            map.put("priorityLabel", priority.getDisplayName());
            map.put("priorityColor", priority.getColorCode());
            map.put("slaHours", slaHours);
            map.put("urgencyScore", urgencyScore);
            map.put("detectedKeywords", detectedKeywords);
            map.put("rationale", rationale);
            return map;
        }
    }

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    private static final List<String> CRITICAL_KEYWORDS = Arrays.asList(
        "emergency", "danger", "burst", "life threatening", "sparking", "fire", "open manhole",
        "deep crater", "live wire", "flooding", "fatal", "toxic", "gas leak", "electrocution"
    );
    private static final List<String> HIGH_KEYWORDS = Arrays.asList(
        "urgent", "accident", "severe", "overflowing", "school zone", "hospital", "completely blocked",
        "blackout", "foul smell", "health hazard", "sinkhole", "collapsed"
    );
    private static final List<String> MEDIUM_KEYWORDS = Arrays.asList(
        "broken", "dirty", "damaged", "repair needed", "not working", "inconvenience", "leaking", "pothole", "dark"
    );

    static {
        CATEGORY_KEYWORDS.put("ROADS", Arrays.asList(
            "pothole", "crater", "road", "asphalt", "tar", "ditch", "divider", "sidewalk", "pavement",
            "footpath", "speed breaker", "traffic signal", "road collapse", "cave in", "street bump"
        ));
        CATEGORY_KEYWORDS.put("WATER_DRAINAGE", Arrays.asList(
            "pipe burst", "pipe", "water", "leakage", "leak", "sewage", "drain", "drainage", "clogged",
            "drain overflow", "dirty water", "contamination", "drinking water", "flood", "waterlogging", "sump", "tap"
        ));
        CATEGORY_KEYWORDS.put("SANITATION", Arrays.asList(
            "garbage", "trash", "dump", "waste", "odor", "stench", "dead animal", "dustbin", "bin",
            "overflowing bin", "litter", "debris", "open defecation", "sweep", "rubbish"
        ));
        CATEGORY_KEYWORDS.put("ELECTRICITY", Arrays.asList(
            "streetlight", "street light", "light", "transformer", "blackout", "wire hanging", "wire", "sparking",
            "power outage", "high voltage", "pole leaning", "pole", "electrical hazard", "dark road", "meter"
        ));
        CATEGORY_KEYWORDS.put("PUBLIC_SAFETY", Arrays.asList(
            "open manhole", "manhole", "gas leak", "hazard", "hazardous", "fire", "collapsed wall",
            "stray dog", "dog bite", "sinkhole", "dangerous structure", "fallen tree", "crime", "unauthorized"
        ));
        CATEGORY_KEYWORDS.put("PARKS", Arrays.asList(
            "broken swing", "swing", "park bench", "bench", "overgrown grass", "fallen branch",
            "fountain broken", "fountain", "public garden", "garden", "play equipment", "park"
        ));
    }

    public TriageResult analyze(String title, String description) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

        Map<String, Integer> categoryScores = new HashMap<>();
        List<String> matchedKeywords = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue()) {
                if (combined.contains(kw)) {
                    score += 10;
                    if (!matchedKeywords.contains(kw)) {
                        matchedKeywords.add(kw);
                    }
                }
            }
            categoryScores.put(entry.getKey(), score);
        }

        // Determine best category
        String bestCategory = "ROADS";
        int maxCategoryScore = -1;
        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > maxCategoryScore) {
                maxCategoryScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        if (maxCategoryScore == 0) {
            bestCategory = "ROADS"; // Default fallback
        }

        // Determine urgency & priority score
        int urgencyScore = 20; // baseline
        boolean hasCritical = false;
        boolean hasHigh = false;

        for (String kw : CRITICAL_KEYWORDS) {
            if (combined.contains(kw)) {
                urgencyScore += 35;
                hasCritical = true;
                if (!matchedKeywords.contains(kw)) matchedKeywords.add(kw);
            }
        }

        for (String kw : HIGH_KEYWORDS) {
            if (combined.contains(kw)) {
                urgencyScore += 20;
                hasHigh = true;
                if (!matchedKeywords.contains(kw)) matchedKeywords.add(kw);
            }
        }

        for (String kw : MEDIUM_KEYWORDS) {
            if (combined.contains(kw)) {
                urgencyScore += 10;
                if (!matchedKeywords.contains(kw)) matchedKeywords.add(kw);
            }
        }

        urgencyScore = Math.min(100, Math.max(15, urgencyScore));

        PriorityLevel priority;
        if (hasCritical || urgencyScore >= 75) {
            priority = PriorityLevel.CRITICAL;
        } else if (hasHigh || urgencyScore >= 50) {
            priority = PriorityLevel.HIGH;
        } else if (urgencyScore >= 30) {
            priority = PriorityLevel.MEDIUM;
        } else {
            priority = PriorityLevel.LOW;
        }

        // Map category to Department
        String department;
        switch (bestCategory) {
            case "WATER_DRAINAGE":
                department = "Water Supply & Sewerage Board";
                break;
            case "SANITATION":
                department = "Solid Waste Management & Sanitation";
                break;
            case "ELECTRICITY":
                department = "Public Lighting & Power Grid";
                break;
            case "PUBLIC_SAFETY":
                department = "Public Safety & Disaster Relief";
                break;
            case "PARKS":
                department = "Urban Parks & Green Environment";
                break;
            case "ROADS":
            default:
                department = "Roads & Highway Infrastructure";
                break;
        }

        TriageResult result = new TriageResult();
        result.category = bestCategory;
        result.department = department;
        result.priority = priority;
        result.slaHours = priority.getSlaHours();
        result.urgencyScore = urgencyScore;
        result.detectedKeywords = matchedKeywords;
        result.rationale = String.format("Auto-classified into %s based on %d civic trigger keywords. Urgency assessed at %d/100 (SLA: %dh).",
                bestCategory, matchedKeywords.size(), urgencyScore, priority.getSlaHours());

        return result;
    }
}
