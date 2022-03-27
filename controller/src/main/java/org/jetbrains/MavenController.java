package org.jetbrains;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.MessageType;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.jetbrains.maven.server.EventPacket;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

// todo resul: pom errors not returned

@Log4j2
public class MavenController {

  private final Invoker invoker;
  private final BuildClient client;

  public MavenController() {
    this(null);
  }

  public MavenController(BuildClient client) {
    this.client = client;
    invoker = new DefaultInvoker();
  }

  public StatusCode compileTargets(CompileParams compileParams) {
    StatusCode resultCode = StatusCode.CANCELLED;

    for (BuildTargetIdentifier x : compileParams.getTargets()) {
      String path = URI.create(x.getUri()).getPath();
      switch (compile(path)) {
        case OK:
          resultCode = StatusCode.OK;
          break;

        case ERROR:
          return StatusCode.ERROR;

        case CANCELLED:
          return StatusCode.CANCELLED;
      }
    }

    return resultCode;
  }

  public StatusCode compile(String projectDirectory) {
    return compile(projectDirectory, false);
  }

  public StatusCode compile(String projectDirectory, boolean clean) {
    log.info("MavenController::compile started");
    InvocationRequest request = new DefaultInvocationRequest();
    if (clean) {
      request.setGoals(Arrays.asList("clean", "compile"));
    } else {
      request.setGoals(Collections.singletonList("compile"));
    }
    request.setBaseDirectory(new File(projectDirectory));

    StatusCode ret = exec(request);
    log.info("MavenController::compile ended");
    return ret;
  }

  public StatusCode install(String projectDirectory) {
    return install(projectDirectory, false);
  }

  public StatusCode install(String projectDirectory, boolean clean) {
    log.info("MavenController::install started");
    InvocationRequest request = new DefaultInvocationRequest();
    if (clean) {
      request.setGoals(Arrays.asList("clean", "install"));
    } else {
      request.setGoals(Collections.singletonList("install"));
    }
    request.setBaseDirectory(new File(projectDirectory));
    StatusCode ret = exec(request);
    log.info("MavenController::install ended");
    return ret;
  }

  private StatusCode exec(InvocationRequest request) {
    StatusCode exitCode = StatusCode.CANCELLED;
    Properties props = new Properties();
    Integer port = startServer(client);
    String jarPath;
    try {
      jarPath =
          new File("../event_listener/target/maven-listener-test-1.0-SNAPSHOT.jar")
              .toPath()
              .toRealPath()
              .toString();
    } catch (IOException e) {
      e.printStackTrace();
      log.error(e.getMessage());
      return StatusCode.CANCELLED;
    }

    props.setProperty("maven.ext.class.path", jarPath);
    request.setProperties(props);
    request.addShellEnvironment("BSP_EVENT_PORT", port.toString());
    request.setOutputHandler(x -> {});

    try {
      InvocationResult result = invoker.execute(request);
      if (result.getExitCode() != 0) {
        StringBuilder errorMessage = new StringBuilder("maven command was unsuccessful");
        errorMessage
            .append(": ")
            .append(request.getBaseDirectory())
            .append(": ")
            .append(
                result.getExecutionException() == null
                    ? result.getExitCode()
                    : result.getExecutionException());
        log.error(errorMessage);
        throw new IllegalStateException("Build failed.");
      }
      log.info("Invoker execution successful: " + request.getBaseDirectory());
      exitCode = (result.getExitCode() == 0) ? StatusCode.OK : StatusCode.ERROR;
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return exitCode;
  }

  private synchronized Integer startServer(BuildClient client) {
    int port = 0;
    try {
      ServerSocket serverSocket = new ServerSocket(0);
      port = serverSocket.getLocalPort();

      new Thread(
              new Runnable() {
                private ServerSocket serverSocket;

                public Runnable init(ServerSocket serverSocket) {
                  this.serverSocket = serverSocket;
                  return this;
                }

                @Override
                public void run() {
                  try {
                    EventServer server = new EventServer(serverSocket);

                    while (server.alive()) {
                      EventPacket eventPacket = server.getPacket();
                      String message = eventPacket.getEvent();
                      if (message != null) {
                        TaskProgressParams event = new TaskProgressParams(new TaskId("todo"));
                        event.setEventTime(System.currentTimeMillis());
                        event.setMessage(eventPacket.getEvent());
                        client.onBuildTaskProgress(event);
                        ShowMessageParams messageParams =
                            new ShowMessageParams(MessageType.INFORMATION, eventPacket.getEvent());
                        client.onBuildShowMessage(messageParams);
                        log.info("sent event to client: " + eventPacket.getEvent());
                      }
                    }
                  } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                  }
                }
              }.init(serverSocket))
          .start();
    } catch (IOException e) {
      e.printStackTrace();
      log.error(e.getMessage());
    }
    log.info("Event Server Started on port " + port);
    return port;
  }
}
