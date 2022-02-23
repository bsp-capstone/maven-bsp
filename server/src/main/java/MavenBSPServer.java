import ch.epfl.scala.bsp4j.*;
import com.google.common.collect.ImmutableList;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingResult;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MavenBSPServer implements BuildServer {

    public BuildClient client;
    private Path rootUri;

    @Override
    public CompletableFuture<InitializeBuildResult> buildInitialize(InitializeBuildParams initializeBuildParams) {
        System.out.println("MyBuildServer::buildInitialize");
        InitializeBuildResult initializeBuildResult = new InitializeBuildResult(
                "MyClient",
                "1.0.0",
                "2.0.0",
                new BuildServerCapabilities());
        rootUri = Path.of(initializeBuildParams.getRootUri());
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
        String mainPom = rootUri.resolve("pom.xml").toString();

        ProjectBuildingResult result = PomParser.buildMavenProject(new File(mainPom));
        MavenProject project = result.getProject();

        List<BuildTarget> modulesResult = new ArrayList<>();

        List<String> modules = project.getModules();
        for (String module : modules) {
            // Resul todo: Handle relative sub-module paths:
            // https://maven.apache.org/xsd/maven-4.0.0.xsd
            File moduleBase = rootUri.resolve(module)
                    .resolve("pom.xml")
                    .toFile();

            ProjectBuildingResult moduleResult = PomParser.buildMavenProject(moduleBase);
            MavenProject moduleProject = moduleResult.getProject();

            BuildTarget target = new BuildTarget(
                    new BuildTargetIdentifier(moduleProject.getId()),
                    ImmutableList.of(),
                    ImmutableList.of("JAVA"),
                    moduleProject.getDependencies()
                            .stream()
                            .map(dependency -> new BuildTargetIdentifier(dependency.getManagementKey()))
                            .collect(Collectors.toList()),
                    new BuildTargetCapabilities(true, true, true, true)
            );
            modulesResult.add(target);
        }
        return CompletableFuture.completedFuture(new WorkspaceBuildTargetsResult(modulesResult));
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
        MavenBSPServer localServer = new MavenBSPServer();
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
