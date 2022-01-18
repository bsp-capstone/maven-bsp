import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildClientCapabilities;
import ch.epfl.scala.bsp4j.BuildServer;
import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;
import ch.epfl.scala.bsp4j.InverseSourcesParams;
import ch.epfl.scala.bsp4j.InverseSourcesResult;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.RunParams;
import ch.epfl.scala.bsp4j.RunResult;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyBuildServer implements BuildServer {

    public BuildClient client;

    @Override
    public CompletableFuture<InitializeBuildResult> buildInitialize(InitializeBuildParams initializeBuildParams) {
        System.out.println("MyBuildServer::buildInitialize");
        InitializeBuildResult initializeBuildResult = new InitializeBuildResult(
                "MyClient",
                "1.0.0",
                "2.0.0",
                new BuildServerCapabilities());
        return CompletableFuture.completedFuture(initializeBuildResult);
    }

    @Override
    public void onBuildInitialized() {
        System.out.println("MyBuildServer::buildInitialized");
    }

    @Override
    public CompletableFuture<Object> buildShutdown() {
        return null;
    }

    @Override
    public void onBuildExit() {

    }

    @Override
    public CompletableFuture<WorkspaceBuildTargetsResult> workspaceBuildTargets() {
        return null;
    }

    @Override
    public CompletableFuture<Object> workspaceReload() {
        return null;
    }

    @Override
    public CompletableFuture<SourcesResult> buildTargetSources(SourcesParams sourcesParams) {
        return CompletableFuture.completedFuture(new SourcesResult(Collections.emptyList()));
    }

    @Override
    public CompletableFuture<InverseSourcesResult> buildTargetInverseSources(InverseSourcesParams inverseSourcesParams) {
        return null;
    }

    @Override
    public CompletableFuture<DependencySourcesResult> buildTargetDependencySources(DependencySourcesParams dependencySourcesParams) {
        return null;
    }

    @Override
    public CompletableFuture<ResourcesResult> buildTargetResources(ResourcesParams resourcesParams) {
        return null;
    }

    @Override
    public CompletableFuture<CompileResult> buildTargetCompile(CompileParams compileParams) {
        return null;
    }

    @Override
    public CompletableFuture<TestResult> buildTargetTest(TestParams testParams) {
        return null;
    }

    @Override
    public CompletableFuture<RunResult> buildTargetRun(RunParams runParams) {
        return null;
    }

    @Override
    public CompletableFuture<CleanCacheResult> buildTargetCleanCache(CleanCacheParams cleanCacheParams) {
        return null;
    }

    @Override
    public CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(DependencyModulesParams dependencyModulesParams) {
        return null;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        int port = Integer.parseInt(args[0]);
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        MyBuildServer localServer = new MyBuildServer();
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("BSP is on port " + serverSocket.getLocalPort());
        threadPool.submit(() -> {
            while (true) {
                // wait for clients to connect
                Socket socket = serverSocket.accept();
                System.out.println("CONNECTED");
                Launcher<BuildClient> launcher = Launcher.createLauncher(localServer,
                        BuildClient.class,
                        socket.getInputStream(),
                        socket.getOutputStream());
                localServer.client = launcher.getRemoteProxy();
                launcher.startListening().get();
            }
        });
    }
}
