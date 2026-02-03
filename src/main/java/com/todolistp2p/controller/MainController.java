package com.todolistp2p.controller;

import com.todolistp2p.model.Action;
import com.todolistp2p.model.Project;
import com.todolistp2p.model.Task;
import com.todolistp2p.model.TodoStateContainer;
import com.todolistp2p.service.*;
import com.todolistp2p.utils.AppConfig;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

public class MainController {
    private final String username = System.getProperty("user.name", "user");
    private final ActionLogger actionLogger = new ActionLogger();
    private final StateManager stateManager = new StateManager();
    private final PeerDiscovery discovery = new PeerDiscovery();
    private final NotificationService notificationService = new NotificationService();
    private final String nodeId = UUID.randomUUID().toString();
    private final SyncService syncService = new SyncService(username, actionLogger, stateManager, discovery, notificationService);

    private Consumer<Void> onStateChanged;

    public MainController() {
        // inform services of our local node id so they can tag outgoing messages and ignore incoming ones from self
        try {
            notificationService.setLocalNodeId(nodeId);
        } catch (Exception ignored) {}
        try {
            discovery.setLocalNodeId(nodeId);
        } catch (Exception ignored) {}
    }

    public void setOnStateChanged(Consumer<Void> c) { this.onStateChanged = c; }

    public void start() {
        // start services
        discovery.startDiscovery(username, stateManager.getLastTimestamp(), this::notifyPeersChanged);
        notificationService.startListener(AppConfig.TCP_NOTIFICATION_PORT, this::handleIncomingMessageFrom);
    }

    // notification listeners
    private final java.util.List<java.util.function.Consumer<String>> notificationListeners = new java.util.ArrayList<>();
    private final java.util.List<java.util.function.Consumer<java.util.Set<String>>> peersListeners = new java.util.ArrayList<>();

    public void addNotificationListener(java.util.function.Consumer<String> c) { notificationListeners.add(c); }
    private void notifyAllListeners(String msg) { for (var c : notificationListeners) c.accept(msg); }

    public void addPeersListener(java.util.function.Consumer<java.util.Set<String>> c) { peersListeners.add(c); }
    private void notifyPeersChanged(java.util.Set<String> set) { for (var c : peersListeners) c.accept(set); }

    private void handleIncomingMessageFrom(String line, String remoteHost) {
        if (line == null) return;
        try {
            if (line.startsWith("ACTION|")) {
                String payload = line.substring("ACTION|".length());
                Action a = Action.fromLogLine(payload);
                if (a!=null) {
                    syncService.onRemoteAction(a);
                    notifyAllListeners(formatNotification(a));
                    if (onStateChanged!=null) onStateChanged.accept(null);
                }
            } else if (line.startsWith("REQUEST_ACTIONS|")) {
                String tsStr = line.substring("REQUEST_ACTIONS|".length());
                long since = 0L;
                try { since = Long.parseLong(tsStr); } catch (NumberFormatException ignored) {}
                java.util.List<Action> list = syncService.requestActionsSince(since);
                StringBuilder sb = new StringBuilder();
                for (int i=0;i<list.size();i++) {
                    if (i>0) sb.append("#");
                    sb.append(list.get(i).toLogLine());
                }
                String msg = "ACTIONS_LIST|" + sb.toString();
                // send back to requester
                notificationService.sendTo(remoteHost, AppConfig.TCP_NOTIFICATION_PORT, msg);
            } else if (line.startsWith("ACTIONS_LIST|")) {
                String payload = line.substring("ACTIONS_LIST|".length());
                if (!payload.isEmpty()) {
                    String[] parts = payload.split("#");
                    for (String p : parts) {
                        Action a = Action.fromLogLine(p);
                        if (a!=null) {
                            syncService.onRemoteAction(a);
                            notifyAllListeners(formatNotification(a));
                        }
                    }
                    if (onStateChanged!=null) onStateChanged.accept(null);
                }
            } else if (line.startsWith("HELLO|")) {
                // optional: could respond or update peers; discovery handles UDP HELLO
            }
        } catch (IOException e) {
            // ignore errors during network handling
        }
    }

