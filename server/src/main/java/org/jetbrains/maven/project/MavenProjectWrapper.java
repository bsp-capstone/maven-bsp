package org.jetbrains.maven.project;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingResult;

// todo resul: add tests
@Value
public class MavenProjectWrapper {

  private final ProjectBuildingResult buildingResult;
  private final URI projectBase;

  public static MavenProjectWrapper fromBase(URI projectBase) {
    File base = new File(projectBase.resolve("pom.xml").getPath());
    ProjectBuildingResult result = PomParser.buildMavenProject(base);
    return new MavenProjectWrapper(result, projectBase);
  }

  public List<BuildTargetIdentifier> getDependencies(Map<ProjectId, MavenProjectWrapper> modules) {
    MavenProject project = getProject();
    List<BuildTargetIdentifier> dependencies = new ArrayList<>();
    for (Dependency dependency : project.getDependencies()) {
      ProjectId projectId = new ProjectId(
          dependency.getGroupId(),
          dependency.getArtifactId(),
          dependency.getVersion()
      );
      if (modules.containsKey(projectId)) {
        MavenProject dependentProject = modules.get(projectId).getProject();
        String dependentProjectUri = dependentProject.getBasedir().toURI().toString();
        dependencies.add(new BuildTargetIdentifier(dependentProjectUri));
      }
    }
    return dependencies;
  }

  public Map<ProjectId, MavenProjectWrapper> getModuleProjects() {
    MavenProject main = buildingResult.getProject();
    List<String> modules = main.getModules();
    Map<ProjectId, MavenProjectWrapper> modulesMap = new HashMap<>();
    for (String module : modules) {
      // Resul todo: Handle relative sub-module paths:
      // https://maven.apache.org/xsd/maven-4.0.0.xsd
      URI moduleURI = projectBase.resolve(module + "/");
      MavenProjectWrapper wrapper = MavenProjectWrapper.fromBase(moduleURI);
      MavenProject project = wrapper.getProject();
      ProjectId projectId = new ProjectId(
          project.getGroupId(),
          project.getArtifactId(),
          project.getVersion()
      );
      modulesMap.put(projectId, wrapper);
    }
    return modulesMap;
  }

  public MavenProject getProject() {
    return buildingResult.getProject();
  }
}
