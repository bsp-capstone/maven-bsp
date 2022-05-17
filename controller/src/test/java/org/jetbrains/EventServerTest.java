package org.jetbrains;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.maven.server.EventClient;
import org.jetbrains.maven.server.EventPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Log4j2
public class EventServerTest {
  private CountDownLatch latch;
  private String recvMessage;
  private EventServer server;

  @Test
  @SneakyThrows
  public void simpleTest() {
    ServerSocket serverSocket = new ServerSocket(0);
    EventServer server = new EventServer(serverSocket);
    int port = serverSocket.getLocalPort();
    log.info("Server started on port: " + port);
    Assertions.assertTrue(server.alive());
  }

  @SneakyThrows
  @Test
  public void connectTest() {
    int port = startServer();
    EventClient client = new EventClient("localhost", port + "");
    assertClose(client);
  }

  @SneakyThrows
  @Test
  public void multiSendTest() {
    int port = startServer();
    EventClient client = new EventClient("localhost", port + "");

    for(int i = 1; i < (1<<30); i *=2) {
      assertSend(client, i + "", 100);
    }

    assertClose(client);
  }

  private int startServer() throws IOException {
    ServerSocket serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();

    new Thread(
        new Runnable() {
          private ServerSocket serverSocket;
          public Runnable init(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
            return this;
          }

          @SneakyThrows
          @Override
          public void run() {
            server = new EventServer(serverSocket);

            while (server.alive()) {
              EventPacket eventPacket = server.getPacket();
              recvMessage = eventPacket.getEvent();
              latch.countDown();
            }
            latch.countDown();
          }
        }.init(serverSocket)).start();
    return port;
  }

  private void assertSend(EventClient client, String message, int times) throws IOException {
    latch = new CountDownLatch(times);
    for(int i = 0; i < times; i++)
      client.send(message);

    assertMessage(message);
  }

  private void assertClose(EventClient client) throws IOException, InterruptedException {
    latch = new CountDownLatch(1);
    client.close();
    latch.await();
    Assertions.assertFalse(server.alive());
  }

  @SneakyThrows
  private void assertMessage(String message) {
    latch.await();
    if(recvMessage != null)
      Assertions.assertEquals(message, recvMessage);
  }
}