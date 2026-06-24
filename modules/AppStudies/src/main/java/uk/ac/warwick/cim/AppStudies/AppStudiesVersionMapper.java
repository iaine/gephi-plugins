package uk.ac.warwick.cim.AppStudies;

/**
 * Maps a semantic version string (e.g. "43.7.3") to a single, monotonically
 * increasing double so it can be used as the Gephi Timeline axis.
 *
 * There is no perfect encoding (it cannot tell "1.0" from "1.0.0", and a
 * pre-release suffix like "1.0.0-beta" collapses to "1.0.0"), but it preserves
 * ordering for the version shapes the AppStudies toolkit emits, which is all the
 * Timeline needs.
 *
 * @author iain
 */
public final class AppStudiesVersionMapper {

    private AppStudiesVersionMapper() {
    }

    public static double toDouble(String version) {
        if (version == null || version.isEmpty()) {
            return 0;
        }

        String[] parts = version.split("\\.");

        int major = parts.length > 0 ? parse(parts[0]) : 0;
        int minor = parts.length > 1 ? parse(parts[1]) : 0;
        int patch = parts.length > 2 ? parse(parts[2]) : 0;

        return major * 1_000_000d + minor * 1_000d + patch;
    }

    private static int parse(String s) {
        String cleaned = s.replaceAll("[^0-9].*$", "");
        return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
    }
}
