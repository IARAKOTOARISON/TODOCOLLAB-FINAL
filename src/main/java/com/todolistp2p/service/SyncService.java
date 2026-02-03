package com.todolistp2p.service;

import com.todolistp2p.model.Action;
import com.todolistp2p.model.Project;
import com.todolistp2p.model.Task;
import com.todolistp2p.model.TodoStateContainer;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import com.todolistp2p.utils.AppConfig;

public class SyncService {
    private final ActionLogger logger;
    private final StateManager stateManager;
    private final PeerDiscovery discovery;
    private final NotificationService notificationService;
    private final String username;

    public SyncService(String username, ActionLogger logger, StateManager stateManager, PeerDiscovery discovery, NotificationService notificationService) {
        this.username = username;
        this.logger = logger;
        this.stateManager = stateManager;
        this.discovery = discovery;
        this.notificationService = notificationService;
    }

    public void onLocalAction(Action a) throws IOException {
        // log locally
        logger.logAction(a);
        // apply locally
        stateManager.applyAction(a);
        // broadcast to peers
        Set<String> peers = discovery.getActivePeers();
        String msg = "ACTION|" + a.toLogLine();
        String localIp = "";
        try { localIp = java.net.InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) {}
        for (String p : peers) {
            if (p==null || p.isEmpty()) continue;
            if (!localIp.isEmpty() && p.equals(localIp)) continue; // skip self
            notificationService.sendTo(p, AppConfig.TCP_NOTIFICATION_PORT, msg);
        }
    }

    public void onRemoteAction(Action a) throws IOException {
        // Check if new
        if (a.getTimestamp() <= stateManager.getLastTimestamp()) return;
        logger.logAction(a);
        stateManager.applyAction(a);
    }

    public TodoStateContainer getState() { return stateManager.getStateContainer(); }

    public List<Action> requestActionsSince(long since) throws IOException {
        return logger.getActionsSince(since);
    }
}
