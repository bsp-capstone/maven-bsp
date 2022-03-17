package maven.project;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import lombok.Value;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingResult;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class MavenProjectWrapper {

    private final ProjectBuildingResult buildingResult;
    private final MavenProject project;

    public static MavenProjectWrapper fromBase(File projectBase) {
        ProjectBuildingResult result = PomParser.buildMavenProject(projectBase);
        return new MavenProjectWrapper(result, result.getProject());
    }

    public List<BuildTargetIdentifier> getDependencies() {
        return project.getDependencies()
                .stream()
                .map(dependency -> new BuildTargetIdentifier(dependency.getManagementKey()))
                .collect(Collectors.toList());
    }
}
