package uk.ac.warwick.cim.AppStudies;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Maps the doubles produced by {@link AppStudiesVersionMapper} back to their
 * original human-readable version strings, and keeps them sorted so a UI can
 * render ticks and find the nearest version for any slider position.
 *
 * @author iain
 */
public class AppStudiesVersionIndex {

    private final Map<Double, String> valueToVersion = new HashMap<>();
    private final NavigableMap<Double, String> sorted = new TreeMap<>();

    public void add(String version) {
        double value = AppStudiesVersionMapper.toDouble(version);
        valueToVersion.put(value, version);
        sorted.put(value, version);
    }

    /** Exact label if known, otherwise the nearest version at or below value. */
    public String getLabel(double value) {
        if (valueToVersion.containsKey(value)) {
            return valueToVersion.get(value);
        }
        Map.Entry<Double, String> floor = sorted.floorEntry(value);
        return floor != null ? floor.getValue() : "";
    }

    public Set<Double> getValues() {
        return sorted.keySet();
    }

    public boolean isEmpty() {
        return sorted.isEmpty();
    }

    public void clear() {
        valueToVersion.clear();
        sorted.clear();
    }
}
