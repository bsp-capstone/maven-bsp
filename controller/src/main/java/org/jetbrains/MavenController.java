package org.jetbrains;

import lombok.extern.log4j.Log4j2;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.jetbrains.maven.server.EventPacket;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

@Log4j2
public class MavenController {
    private final String projectDirectory;
    Invoker invoker;

    public MavenController(String projectDirectory) {
        invoker = new DefaultInvoker();
        this.projectDirectory = projectDirectory;
    }

    private void startServer(int port) {
        Thread messenger = new Thread(() -> {
            try {
                EventServer server = new EventServer(port);
                while (server.alive()) {
                    EventPacket eventPacket = server.getPacket();
                    String message =  eventPacket.getEvent();
                    if(message != null)
                        log.error("startServer message is null " + eventPacket.getEvent());
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        messenger.start();

        log.trace("Event Server Started");
    }

    private void exec(InvocationRequest request) throws IOException {
        request.setBaseDirectory(new File(projectDirectory));
        Properties props = new Properties();
        startServer(1337);
        String jarPath = new File("../event_listener/target/maven-listener-test-0.1.0.jar")
                .toPath().toRealPath().toString();

        props.setProperty("maven.ext.class.path", jarPath);
        request.setProperties(props);
        request.setOutputHandler(null);

        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                log.error("maven command was unsuccessful");
                throw new IllegalStateException( "Build failed." );
            }

        } catch (Exception e) {
            // Signals an error during the construction of the command line used to invoke Maven
            // e.g. illegal invocation arguments.
            log.error(e.getMessage());
        }
    }

    public void compile() throws IOException {
        compile(false);
    }

    public void compile(boolean clean) throws IOException {
        InvocationRequest request = new DefaultInvocationRequest();
        if (clean) {
            request.setGoals(Arrays.asList("clean", "compile"));
        } else {
            request.setGoals(Collections.singletonList("compile"));
        }
        exec(request);
    }

    public void install() throws IOException {
        install(false);
    }

    public void install(boolean clean) throws IOException {
        InvocationRequest request = new DefaultInvocationRequest();
        if (clean) {
            request.setGoals(Arrays.asList("clean", "install"));
        } else {
            request.setGoals(Collections.singletonList("install"));
        }
        exec(request);
    }
}
