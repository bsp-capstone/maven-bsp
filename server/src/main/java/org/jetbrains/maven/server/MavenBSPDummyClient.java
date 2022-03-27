package org.jetbrains.maven.server;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildClientCapabilities;
import ch.epfl.scala.bsp4j.BuildServer;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.DidChangeBuildTarget;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;
import ch.epfl.scala.bsp4j.LogMessageParams;
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MavenBSPDummyClient implements BuildClient {

    @Override
    public void onBuildShowMessage(ShowMessageParams params) {
    }

    @Override
    public void onBuildLogMessage(LogMessageParams params) {

    }

    @Override
    public void onBuildTaskStart(TaskStartParams params) {
        System.out.println("Build Task Started with task id: " + params.getTaskId().getId());
    }

    @Override
    public void onBuildTaskProgress(TaskProgressParams params) {
        System.out.println("progress event: " + params.getMessage());
    }

    @Override
    public void onBuildTaskFinish(TaskFinishParams params) {
        System.out.println("Build task Finished with code: " + params.getStatus());
    }

    @Override
    public void onBuildPublishDiagnostics(PublishDiagnosticsParams params) {

    }

    @Override
    public void onBuildTargetDidChange(DidChangeBuildTarget params) {

    }

    public static void main(String[] args) throws IOException {
        BuildClient localClient = new MavenBSPDummyClient();
        ExecutorService service = Executors.newFixedThreadPool(1);

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        Socket socket = new Socket(host, port);

        Launcher<BuildServer> launcher = new Launcher.Builder<BuildServer>()
                .setInput(socket.getInputStream())
                .setOutput(socket.getOutputStream())
                .setLocalService(localClient)
                .setExecutorService(service)
                .setRemoteInterface(BuildServer.class)
                .create();
        BuildServer server = launcher.getRemoteProxy();
        new Thread() {
            public void run() {
                try {
                    launcher.startListening().get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        Path workspace = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
        CompletableFuture<InitializeBuildResult> initial = server.buildInitialize(new InitializeBuildParams(
                "MyClient",
                "1.0.0",
                "2.0.0",
                workspace.toString(),
                new BuildClientCapabilities(java.util.Collections.singletonList("scala"))
        ));
        initial.thenAccept(x -> server.onBuildInitialized());
        initial.thenAccept(x -> System.out.println(server.workspaceBuildTargets()));

        List<BuildTargetIdentifier> targets = new ArrayList<>();
        String proj = new File("../controller/mvn_test_project").getCanonicalPath();
        targets.add(new BuildTargetIdentifier(proj));

        initial.thenAccept(x -> server.buildTargetCompile(new CompileParams(targets)));
    }
}
