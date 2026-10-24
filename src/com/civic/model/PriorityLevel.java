package com.civic.model;

public enum PriorityLevel {
    CRITICAL(12, "Critical", "#EF4444"),
    HIGH(24, "High", "#F97316"),
    MEDIUM(48, "Medium", "#FBBF24"),
    LOW(72, "Low", "#3B82F6");

    private final int slaHours;
    private final String displayName;
    private final String colorCode;

    PriorityLevel(int slaHours, String displayName, String colorCode) {
        this.slaHours = slaHours;
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public int getSlaHours() {
        return slaHours;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public static PriorityLevel fromString(String val) {
        if (val == null) return MEDIUM;
        try {
            return PriorityLevel.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