    private String formatNotification(Action a) {
        if (a==null) return "";
        switch (a.getType()) {
            case "CREATE_PROJECT": return String.format("%s a créé le projet '%s'", a.getUsername(), a.getDetails());
            case "CREATE_TASK": {
                String title = (a.getDetails()==null?"":a.getDetails()).split("\\|\\|",2)[0];
                return String.format("%s a créé la tâche '%s'", a.getUsername(), title);
            }
            case "UPDATE_TASK_STATE": {
                String state = a.getDetails();
                String pretty;
                if ("TODO".equals(state)) pretty = "À faire";
                else if ("IN_PROGRESS".equals(state)) pretty = "En cours";
                else if ("DONE".equals(state)) pretty = "Fait";
                else pretty = state == null ? "(inconnu)" : state;
                return String.format("%s a changé l'état de la tâche %s à %s", a.getUsername(), a.getTaskId(), pretty);
            }
            case "USER_CONNECTED": return String.format("%s s'est connecté", a.getUsername());
            case "USER_DISCONNECTED": return String.format("%s s'est déconnecté", a.getUsername());
            case "DELETE_TASK": {
                return String.format("%s a supprimé la tâche %s", a.getUsername(), a.getTaskId());
            }
            case "DELETE_PROJECT": {
                return String.format("%s a supprimé le projet %s", a.getUsername(), a.getProjectId());
            }
            default: return String.format("%s: %s", a.getUsername(), a.getType());
        }
    }

    private long genTimestamp() {
        return Instant.now().toEpochMilli();
    }

    private String genId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0,8);
    }

    public void createProject(String name) throws IOException {
        String id = genId("proj");
        Action a = new Action(genTimestamp(), username, "CREATE_PROJECT", id, null, name);
        syncService.onLocalAction(a);
        notifyAllListeners(formatNotification(a));
        if (onStateChanged!=null) onStateChanged.accept(null);
    }

    public void createTask(String projectId, String title, String description) throws IOException {
        String taskId = genId("task");
        String details = title + "||" + description;
        Action a = new Action(genTimestamp(), username, "CREATE_TASK", projectId, taskId, details);
        syncService.onLocalAction(a);
        notifyAllListeners(formatNotification(a));
        if (onStateChanged!=null) onStateChanged.accept(null);
    }

    public void updateTaskState(String projectId, String taskId, String newState) throws IOException {
        Action a = new Action(genTimestamp(), username, "UPDATE_TASK_STATE", projectId, taskId, newState);
        syncService.onLocalAction(a);
        notifyAllListeners(formatNotification(a));
        if (onStateChanged!=null) onStateChanged.accept(null);
    }

    /**
     * Ask peers for actions since our last processed timestamp.
     */
    public void syncNow() {
        long since = stateManager.getLastTimestamp();
        java.util.Set<String> peers = discovery.getActivePeers();
        String localIp = "";
        try { localIp = java.net.InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) {}
        for (String p : peers) {
            if (p==null || p.isEmpty()) continue;
            if (!localIp.isEmpty() && p.equals(localIp)) continue;
            String msg = "REQUEST_ACTIONS|" + since;
            notificationService.sendTo(p, AppConfig.TCP_NOTIFICATION_PORT, msg);
        }
    }

    public TodoStateContainer getState() { return syncService.getState(); }

    public void deleteTask(String projectId, String taskId) throws IOException {
        Action a = new Action(genTimestamp(), username, "DELETE_TASK", projectId, taskId, null);
        syncService.onLocalAction(a);
        notifyAllListeners(formatNotification(a));
        if (onStateChanged!=null) onStateChanged.accept(null);
    }

    public void deleteProject(String projectId) throws IOException {
        Action a = new Action(genTimestamp(), username, "DELETE_PROJECT", projectId, null, null);
        syncService.onLocalAction(a);
        notifyAllListeners(formatNotification(a));
        if (onStateChanged!=null) onStateChanged.accept(null);
    }
}
