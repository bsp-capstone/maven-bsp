package org.jetbrains.maven.server;

import org.apache.maven.execution.ExecutionEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class EventClient {
    Socket socket;
    ObjectOutputStream outStream;

    public EventClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        outStream = new ObjectOutputStream(socket.getOutputStream());
    }

    public void send(Object packet) throws IOException {
        EventPacket eventPacket = null;

        if (packet instanceof ExecutionEvent)
            eventPacket = new EventPacket(packet.toString());

        if(eventPacket != null)
            outStream.writeObject(eventPacket);
    }

    public void close() throws IOException {
        outStream.writeObject(new EventPacket());
        socket.close();
    }
}
