package org.jetbrains.maven.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class EventClient {

  private final Socket socket;
  private final ObjectOutputStream outStream;

  public EventClient(String host, String port) throws IOException {
    socket = new Socket(host, Integer.parseInt(port));
    outStream = new ObjectOutputStream(socket.getOutputStream());
  }

  public void send(Object packet) throws IOException {
    EventPacket eventPacket = null;
    if (packet instanceof String) {
      eventPacket = new EventPacket((String) packet);
    }
    if (packet instanceof ExecutionEvent) {
      ExecutionEvent event = (ExecutionEvent) packet;
      MojoExecution mojoExec = event.getMojoExecution();

      if (mojoExec != null) {
        String errMessage =
            (event.getException() == null)
                ? event.getType().name()
                : getErrorMessage(event.getException());
        eventPacket = new EventPacket(errMessage);
      }
    }

    if (eventPacket != null) {
      outStream.writeObject(eventPacket);
    }
  }

  private static String getErrorMessage(Exception exception) {
    String baseMessage = exception.getMessage();
    Throwable rootCause = ExceptionUtils.getRootCause(exception);
    String rootMessage = rootCause != null ? rootCause.getMessage() : StringUtils.EMPTY;
    return "ERROR=" + rootMessage + "MESSAGE=" + baseMessage;
  }

  public void close() throws IOException {
    outStream.writeObject(new EventPacket());
    socket.close();
  }
}
