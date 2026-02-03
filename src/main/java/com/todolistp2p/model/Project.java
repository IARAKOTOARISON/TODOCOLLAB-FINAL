package com.todolistp2p.model;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private String id;
    private String name;
    private List<Task> tasks = new ArrayList<>();

    public Project() {}

    public Project(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<Task> getTasks() { return tasks; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }

    public void addTask(Task t) { tasks.add(t); }
    public void removeTaskById(String taskId) { tasks.removeIf(t -> t.getId().equals(taskId)); }
    public Task findTaskById(String taskId) {
        return tasks.stream().filter(t -> t.getId().equals(taskId)).findFirst().orElse(null);
    }
}
