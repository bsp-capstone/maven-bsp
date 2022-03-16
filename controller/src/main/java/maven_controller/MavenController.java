package maven_controller;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public class MavenController {

    private final String projectDirectory;
    private final Invoker invoker;

    public MavenController(String projectDirectory) {
        this.invoker = new DefaultInvoker();
        this.projectDirectory = projectDirectory;
    }

    private void exec(InvocationRequest request) {
        //invoker.setMavenHome(new File("/usr/share/maven"));
        request.setBaseDirectory(new File(projectDirectory));
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                System.err.println("ERROR maven command was unsuccessful");
            }

        } catch (MavenInvocationException e) {
            // Signals an error during the construction of the command line used to invoke Maven
            // e.g. illegal invocation arguments.
            e.printStackTrace();
        }
    }

    public void compile() {
        compile(false);
    }

    public void compile(boolean clean) {
        InvocationRequest request = new DefaultInvocationRequest();
        if (clean) {
            request.setGoals(Arrays.asList("clean", "compile"));
        } else {
            request.setGoals(Collections.singletonList("compile"));
        }
        exec(request);
    }

    public void install() {
        install(false);
    }

    public void install(boolean clean) {
        InvocationRequest request = new DefaultInvocationRequest();
        if (clean) {
            request.setGoals(Arrays.asList("clean", "install"));
        } else {
            request.setGoals(Collections.singletonList("install"));
        }
        exec(request);
    }
}
