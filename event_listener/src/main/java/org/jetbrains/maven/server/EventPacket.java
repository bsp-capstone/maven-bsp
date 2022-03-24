package org.jetbrains.maven.server;

import java.io.Serializable;

public class EventPacket implements Serializable {
    private final String message;
    public EventPacket(String message) {
        this.message = message;
    }
    public EventPacket() {
        message = null;
    }
    public String getEvent() {
        return message;
    }
}
