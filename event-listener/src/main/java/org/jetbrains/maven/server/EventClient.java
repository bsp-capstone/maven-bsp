package org.jetbrains.maven.server;



import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.apache.maven.project.MavenProject;

public class EventClient {

  private final Socket socket;
  private final ObjectOutputStream outStream;

  public EventClient(String host, String port) throws IOException {
    socket = new Socket(host, Integer.parseInt(port));
    outStream = new ObjectOutputStream(socket.getOutputStream());
  }

  String handleExecutionEvent(Object packet) {
    ExecutionEvent event = (ExecutionEvent) packet;
    MojoExecution mojoExec = event.getMojoExecution();

    if (mojoExec != null) {
      return (event.getException() == null)
          ? "compile " +  event.getProject().getName() + " ("+ event.getProject().getBasedir() + ")"
          : getErrorMessage(event.getException());
    }
    String projectId = event.getProject() == null ? "unknown" : event.getProject().getId();

    if (event.getType() == ExecutionEvent.Type.SessionStarted) {
      return "execute " + printSessionStartedEventAndReactorData(event, projectId);
    }
    return "execute " + event.getType() + "id=" + projectId;
  }

  String eventToString(Object o) {
    if (o instanceof String) return (String)o;
    if (o instanceof ExecutionEvent) return handleExecutionEvent(o);
    return null;
  }
  public void send(Object packet) throws IOException {
    EventPacket eventPacket = new EventPacket(eventToString(packet));

    if (eventPacket.getEvent() != null) {
      outStream.writeObject(eventPacket);
    }
  }

  private static String getErrorMessage(Exception exception) {
    String baseMessage = exception.getMessage();
    Throwable rootCause = ExceptionUtils.getRootCause(exception);
    String rootMessage = rootCause != null ? rootCause.getMessage() : StringUtils.EMPTY;
    return "ERROR: " + rootMessage + baseMessage;
  }

  public void close() throws IOException {
    outStream.writeObject(new EventPacket());
    socket.close();
  }

  private static String printSessionStartedEventAndReactorData(ExecutionEvent event, String projectId) {
    MavenSession session = event.getSession();
    if (session != null) {
      List<MavenProject> projectsInReactor = session.getProjects();
      if (projectsInReactor == null) {
        projectsInReactor = new ArrayList<>();
      }
      StringBuilder builder = new StringBuilder();
      for (MavenProject project : projectsInReactor) {
        builder
            .append(project.getGroupId()).append(":")
            .append(project.getArtifactId()).append(":")
            .append(project.getVersion()).append("&&");
      }
      return ExecutionEvent.Type.SessionStarted + "id=" + projectId + "projects=" + builder;
    }

    return ExecutionEvent.Type.SessionStarted + "id" + projectId + "projects";
  }
}
