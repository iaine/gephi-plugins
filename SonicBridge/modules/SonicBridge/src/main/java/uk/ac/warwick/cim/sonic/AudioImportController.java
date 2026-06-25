/*
 * SonicBridge - audio ingestion.
 *
 * Populates the "audio" node attribute (consumed by AudioItem/AudioNodes for
 * rendering and by PlayAudioTool for playback) from one of two sources:
 *
 *   1. A directory of audio files: each node whose Id or Label matches a file
 *      stem (e.g. node "bee" <- bee_bop.wav) gets that filename.
 *   2. A CSV with a node column and an audio column (e.g. bee,bee_bop.wav).
 *
 * The chosen audio directory is stored in the same NbPreferences key the
 * renderer and tool read, so all three stay in sync.
 */
package uk.ac.warwick.cim.sonic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.Table;
import org.openide.util.NbPreferences;

/**
 * Links audio files to graph nodes via the {@code audio} attribute.
 *
 * @author Iain Emsley (SonicBridge)
 */
public final class AudioImportController {

    private static final Logger LOGGER =
            Logger.getLogger(AudioImportController.class.getName());

    /** Recognised audio extensions for directory scanning. */
    private static final String[] AUDIO_EXTENSIONS =
            {".wav", ".aif", ".aiff", ".au", ".mp3", ".ogg", ".flac"};

    private AudioImportController() {
    }

    /**
     * Ensure the node table has an {@code audio} string column and return it.
     */
    private static Column ensureAudioColumn(Graph graph) {
        Table nodeTable = graph.getModel().getNodeTable();
        if (!nodeTable.hasColumn(PlayAudioTool.AUDIO_ATTRIBUTE)) {
            nodeTable.addColumn(PlayAudioTool.AUDIO_ATTRIBUTE, String.class);
        }
        return nodeTable.getColumn(PlayAudioTool.AUDIO_ATTRIBUTE);
    }

    /**
     * Remember the directory so the renderer and tool resolve relative
     * filenames against it.
     */
    private static void rememberDirectory(File directory) {
        NbPreferences.forModule(sonicBridge.class)
                .put(PlayAudioTool.AUDIO_DIRECTORY_PREF, directory.getAbsolutePath());
    }

    /**
     * Scan a directory of audio files and attach each one to the node whose
     * Id or Label matches the file stem (case-insensitive). For example a node
     * with label "bee" is linked to {@code bee_bop.wav} if the stem "bee_bop"
     * starts with "bee"... to keep matching strict, this uses exact stem
     * equality by default; adjust matchesNode() if you want fuzzier matching.
     *
     * @param graph     the graph to annotate
     * @param directory folder containing audio files
     * @return number of nodes linked
     */
    public static int loadFromDirectory(Graph graph, File directory) {
        if (directory == null || !directory.isDirectory()) {
            LOGGER.log(Level.WARNING, "Not a directory: {0}", directory);
            return 0;
        }
        rememberDirectory(directory);
        ensureAudioColumn(graph);

        // map of lowercase file-stem -> filename
        Map<String, String> stemToFile = new HashMap<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && isAudioFile(f.getName())) {
                    stemToFile.put(stem(f.getName()).toLowerCase(Locale.ROOT), f.getName());
                }
            }
        }

        int linked = 0;
        for (Node n : graph.getNodes()) {
            String key = nodeKey(n);
            if (key == null) {
                continue;
            }
            String fileName = stemToFile.get(key.toLowerCase(Locale.ROOT));
            if (fileName != null) {
                n.setAttribute(PlayAudioTool.AUDIO_ATTRIBUTE, fileName);
                linked++;
            }
        }
        LOGGER.log(Level.INFO, "Directory link: {0} nodes linked to audio", linked);
        return linked;
    }

    /**
     * Load a CSV mapping nodes to audio files.
     *
     * @param graph        the graph to annotate
     * @param csv          CSV file, optionally with a header row
     * @param nodeColumn   zero-based index of the node id/label column
     * @param audioColumn  zero-based index of the audio filename column
     * @param hasHeader    skip the first row if true
     * @return number of nodes linked
     */
    public static int loadFromCsv(Graph graph, File csv,
                                  int nodeColumn, int audioColumn,
                                  boolean hasHeader) {
        if (csv == null || !csv.isFile()) {
            LOGGER.log(Level.WARNING, "Not a file: {0}", csv);
            return 0;
        }
        ensureAudioColumn(graph);

        // Build node lookup by id and by label (lowercased).
        Map<String, Node> lookup = new HashMap<>();
        for (Node n : graph.getNodes()) {
            lookup.put(String.valueOf(n.getId()).toLowerCase(Locale.ROOT), n);
            if (n.getLabel() != null) {
                lookup.put(n.getLabel().toLowerCase(Locale.ROOT), n);
            }
        }

        int linked = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(csv))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first && hasHeader) {
                    first = false;
                    continue;
                }
                first = false;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] cols = splitCsv(line);
                if (cols.length <= Math.max(nodeColumn, audioColumn)) {
                    continue;
                }
                String nodeKey = cols[nodeColumn].trim().toLowerCase(Locale.ROOT);
                String audio = cols[audioColumn].trim();
                Node n = lookup.get(nodeKey);
                if (n != null && !audio.isEmpty()) {
                    n.setAttribute(PlayAudioTool.AUDIO_ATTRIBUTE, audio);
                    linked++;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed reading CSV " + csv, e);
        }
        LOGGER.log(Level.INFO, "CSV link: {0} nodes linked to audio", linked);
        return linked;
    }

    // ---- helpers -------------------------------------------------------

    private static String nodeKey(Node n) {
        if (n.getLabel() != null && !n.getLabel().isEmpty()) {
            return n.getLabel();
        }
        return n.getId() != null ? String.valueOf(n.getId()) : null;
    }

    private static boolean isAudioFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : AUDIO_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static String stem(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /**
     * Minimal CSV splitter handling simple double-quoted fields. For complex
     * CSVs swap in a real parser (e.g. opencsv) here.
     */
    private static String[] splitCsv(String line) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
