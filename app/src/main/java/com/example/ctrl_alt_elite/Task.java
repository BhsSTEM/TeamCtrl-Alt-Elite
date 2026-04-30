package com.example.ctrl_alt_elite;

import java.util.List;
import java.util.Map;

public class Task {
    private String id;
    private String title;
    private String description;
    private String dueDate;
    private String assignedTo;
    private String repeatInterval;
    private String tractorId;
    private List<Map<String, Object>> checklist;
    private boolean completed;

    public Task() {
        // Required for Firestore
    }

    public Task(String id, String title, String description, String dueDate, String assignedTo, String repeatInterval, String tractorId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.assignedTo = assignedTo;
        this.repeatInterval = repeatInterval;
        this.tractorId = tractorId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(String repeatInterval) { this.repeatInterval = repeatInterval; }

    public String getTractorId() { return tractorId; }
    public void setTractorId(String tractorId) { this.tractorId = tractorId; }

    public List<Map<String, Object>> getChecklist() { return checklist; }
    public void setChecklist(List<Map<String, Object>> checklist) { this.checklist = checklist; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}