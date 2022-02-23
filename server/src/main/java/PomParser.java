import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Profile;
import org.apache.maven.project.*;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.*;
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
import java.util.Locale;
import java.util.Properties;

public final class PomParser {
    private static final CharMatcher LOWER_ALPHA_NUMERIC =
            CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('0', '9'));

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
        try {
            PlexusContainer container = new DefaultPlexusContainer(containerConfiguration);

            MavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();
            ProjectBuildingRequest projectBuildingRequest =
                    mavenExecutionRequest.getProjectBuildingRequest();
            projectBuildingRequest.setRepositorySession(session);
            projectBuildingRequest.setResolveDependencies(true);

            // Profile activation needs properties such as JDK version
            Properties properties = new Properties(); // allowing duplicate entries
            properties.putAll(projectBuildingRequest.getSystemProperties());
            properties.putAll(detectOsProperties());
            properties.putAll(System.getProperties());
            projectBuildingRequest.setSystemProperties(properties);

            ProjectBuilder projectBuilder = container.lookup(ProjectBuilder.class);

            return projectBuilder.build(pomFile, projectBuildingRequest);
        } catch (PlexusContainerException | ComponentLookupException | ProjectBuildingException ex) {
            throw new IllegalStateException(ex);
        }
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

    public static ImmutableMap<String, String> detectOsProperties() {
        return ImmutableMap.of(
                "os.detected.name",
                osDetectedName(),
                "os.detected.arch",
                osDetectedArch(),
                "os.detected.classifier",
                osDetectedName() + "-" + osDetectedArch());
    }

    private static String osDetectedName() {
        String osNameNormalized =
                LOWER_ALPHA_NUMERIC.retainFrom(System.getProperty("os.name").toLowerCase(Locale.ENGLISH));

        if (osNameNormalized.startsWith("macosx") || osNameNormalized.startsWith("osx")) {
            return "osx";
        } else if (osNameNormalized.startsWith("windows")) {
            return "windows";
        }
        // Since we only load the dependency graph, not actually use the
        // dependency, it doesn't matter a great deal which one we pick.
        return "linux";
    }

    private static String osDetectedArch() {
        String osArchNormalized =
                LOWER_ALPHA_NUMERIC.retainFrom(System.getProperty("os.arch").toLowerCase(Locale.ENGLISH));
        switch (osArchNormalized) {
            case "x8664":
            case "amd64":
            case "ia32e":
            case "em64t":
            case "x64":
                return "x86_64";
            default:
                return "x86_32";
        }
    }
}