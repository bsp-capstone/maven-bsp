package org.jetbrains.maven.server;

import java.net.ServerSocket;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class EventClientTest {
  @SneakyThrows
  @Test
  public void simpleTest() {
    ServerSocket serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();
    EventClient client = new EventClient("localhost", "" + port);
    client.send("hello");
    client.send(new EventPacket("event"));
    client.close();
    serverSocket.close();
  }
}