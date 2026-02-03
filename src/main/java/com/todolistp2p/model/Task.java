package com.todolistp2p.model;

public class Task {
    private String id;
    private String title;
    private String description;
    private TodoState state = TodoState.TODO;
    private long createdAt;

    public Task() {}

    public Task(String id, String title, String description, long createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.state = TodoState.TODO;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TodoState getState() { return state; }
    public long getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setState(TodoState state) { this.state = state; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
