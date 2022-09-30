package org.jetbrains.maven.server;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildServer;
import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetDataKind;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesItem;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;
import ch.epfl.scala.bsp4j.InverseSourcesParams;
import ch.epfl.scala.bsp4j.InverseSourcesResult;
import ch.epfl.scala.bsp4j.JavaBuildServer;
import ch.epfl.scala.bsp4j.JavacOptionsItem;
import ch.epfl.scala.bsp4j.JavacOptionsParams;
import ch.epfl.scala.bsp4j.JavacOptionsResult;
import ch.epfl.scala.bsp4j.JvmBuildTarget;
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
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskStartParams;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.jetbrains.MavenController;
import org.jetbrains.maven.project.MavenProjectWrapper;
import org.jetbrains.maven.project.ProjectId;

@Log4j2
public class MavenBSPServer implements BuildServer, JavaBuildServer {

  @Setter public BuildClient client;
  private URI rootUri;
  private MavenProjectWrapper mainProjectWrapper;

  @Override
  public CompletableFuture<InitializeBuildResult> buildInitialize(
      InitializeBuildParams initializeBuildParams) {
    log.info("MyBuildServer::buildInitialize started");
    BuildServerCapabilities capabilities = new BuildServerCapabilities();
    capabilities.setResourcesProvider(true);
    capabilities.setDependencySourcesProvider(true);
    InitializeBuildResult initializeBuildResult =
        new InitializeBuildResult("maven-bsp", "1.0.0", "1.0.0", capabilities);
    rootUri = URI.create(initializeBuildParams.getRootUri());
    mainProjectWrapper = MavenProjectWrapper.fromBase(rootUri);
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
  public void onBuildExit() {}

  @Override
  public CompletableFuture<WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    Map<ProjectId, MavenProjectWrapper> modulesMap = mainProjectWrapper.getModulesMap();
    List<BuildTarget> modulesResult = new ArrayList<>();
    for (Map.Entry<ProjectId, MavenProjectWrapper> module : modulesMap.entrySet()) {
      MavenProjectWrapper moduleProjectWrapper = module.getValue();
      String moduleUri = moduleProjectWrapper.getProjectBase().toString();
      var target =
          new BuildTarget(
              new BuildTargetIdentifier(moduleUri),
              List.of(),
              List.of("java"),
              mainProjectWrapper.getInternalDependencies(moduleUri),
              new BuildTargetCapabilities(true, true, true, true));
      target.setDisplayName(moduleProjectWrapper.getProject().getName());
      log.info("target {} project base {}", moduleProjectWrapper.getProject().getName(), moduleUri);
      target.setBaseDirectory(moduleUri);
      target.setDataKind(BuildTargetDataKind.JVM);
      String javaHome = System.getProperty("java.home");
      String javaVersion = System.getProperty("java.version");
      log.info("java.home {} \n java.version {}", javaHome, javaVersion);
      target.setData(new JvmBuildTarget("file:" + javaHome, javaVersion));
      modulesResult.add(target);
    }
    return CompletableFuture.completedFuture(new WorkspaceBuildTargetsResult(modulesResult));
  }

  @Override
  public CompletableFuture<Object> workspaceReload() {
    return CompletableFuture.completedFuture("reload");
  }

  @Override
  public CompletableFuture<SourcesResult> buildTargetSources(SourcesParams sourcesParams) {
    List<BuildTargetIdentifier> targets = sourcesParams.getTargets();
    List<SourcesItem> items =
        targets.stream()
            .map(
                target -> {
                  URI targetUri = URI.create(target.getUri());
                  List<SourceItem> targetItems = new ArrayList<>();
                  String targetSourcesUri = targetUri.resolve("src/main/java/").toString();
                  SourceItem src =
                      new SourceItem(targetSourcesUri, SourceItemKind.DIRECTORY, false);
                  log.info("target {} with sources item {}", targetUri, src.getUri());
                  targetItems.add(src);
                  SourcesItem targetSources = new SourcesItem(target, targetItems);
                  List<String> roots = List.of(targetSourcesUri);
                  targetSources.setRoots(roots);
                  return targetSources;
                })
            .collect(Collectors.toList());
    return CompletableFuture.completedFuture(new SourcesResult(items));
  }

