package com.civic.model;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class TimelineEvent {
    private String timestamp;
    private String status;
    private String actor;
    private String action;
    private String note;

    public TimelineEvent() {
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    public TimelineEvent(String status, String actor, String action, String note) {
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        this.status = status;
        this.actor = actor;
        this.action = action;
        this.note = note;
    }

    public TimelineEvent(String timestamp, String status, String actor, String action, String note) {
        this.timestamp = timestamp;
        this.status = status;
        this.actor = actor;
        this.action = action;
        this.note = note;
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
