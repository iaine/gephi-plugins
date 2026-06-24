/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.AppStudies;

/**
 * Map version data to a timeline. 
 * 
 * There's no lovely way of doing this. 
 * @author iain
 */

public final class AppStudiesVersionMapper {

    public static double toDouble(String version) {
        if (version == null || version.isEmpty()) return 0;

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
