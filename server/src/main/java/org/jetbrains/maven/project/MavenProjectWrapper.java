package org.jetbrains.maven.project;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import lombok.Value;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingResult;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class MavenProjectWrapper {

    private final ProjectBuildingResult buildingResult;

    public static MavenProjectWrapper fromBase(URI projectBase) {
        File base = new File(projectBase.getPath());
        ProjectBuildingResult result = PomParser.buildMavenProject(base);
        return new MavenProjectWrapper(result);
    }

    public List<BuildTargetIdentifier> getDependencies() {
        MavenProject project = getProject();
        return project.getDependencies()
                .stream()
                .map(Dependency::getManagementKey)
                .map(BuildTargetIdentifier::new)
                .collect(Collectors.toList());
    }

    public MavenProject getProject() {
        return buildingResult.getProject();
    }
}
