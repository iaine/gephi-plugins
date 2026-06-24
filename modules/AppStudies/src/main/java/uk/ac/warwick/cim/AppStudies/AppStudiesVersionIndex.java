/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.AppStudies;

import java.util.*;

public class AppStudiesVersionIndex {

    private final Map<Double, String> valueToVersion = new HashMap<>();
    private final NavigableMap<Double, String> sorted = new TreeMap<>();

    public void add(String version) {
        double value = AppStudiesVersionMapper.toDouble(version);
        valueToVersion.put(value, version);
        sorted.put(value, version);
    }

    public String getLabel(double value) {
        // Exact match
        if (valueToVersion.containsKey(value)) {
            return valueToVersion.get(value);
        }

        // Nearest match (for slider positions)
        Map.Entry<Double, String> floor = sorted.floorEntry(value);
        return floor != null ? floor.getValue() : "";
    }

    public Set<Double> getValues() {
        return sorted.keySet();
    }
}