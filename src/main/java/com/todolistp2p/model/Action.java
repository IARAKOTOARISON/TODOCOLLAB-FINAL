package com.todolistp2p.model;

import java.util.Objects;

public class Action {
    private long timestamp;
    private String username;
    private String type; // e.g., CREATE_PROJECT, CREATE_TASK, UPDATE_TASK_STATE, USER_CONNECTED, USER_DISCONNECTED
    private String projectId;
    private String taskId;
    private String details; // JSON or free text

    public Action() {}

    public Action(long timestamp, String username, String type, String projectId, String taskId, String details) {
        this.timestamp = timestamp;
        this.username = username;
        this.type = type;
        this.projectId = projectId;
        this.taskId = taskId;
        this.details = details;
    }

    public long getTimestamp() { return timestamp; }
    public String getUsername() { return username; }
    public String getType() { return type; }
    public String getProjectId() { return projectId; }
    public String getTaskId() { return taskId; }
    public String getDetails() { return details; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setUsername(String username) { this.username = username; }
    public void setType(String type) { this.type = type; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public void setDetails(String details) { this.details = details; }

    public String toLogLine() {
        // TIMESTAMP|USERNAME|ACTION_TYPE|PROJECT_ID|TASK_ID|DETAILS
        return String.format("%d|%s|%s|%s|%s|%s",
                timestamp,
                username == null ? "" : username.replace("|", " "),
                type == null ? "" : type,
                projectId == null ? "" : projectId,
                taskId == null ? "" : taskId,
                details == null ? "" : details.replace("\n", " ").replace("|", " ")
        );
    }

    public static Action fromLogLine(String line) {
        if (line == null || line.isEmpty()) return null;
        String[] parts = line.split("\\|", 6);
        if (parts.length < 6) return null;
        try {
            long ts = Long.parseLong(parts[0]);
            return new Action(ts, parts[1], parts[2], parts[3].isEmpty() ? null : parts[3], parts[4].isEmpty() ? null : parts[4], parts[5].isEmpty() ? null : parts[5]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return toLogLine();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return timestamp == action.timestamp && Objects.equals(username, action.username) && Objects.equals(type, action.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, username, type);
    }
}
