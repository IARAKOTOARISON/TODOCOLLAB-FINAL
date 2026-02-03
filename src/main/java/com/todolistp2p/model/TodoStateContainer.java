package com.todolistp2p.model;

import java.util.ArrayList;
import java.util.List;

public class TodoStateContainer {
    private List<Project> projects = new ArrayList<>();

    public List<Project> getProjects() { return projects; }
    public void setProjects(List<Project> projects) { this.projects = projects; }

    public Project findProjectById(String id) {
        return projects.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    public void addProject(Project p) { projects.add(p); }

    public void removeProjectById(String id) {
        projects.removeIf(p -> p.getId() != null && p.getId().equals(id));
    }
}
