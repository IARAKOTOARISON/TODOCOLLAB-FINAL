package com.todolistp2p.service;

import com.todolistp2p.utils.AppConfig;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerDiscovery {
    private final Set<String> peers = new HashSet<>();
    // map ip -> username (from HELLO) for display purposes
    private final Map<String, String> peerNames = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    private String localNodeId = null;
    public void startDiscovery(String username, long lastTimestamp, java.util.function.Consumer<java.util.Set<String>> peersListener) {
        running = true;
        Thread broadcaster = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                String msgBase = String.format("HELLO|%s|%d", username, lastTimestamp);
                String msg = msgBase;
                if (localNodeId != null) {
                    String msgId = java.util.UUID.randomUUID().toString();
                    msg = msgBase + "|ORIGIN|" + localNodeId + "|MSGID|" + msgId;
                }
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), AppConfig.UDP_DISCOVERY_PORT);
                while (running) {
                    socket.send(packet);
                    Thread.sleep(5000);
                }
            } catch (IOException | InterruptedException e) {
                // ignore
            }
        }, "peer-discover-broadcast");
        broadcaster.setDaemon(true);
        broadcaster.start();

        Thread listener = new Thread(() -> {
            try (DatagramSocket sock = new DatagramSocket(AppConfig.UDP_DISCOVERY_PORT)) {
                sock.setSoTimeout(2000);
                byte[] buf = new byte[1024];
                while (running) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    try {
                        sock.receive(p);
                        String s = new String(p.getData(), 0, p.getLength());
                        if (s.startsWith("HELLO|")) {
                            String[] parts = s.split("\\|",3);
                            if (parts.length>=2) {
                                String remoteUser = parts[1];
                                String host = p.getAddress().getHostAddress();
                                // remember the username for this IP (will be used for display)
                                if (remoteUser != null && !remoteUser.isEmpty()) peerNames.put(host, remoteUser);
                                boolean added = peers.add(host);
                                if (added && peersListener != null) {
                                    // build a display set: prefer the 'whoami' username, fallback to reverse-DNS, then show ip (no-hostname)
                                    java.util.Set<String> display = new java.util.HashSet<>();
                                    for (String ip : peers) {
                                        String displayName = null;
                                        // prefer username from HELLO
                                        String uname = peerNames.get(ip);
                                        if (uname != null && !uname.isEmpty()) {
                                            displayName = uname + " (" + ip + ")";
                                        } else {
                                            // fallback to reverse DNS hostname
                                            try {
                                                java.net.InetAddress ia = java.net.InetAddress.getByName(ip);
                                                String hn = ia.getHostName();
                                                if (hn != null && !hn.isEmpty() && !hn.equals(ip)) {
                                                    displayName = hn + " (" + ip + ")";
                                                }
                                            } catch (Exception ignored) {}
                                        }
                                        if (displayName==null) displayName = ip + " (no-hostname)";
                                        display.add(displayName);
                                    }
                                    peersListener.accept(display);
                                }
                            }
                        }
                    } catch (SocketTimeoutException ste) {
                        // loop
                    }
                }
            } catch (SocketException e) {
                // ignore
            } catch (IOException e) {
                // ignore
            }
        }, "peer-discover-listener");
        listener.setDaemon(true);
        listener.start();
    }

    public void stop() { running = false; }
    public void setLocalNodeId(String id) {
        this.localNodeId = id;
    }

    public Set<String> getActivePeers() { return peers; }
}
