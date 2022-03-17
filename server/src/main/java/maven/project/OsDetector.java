package maven.project;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;

import java.util.Locale;

/**
 * Util class to detect and return OS properties
 */
public final class OsDetector {
    private static final CharMatcher LOWER_ALPHA_NUMERIC =
            CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('0', '9'));

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
