package org.jetbrains;

import org.apache.maven.shared.invoker.*;

import org.jetbrains.maven.server.EventPacket;
import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

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
                        System.out.println(eventPacket.getEvent());
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        messenger.start();

        System.out.println("Event Server Started");
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
                System.err.println("ERROR maven command was unsuccessful");
                throw new IllegalStateException( "Build failed." );
            }

        } catch (Exception e) {
            // Signals an error during the construction of the command line used to invoke Maven
            // e.g. illegal invocation arguments.
            System.out.println(e.getMessage());
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
