package org.jetbrains;

import org.jetbrains.maven.server.EventPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class EventServer {
    private boolean is_alive;
    ServerSocket serverSocket;
    Socket socket = null;
    ObjectInputStream inStream;

    public EventServer(int port) throws IOException, ClassNotFoundException {
        serverSocket = new ServerSocket(port);
        is_alive = true;
    }

    public EventPacket getPacket() throws IOException, ClassNotFoundException {
        if (socket == null) {
            socket = serverSocket.accept(); // Blocking
            inStream = new ObjectInputStream(socket.getInputStream());
        }

        EventPacket eventPacket = (EventPacket)inStream.readObject();
        if (eventPacket.getEvent() == null) {
            serverSocket.close();
            is_alive = false;
        }
        return eventPacket;
    }

    public boolean alive() {
        return is_alive;
    }
}
