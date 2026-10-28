package com.civic.model;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CivicIssue {
    private String id;
    private String title;
    private String description;
    private String category;
    private PriorityLevel priority;
    private IssueStatus status;
    private String department;
    private String assignedOfficer;
    private String address;
    private String ward;
    private double latitude;
    private double longitude;
    private String reporterName;
    private String reporterPhone;
    private String reporterEmail;
    private String photoUrl;
    private String resolutionPhotoUrl;
    private boolean fraudFlag;
    private String fraudReason;
    private int upvotes;
    private String createdAt;
    private String updatedAt;
    private String resolvedAt;
    private int slaHours;
    private String resolutionNotes;
    private List<TimelineEvent> timeline = new ArrayList<>();

    public CivicIssue() {
        this.createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        this.updatedAt = this.createdAt;
        this.status = IssueStatus.REPORTED;
        this.priority = PriorityLevel.MEDIUM;
        this.slaHours = this.priority.getSlaHours();
        this.upvotes = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public PriorityLevel getPriority() { return priority; }
    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
        if (priority != null && this.slaHours == 0) {
            this.slaHours = priority.getSlaHours();
        }
    }

    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getAssignedOfficer() { return assignedOfficer; }
    public void setAssignedOfficer(String assignedOfficer) { this.assignedOfficer = assignedOfficer; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }

    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getResolutionPhotoUrl() { return resolutionPhotoUrl; }
    public void setResolutionPhotoUrl(String resolutionPhotoUrl) { this.resolutionPhotoUrl = resolutionPhotoUrl; }
    public boolean isFraudFlag() { return fraudFlag; }
    public void setFraudFlag(boolean fraudFlag) { this.fraudFlag = fraudFlag; }
    public String getFraudReason() { return fraudReason; }
    public void setFraudReason(String fraudReason) { this.fraudReason = fraudReason; }

    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
    public synchronized int incrementUpvote() {
        this.upvotes++;
        return this.upvotes;
    }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }

    public int getSlaHours() { return slaHours; }
    public void setSlaHours(int slaHours) { this.slaHours = slaHours; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public List<TimelineEvent> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineEvent> timeline) { this.timeline = timeline; }

    public void addTimelineEvent(String status, String actor, String action, String note) {
        if (this.timeline == null) {
            this.timeline = new ArrayList<>();
        }
        this.timeline.add(new TimelineEvent(status, actor, action, note));
        this.updatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    public boolean isSlaBreached() {
        if (this.status == IssueStatus.RESOLVED || this.status == IssueStatus.CLOSED) {
            return false;
        }
        try {
            Instant created = Instant.parse(this.createdAt);
            long hoursElapsed = Duration.between(created, Instant.now()).toHours();
            return hoursElapsed > this.slaHours;
        } catch (Exception e) {
            return false;
        }
    }

    public long getRemainingSlaHours() {
        if (this.status == IssueStatus.RESOLVED || this.status == IssueStatus.CLOSED) {
            return 0;
        }
        try {
            Instant created = Instant.parse(this.createdAt);
            long hoursElapsed = Duration.between(created, Instant.now()).toHours();
            return Math.max(0, this.slaHours - hoursElapsed);
        } catch (Exception e) {
            return this.slaHours;
        }
    }
}
