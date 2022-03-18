package org.jetbrains.maven.project;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class PomParser {

    public static ProjectBuildingResult buildMavenProject(File pomFile) {
        // MavenCli's way to instantiate PlexusContainer
        RepositorySystemSession session = createDefaultRepositorySystemSession(newRepositorySystem());
        ClassWorld classWorld =
                new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
        ContainerConfiguration containerConfiguration =
                new DefaultContainerConfiguration()
                        .setClassWorld(classWorld)
                        .setRealm(classWorld.getClassRealm("plexus.core"))
                        .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                        .setAutoWiring(true)
                        .setJSR250Lifecycle(true)
                        .setName("pom-reader");
        return getBuildResult(containerConfiguration, session, pomFile);
    }

    private static ProjectBuildingResult getBuildResult(ContainerConfiguration containerConfiguration,
                                                        RepositorySystemSession session,
                                                        File pomFile) {
        try {
            PlexusContainer container = new DefaultPlexusContainer(containerConfiguration);
            ProjectBuildingRequest projectBuildingRequest = newProjectBuildingRequest(session);
            ProjectBuilder projectBuilder = container.lookup(ProjectBuilder.class);
            return projectBuilder.build(pomFile, projectBuildingRequest);
        } catch (PlexusContainerException | ComponentLookupException | ProjectBuildingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static ProjectBuildingRequest newProjectBuildingRequest(RepositorySystemSession session) {
        MavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();
        ProjectBuildingRequest projectBuildingRequest =
                mavenExecutionRequest.getProjectBuildingRequest();
        projectBuildingRequest.setRepositorySession(session);
        projectBuildingRequest.setResolveDependencies(true);

        // Profile activation needs properties such as JDK version
        Properties properties = new Properties(); // allowing duplicate entries
        properties.putAll(projectBuildingRequest.getSystemProperties());
        properties.putAll(OsDetector.detectOsProperties());
        properties.putAll(System.getProperties());
        projectBuildingRequest.setSystemProperties(properties);
        return projectBuildingRequest;
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    private static DefaultRepositorySystemSession createDefaultRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepository = new LocalRepository(findLocalRepository());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
        return session;
    }

    private static String findLocalRepository() {
        Path home = Paths.get(System.getProperty("user.home"));
        Path localRepo = home.resolve(".m2").resolve("repository");
        if (Files.isDirectory(localRepo)) {
            return localRepo.toAbsolutePath().toString();
        } else {
            return makeTemporaryLocalRepository();
        }
    }

    private static String makeTemporaryLocalRepository() {
        try {
            File temporaryDirectory = Files.createTempDirectory("m2").toFile();
            temporaryDirectory.deleteOnExit();
            return temporaryDirectory.getAbsolutePath();
        } catch (IOException ex) {
            return null;
        }
    }
}
