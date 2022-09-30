package org.jetbrains.maven.project;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingResult;
import org.eclipse.aether.artifact.Artifact;

@Value
@Log4j2
public class MavenProjectWrapper {

  private final ProjectBuildingResult buildingResult;
  private final URI projectBase;
  private final Map<ProjectId, MavenProjectWrapper> modulesMap;
  private final Map<String, ProjectId> uriToProjectIdMap;

  public static MavenProjectWrapper fromBase(URI projectBase) {
    File base = new File(projectBase.resolve("pom.xml").getPath());
    ProjectBuildingResult result = PomParser.buildMavenProject(base);
    Map<ProjectId, MavenProjectWrapper> modulesMap = new HashMap<>();
    Map<String, ProjectId> uriToProjectIdMap = new HashMap<>();
    getModuleProjects(result.getProject(), projectBase, modulesMap, uriToProjectIdMap);
    return new MavenProjectWrapper(result, projectBase, modulesMap, uriToProjectIdMap);
  }

  public List<BuildTargetIdentifier> getInternalDependencies(String moduleUri) {
    MavenProject project = modulesMap.get(uriToProjectIdMap.get(moduleUri)).getProject();
    List<BuildTargetIdentifier> dependencies = new ArrayList<>();
    for (Dependency dependency : project.getDependencies()) {
      ProjectId projectId =
          new ProjectId(
              dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
      if (modulesMap.containsKey(projectId)) {
        MavenProject dependentProject = modulesMap.get(projectId).getProject();
        String dependentProjectUri = dependentProject.getBasedir().toURI().toString();
        dependencies.add(new BuildTargetIdentifier(dependentProjectUri));
        log.info(
            "MavenProjectWrapper::getModuleProjects module {} dependency {}",
            projectBase,
            dependentProjectUri);
      }
    }
    return dependencies;
  }

  public List<String> getExternalDependencies(String moduleUri) {
    ProjectBuildingResult result =
        modulesMap.get(uriToProjectIdMap.get(moduleUri)).getBuildingResult();
    List<String> dependencies = new ArrayList<>();
    for (org.eclipse.aether.graph.Dependency dependency :
        result.getDependencyResolutionResult().getResolvedDependencies()) {
      Artifact dependencyArtifact = dependency.getArtifact();
      ProjectId projectId =
          new ProjectId(
              dependencyArtifact.getGroupId(),
              dependencyArtifact.getArtifactId(),
              dependencyArtifact.getVersion());
      if (!modulesMap.containsKey(projectId)) {
        dependencies.add(dependency.getArtifact().getFile().toURI().toString());
      }
    }
    return dependencies;
  }

  private static void getModuleProjects(
      MavenProject main,
      URI projectBase,
      Map<ProjectId, MavenProjectWrapper> modulesMap,
      Map<String, ProjectId> uriToProjectIdMap) {
    List<String> modules = main.getModules();
    for (String module : modules) {
      URI moduleURI = projectBase.resolve(module + "/");
      log.info("Wrapper init for module {} of {}", moduleURI, projectBase.toString());
      MavenProjectWrapper wrapper = MavenProjectWrapper.fromBase(moduleURI);
      MavenProject project = wrapper.getProject();
      ProjectId projectId =
          new ProjectId(project.getGroupId(), project.getArtifactId(), project.getVersion());
      modulesMap.put(projectId, wrapper);
      uriToProjectIdMap.put(moduleURI.toString(), projectId);
    }
  }

  public MavenProject getProject() {
    return buildingResult.getProject();
  }
}
