package com.todolistp2p.model;

public class Peer {
    private String id;
    private String address; // ip:port

    public Peer() {}

    public Peer(String id, String address) { this.id = id; this.address = address; }

    public String getId() { return id; }
    public String getAddress() { return address; }

    public void setId(String id) { this.id = id; }
    public void setAddress(String address) { this.address = address; }
}
