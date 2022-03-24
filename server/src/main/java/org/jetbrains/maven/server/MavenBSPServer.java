package org.jetbrains.maven.server;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildServer;
import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
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
import ch.epfl.scala.bsp4j.ResourcesItem;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.RunParams;
import ch.epfl.scala.bsp4j.RunResult;
import ch.epfl.scala.bsp4j.SourceItem;
import ch.epfl.scala.bsp4j.SourceItemKind;
import ch.epfl.scala.bsp4j.SourcesItem;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import lombok.extern.log4j.Log4j2;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.jetbrains.maven.project.MavenProjectWrapper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Log4j2
public class MavenBSPServer implements BuildServer {

    public BuildClient client;
    private URI rootUri;

    @Override
    public CompletableFuture<InitializeBuildResult> buildInitialize(InitializeBuildParams initializeBuildParams) {
        log.info("MyBuildServer::buildInitialize started");
        InitializeBuildResult initializeBuildResult = new InitializeBuildResult(
                "maven-bsp",
                "1.0.0",
                "1.0.0",
                new BuildServerCapabilities());
        rootUri = URI.create(initializeBuildParams.getRootUri());
        return CompletableFuture.completedFuture(initializeBuildResult);
    }

    @Override
    public void onBuildInitialized() {
        log.info("MyBuildServer::buildInitialized");
    }

    @Override
    public CompletableFuture<Object> buildShutdown() {
        return CompletableFuture.completedFuture("finished");
    }

    @Override
    public void onBuildExit() {

    }

    @Override
    public CompletableFuture<WorkspaceBuildTargetsResult> workspaceBuildTargets() {
        URI mainPom = rootUri.resolve("pom.xml");
        MavenProjectWrapper projectWrapper = MavenProjectWrapper.fromBase(mainPom);

        List<String> modules = projectWrapper.getProject().getModules();
        List<BuildTarget> modulesResult = modules.stream()
                // Resul todo: Handle relative sub-module paths:
                // https://maven.apache.org/xsd/maven-4.0.0.xsd
                .map(module -> rootUri.resolve(module + "/"))
                .map(MavenProjectWrapper::fromBase)
                .map(moduleProjectWrapper -> {
                    var target = new BuildTarget(
                        // resul todo: change to valid uri
                        new BuildTargetIdentifier(moduleProjectWrapper
                                .getProjectBase()
                                .toString()),
                        List.of(),
                        List.of("JAVA"),
                        moduleProjectWrapper.getDependencies(),
                        new BuildTargetCapabilities(true, true, true, true)
                    );
                    target.setDisplayName("displayName");
                    target.setBaseDirectory(moduleProjectWrapper.getProjectBase().toString());
                    return target;
                })
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(new WorkspaceBuildTargetsResult(modulesResult));
    }

    // resul todo: return success instead of null (same for all end-points)
    @Override
    public CompletableFuture<Object> workspaceReload() {
        return CompletableFuture.completedFuture("reload");
    }

    @Override
    public CompletableFuture<SourcesResult> buildTargetSources(SourcesParams sourcesParams) {
        List<BuildTargetIdentifier> targets = sourcesParams.getTargets();
        List<SourcesItem> items = targets.stream()
            .map(target -> {
                URI targetUri = URI.create(target.getUri());
                List<SourceItem> targetItems = new ArrayList<>();
                SourceItem src = new SourceItem(
                        targetUri.resolve("src/").toString(),
                        SourceItemKind.DIRECTORY,
                        false
                );
                targetItems.add(src);
                SourcesItem targetSources = new SourcesItem(
                        target,
                        targetItems
                );
                List<String> roots = List.of(targetUri.toString());
                targetSources.setRoots(roots);
                return targetSources;
            })
            .collect(Collectors.toList());
        return CompletableFuture.completedFuture(new SourcesResult(items));
    }

    @Override
    public CompletableFuture<InverseSourcesResult> buildTargetInverseSources(InverseSourcesParams inverseSourcesParams) {
        return CompletableFuture.completedFuture(new InverseSourcesResult(Collections.emptyList()));
    }

    @Override
    public CompletableFuture<DependencySourcesResult> buildTargetDependencySources(DependencySourcesParams dependencySourcesParams) {
        return CompletableFuture.completedFuture(new DependencySourcesResult(Collections.emptyList()));
    }

    @Override
    public CompletableFuture<ResourcesResult> buildTargetResources(ResourcesParams resourcesParams) {
        List<BuildTargetIdentifier> targets = resourcesParams.getTargets();
        List<ResourcesItem> resources = new ArrayList<>();
        targets.forEach(target -> {
            URI targetUri = URI.create(target.getUri());
            List<String> targetResources = new ArrayList<>();
            String resourceUri = targetUri.resolve("resources/").toString();
            targetResources.add(resourceUri);
            resources.add(new ResourcesItem(target, targetResources));
        });
        return CompletableFuture.completedFuture(new ResourcesResult(resources));
    }

    @Override
    public CompletableFuture<CompileResult> buildTargetCompile(CompileParams compileParams) {
        return CompletableFuture.completedFuture(new CompileResult(StatusCode.OK));
    }

    @Override
    public CompletableFuture<TestResult> buildTargetTest(TestParams testParams) {
        return CompletableFuture.completedFuture(new TestResult(StatusCode.OK));
    }

    @Override
    public CompletableFuture<RunResult> buildTargetRun(RunParams runParams) {
        return CompletableFuture.completedFuture(new RunResult(StatusCode.OK));
    }

    @Override
    public CompletableFuture<CleanCacheResult> buildTargetCleanCache(CleanCacheParams cleanCacheParams) {
        return CompletableFuture.completedFuture(new CleanCacheResult("clean", true));
    }

    @Override
    public CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(DependencyModulesParams dependencyModulesParams) {
        return CompletableFuture.completedFuture(new DependencyModulesResult(Collections.emptyList()));
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        int port = Integer.parseInt(args[0]);
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        MavenBSPServer localServer = new MavenBSPServer();
        ServerSocket serverSocket = new ServerSocket(port);
        log.info("BSP is on port " + serverSocket.getLocalPort());
        threadPool.submit(() -> {
            while (true) {
                // wait for clients to connect
                Socket socket = serverSocket.accept();
                log.info("Client connected");
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
