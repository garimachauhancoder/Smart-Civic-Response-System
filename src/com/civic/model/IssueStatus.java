package com.civic.model;

public enum IssueStatus {
    REPORTED("Reported", "Issue logged by citizen into civic registry", 1),
    TRIAGED("Triaged", "Issue reviewed and assigned to nodal department", 2),
    IN_PROGRESS("In Progress", "Field team dispatched and work underway", 3),
    RESOLVED("Resolved", "Repair or civic maintenance work successfully completed", 4),
    CLOSED("Closed", "Citizen satisfaction verified and ticket closed", 5);

    private final String label;
    private final String description;
    private final int stepOrder;

    IssueStatus(String label, String description, int stepOrder) {
        this.label = label;
        this.description = description;
        this.stepOrder = stepOrder;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public static IssueStatus fromString(String val) {
        if (val == null) return REPORTED;
        String normalized = val.trim().replace(" ", "_").toUpperCase();
        try {
            return IssueStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return REPORTED;
        }
    }
}
