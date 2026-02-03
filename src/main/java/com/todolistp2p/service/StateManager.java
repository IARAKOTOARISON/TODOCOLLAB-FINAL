package com.todolistp2p.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.todolistp2p.model.Action;
import com.todolistp2p.model.Project;
import com.todolistp2p.model.Task;
import com.todolistp2p.model.TodoStateContainer;
import com.todolistp2p.model.TodoState;
import com.todolistp2p.utils.AppConfig;
import com.todolistp2p.utils.FileHelpers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class StateManager {
    private final Path stateFile = Path.of(AppConfig.DATA_DIR).resolve("state.json");
    private final Path lastProcessedFile = Path.of(AppConfig.DATA_DIR).resolve("last_processed.txt");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private TodoStateContainer state = new TodoStateContainer();
    private long lastTimestamp = 0L;

    public StateManager() {
        try { loadState(); } catch (IOException e) { /* ignore, will start empty */ }
    }

    public synchronized void loadState() throws IOException {
        List<String> lines = FileHelpers.readAllLines(stateFile);
        if (!lines.isEmpty()) {
            String json = String.join(System.lineSeparator(), lines);
            TodoStateContainer c = gson.fromJson(json, TodoStateContainer.class);
            if (c != null) state = c;
        }
        List<String> lp = FileHelpers.readAllLines(lastProcessedFile);
        if (!lp.isEmpty()) {
            try { lastTimestamp = Long.parseLong(lp.get(0).trim()); } catch (NumberFormatException ignored) {}
        }
    }

    public synchronized void saveState() throws IOException {
        String json = gson.toJson(state);
        FileHelpers.appendLine(stateFile, json);
    }

    public synchronized TodoStateContainer getStateContainer() { return state; }

    public synchronized long getLastTimestamp() { return lastTimestamp; }

    public synchronized void setLastTimestamp(long ts) throws IOException {
        this.lastTimestamp = ts;
        Path p = lastProcessedFile;
        // overwrite file
        java.nio.file.Files.writeString(p, String.valueOf(ts));
    }

    public synchronized void applyAction(Action a) throws IOException {
        if (a == null) return;
        // ignore already processed timestamps
        if (a.getTimestamp() <= lastTimestamp) return;

        String type = a.getType();
        switch (type) {
            case "CREATE_PROJECT": {
                String name = a.getDetails();
                Project p = new Project(a.getProjectId(), name);
                state.addProject(p);
                break;
            }
            case "CREATE_TASK": {
                // details: title||description
                String[] parts = (a.getDetails() == null ? "" : a.getDetails()).split("\\|\\|",2);
                String title = parts.length>0?parts[0]:"";
                String desc = parts.length>1?parts[1]:"";
                Task t = new Task(a.getTaskId(), title, desc, a.getTimestamp());
                Project proj = state.findProjectById(a.getProjectId());
                if (proj != null) proj.addTask(t);
                break;
            }
            case "UPDATE_TASK_STATE": {
                String newState = a.getDetails();
                Project proj = state.findProjectById(a.getProjectId());
                if (proj != null) {
                    Task t = proj.findTaskById(a.getTaskId());
                    if (t != null) {
                        t.setState(TodoState.valueOf(newState));
                    }
                }
                break;
            }
            case "DELETE_TASK": {
                Project proj = state.findProjectById(a.getProjectId());
                if (proj != null) proj.removeTaskById(a.getTaskId());
                break;
            }
            case "DELETE_PROJECT": {
                // remove project by id
                state.removeProjectById(a.getProjectId());
                break;
            }
            // other types: USER_CONNECTED, USER_DISCONNECTED - ignored for state
            default:
                break;
        }
        // update last processed timestamp
        setLastTimestamp(a.getTimestamp());
        // persist state (overwrite simple approach)
        java.nio.file.Files.writeString(stateFile, gson.toJson(state));
    }
}
