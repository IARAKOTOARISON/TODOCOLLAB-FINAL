package com.todolistp2p.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class NotificationService {
    private final List<Thread> serverThreads = new ArrayList<>();
    private ServerSocket serverSocket;
    // local node id used to mark outgoing messages and ignore incoming ones from self
    private String localNodeId = null;
    // deduplication set for message ids
    private final Set<String> seenMsgIds = ConcurrentHashMap.newKeySet();

    /**
     * Start the TCP notification listener on the given port and pass incoming raw messages to handler
     */
    public void startListener(int port, BiConsumer<String,String> handler) {
        Thread server = new Thread(() -> {
            try (ServerSocket srv = new ServerSocket(port)) {
                this.serverSocket = srv;
                while (!srv.isClosed()) {
                    Socket client = srv.accept();
                    Thread t = new Thread(() -> handleClient(client, handler));
                    t.setDaemon(true);
                    t.start();
                    serverThreads.add(t);
                }
            } catch (IOException e) {
                // ignore
            }
        }, "tcp-notification-server");
        server.setDaemon(true);
        server.start();
    }

    private void handleClient(Socket client, BiConsumer<String,String> handler) {
        String remote = client.getInetAddress().getHostAddress();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                // Attempt to parse meta prefix: ORIGIN|<nodeId>|MSGID|<msgId>|<payload>
                String payload = line;
                String origin = null;
                String msgId = null;
                if (line != null && line.startsWith("ORIGIN|")) {
                    String[] parts = line.split("\\|", 5);
                    if (parts.length >= 5 && "MSGID".equals(parts[2])) {
                        origin = parts[1];
                        msgId = parts[3];
                        payload = parts[4];
                    }
                }

                // ignore messages originating from this node
                if (origin != null && localNodeId != null && localNodeId.equals(origin)) {
                    continue;
                }
                // dedupe by msgId
                if (msgId != null) {
                    if (!seenMsgIds.add(msgId)) continue; // already seen
                }

                handler.accept(payload, remote);
            }
        } catch (IOException e) {
            // ignore
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    public void sendTo(String host, int port, String message) {
        try (Socket s = new Socket(host, port);
             BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            String out = message;
            if (localNodeId != null) {
                String msgId = UUID.randomUUID().toString();
                out = "ORIGIN|" + localNodeId + "|MSGID|" + msgId + "|" + message;
            }
            w.write(out);
            w.newLine();
            w.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    public void setLocalNodeId(String id) {
        this.localNodeId = id;
    }
}