  @Override
  public CompletableFuture<InverseSourcesResult> buildTargetInverseSources(
      InverseSourcesParams inverseSourcesParams) {
    return CompletableFuture.completedFuture(new InverseSourcesResult(Collections.emptyList()));
  }

  @Override
  public CompletableFuture<DependencySourcesResult> buildTargetDependencySources(
      DependencySourcesParams dependencySourcesParams) {
    log.info("MavenBSPServer::buildTargetDependencySources");
    List<DependencySourcesItem> items = new ArrayList<>();

    for (BuildTargetIdentifier target : dependencySourcesParams.getTargets()) {
      DependencySourcesItem localRepoItem =
          new DependencySourcesItem(
              target, mainProjectWrapper.getExternalDependencies(target.getUri()));
      items.add(localRepoItem);
    }
    return CompletableFuture.completedFuture(new DependencySourcesResult(items));
  }

  @Override
  public CompletableFuture<ResourcesResult> buildTargetResources(ResourcesParams resourcesParams) {
    log.info("buildTargetResources");
    List<BuildTargetIdentifier> targets = resourcesParams.getTargets();
    List<ResourcesItem> resources = new ArrayList<>();
    targets.forEach(
        target -> {
          URI targetUri = URI.create(target.getUri());
          List<String> targetResources = new ArrayList<>();
          String resourceUri = targetUri.resolve("src/main/resources/").toString();
          log.info("Target {} resourceUri {}", target, resourceUri);
          targetResources.add(resourceUri);
          resources.add(new ResourcesItem(target, targetResources));
        });
    return CompletableFuture.completedFuture(new ResourcesResult(resources));
  }

  @Override
  public CompletableFuture<CompileResult> buildTargetCompile(CompileParams compileParams) {
    MavenController controller = new MavenController(client);
    client.onBuildTaskStart(new TaskStartParams(new TaskId("taskId")));

    StatusCode exitCode = controller.compileTargets(compileParams);

    client.onBuildTaskFinish(new TaskFinishParams(new TaskId("taskId"), exitCode));
    return CompletableFuture.completedFuture(new CompileResult(exitCode));
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
  public CompletableFuture<CleanCacheResult> buildTargetCleanCache(
      CleanCacheParams cleanCacheParams) {
    return CompletableFuture.completedFuture(new CleanCacheResult("clean", true));
  }

  @Override
  public CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(
      DependencyModulesParams dependencyModulesParams) {
    log.info("MavenBSPServer::buildTargetDependencyModules");
    return CompletableFuture.completedFuture(new DependencyModulesResult(Collections.emptyList()));
  }

  @Override
  public CompletableFuture<JavacOptionsResult> buildTargetJavacOptions(JavacOptionsParams params) {
    log.info("MavenBSPServer::buildTargetJavacOptions");
    List<JavacOptionsItem> items = new ArrayList<>();

    for (BuildTargetIdentifier target : params.getTargets()) {
      JavacOptionsItem item =
          new JavacOptionsItem(
              target,
              Collections.emptyList(),
              mainProjectWrapper.getExternalDependencies(target.getUri()),
              rootUri.toString());
      items.add(item);
    }

    return CompletableFuture.completedFuture(new JavacOptionsResult(items));
  }

  public static void main(String[] args)
      throws ExecutionException, InterruptedException, IOException {
    int port = Integer.parseInt(args[0]);
    ExecutorService threadPool = Executors.newFixedThreadPool(1);
    MavenBSPServer localServer = new MavenBSPServer();
    ServerSocket serverSocket = new ServerSocket(port);
    log.info("BSP is on port " + serverSocket.getLocalPort());
    threadPool.submit(
        () -> {
          while (true) {
            // wait for clients to connect
            Socket socket = serverSocket.accept();
            log.info("Client connected");
            Launcher<BuildClient> launcher =
                Launcher.createLauncher(
                    localServer,
                    BuildClient.class,
                    socket.getInputStream(),
                    socket.getOutputStream());
            localServer.client = launcher.getRemoteProxy();
            launcher.startListening().get();
          }
        });
  }
}
