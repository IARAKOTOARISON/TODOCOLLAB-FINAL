package com.todolistp2p.service;

import com.todolistp2p.model.Action;
import com.todolistp2p.utils.FileHelpers;
import com.todolistp2p.utils.AppConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ActionLogger {
    private final Path actionsFile = Path.of(AppConfig.DATA_DIR).resolve("actions.log");

    public synchronized void logAction(Action action) throws IOException {
        FileHelpers.appendLine(actionsFile, action.toLogLine());
    }

    public List<Action> getAllActions() throws IOException {
        List<String> lines = FileHelpers.readAllLines(actionsFile);
        List<Action> out = new ArrayList<>();
        for (String l : lines) {
            Action a = Action.fromLogLine(l);
            if (a != null) out.add(a);
        }
        return out;
    }

    public List<Action> getActionsSince(long sinceTs) throws IOException {
        List<Action> all = getAllActions();
        List<Action> out = new ArrayList<>();
        for (Action a : all) if (a.getTimestamp() > sinceTs) out.add(a);
        return out;
    }
}
