package org.jetbrains;

<<<<<<< HEAD
import lombok.extern.log4j.Log4j2;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
=======
import ch.epfl.scala.bsp4j.*;
import org.apache.maven.shared.invoker.*;
>>>>>>> 6198c5f (Event Server)
import org.jetbrains.maven.server.EventPacket;

import java.io.File;
import java.io.IOException;
<<<<<<< HEAD
=======
import java.net.ServerSocket;
>>>>>>> 6198c5f (Event Server)
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

@Log4j2
public class MavenController {
    Invoker invoker;

    BuildClient client;
    public MavenController() {
        this(null);
    }
    public MavenController(BuildClient client) {
        this.client = client;
        invoker = new DefaultInvoker();
    }

<<<<<<< HEAD
    private void startServer(int port) {
        Thread messenger = new Thread(() -> {
            try {
                EventServer server = new EventServer(port);
                while (server.alive()) {
                    EventPacket eventPacket = server.getPacket();
                    String message = eventPacket.getEvent();
                    if (message != null) {
                        log.error("startServer message is null " + eventPacket.getEvent());
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        messenger.start();
        log.info("Event Server Started");
    }

    private void exec(InvocationRequest request) throws IOException {
        request.setBaseDirectory(new File(projectDirectory));
=======
    private synchronized Integer startServer(BuildClient client) {
        int port = 0;
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            port = serverSocket.getLocalPort();

            new Thread(new Runnable() {
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
                            String message =  eventPacket.getEvent();
                            if(message != null) {
                                TaskProgressParams event = new TaskProgressParams(new TaskId("todo"));
                                event.setEventTime(System.currentTimeMillis());
                                event.setMessage(eventPacket.getEvent());
                                client.onBuildTaskProgress(event);
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }.init(serverSocket)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Event Server Started on port " + port);
        return port;
    }

    private StatusCode exec(InvocationRequest request) {
        StatusCode exitCode = StatusCode.CANCELLED;
>>>>>>> 6198c5f (Event Server)
        Properties props = new Properties();
        Integer port = startServer(client);
        String jarPath;
        try {
            jarPath = new File("../event_listener/target/maven-listener-test-0.1.0.jar")
                    .toPath().toRealPath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return StatusCode.CANCELLED;
        }

        props.setProperty("maven.ext.class.path", jarPath);
        request.setProperties(props);
        request.addShellEnvironment("BSP_EVENT_PORT", port.toString());

        request.setOutputHandler(x -> {});

        try {
<<<<<<< HEAD
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                log.error("maven command was unsuccessful: " + result.getExitCode());
                throw new IllegalStateException("Build failed.");
            }
            log.info("Invoker execution successful");
=======

            InvocationResult result = invoker.execute(request);
            exitCode = (result.getExitCode() == 0) ? StatusCode.OK : StatusCode.ERROR;
>>>>>>> 6198c5f (Event Server)
        } catch (Exception e) {
            // Signals an error during the construction of the command line used to invoke Maven
            // e.g. illegal invocation arguments.
            log.error(e.getMessage());
        }
        return exitCode;

<<<<<<< HEAD
    public void compile() throws IOException {
        log.info("MavenController::compile started");
        compile(false);
        log.info("MavenController::compile ended");
=======
>>>>>>> 6198c5f (Event Server)
    }
    public StatusCode compile(String projectDirectory) {
        return compile(projectDirectory, false);
    }
    public StatusCode compile(String projectDirectory, boolean clean) {
        InvocationRequest request = new DefaultInvocationRequest();
        if (clean) {
            request.setGoals(Arrays.asList("clean", "compile"));
        } else {
            request.setGoals(Collections.singletonList("compile"));
        }
        request.setBaseDirectory(new File (projectDirectory));
        return exec(request);
    }
<<<<<<< HEAD

    public void install() throws IOException {
        log.info("MavenController::install started");
        install(false);
        log.info("MavenController::install ended");
=======
    public StatusCode install(String projectDirectory) {
        return install(projectDirectory, false);
>>>>>>> 6198c5f (Event Server)
    }
    public StatusCode install(String projectDirectory, boolean clean) {
        InvocationRequest request = new DefaultInvocationRequest();
        if (clean) {
            request.setGoals(Arrays.asList("clean", "install"));
        } else {
            request.setGoals(Collections.singletonList("install"));
        }
        request.setBaseDirectory(new File (projectDirectory));
        return exec(request);
    }

    public StatusCode compileTargets(CompileParams compileParams) {
        StatusCode resultCode = StatusCode.CANCELLED;

        for(BuildTargetIdentifier x: compileParams.getTargets()) {
            String path = x.getUri();
            switch(compile(path)) {
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
}
