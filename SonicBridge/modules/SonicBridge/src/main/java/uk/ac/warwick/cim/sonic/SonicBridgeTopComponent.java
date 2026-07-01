/*
 * SonicBridge - docked control panel.
 *
 * A NetBeans TopComponent (dockable window) that lets users drive the plugin
 * from the UI instead of calling AudioImportController programmatically:
 *
 *   - "Link from folder...": pick an audio directory and match files to nodes.
 *   - "Link from CSV...": pick a CSV (node column + audio column) to map files.
 *   - Stop button: stops any clip currently playing.
 *   - Volume slider: sets the master playback volume (0..100%).
 *
 * Ingestion runs off the EDT (graphs can be large) and writes back the result
 * count on the EDT. The window appears under Window > SonicBridge and docks in
 * the Overview perspective.
 */
package uk.ac.warwick.cim.sonic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.windows.TopComponent;

/**
 * Dockable control panel for SonicBridge ingestion and playback.
 *
 * @author Iain Emsley (SonicBridge)
 */
@ConvertAsProperties(dtd = "-//uk.ac.warwick.cim.sonic//SonicBridge//EN", autostore = false)
@TopComponent.Description(
        preferredID = "SonicBridgeTopComponent",
        iconBase = "uk/ac/warwick/cim/sonic/resources/cassette-16.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "properties", openAtStartup = false, roles = {"overview"})
@ActionID(category = "Window", id = "uk.ac.warwick.cim.sonic.SonicBridgeTopComponent")
@ActionReference(path = "Menu/Window", position = 1200)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SonicBridgeAction",
        preferredID = "SonicBridgeTopComponent")
public final class SonicBridgeTopComponent extends TopComponent {

    private final AudioPlayer player = AudioPlayer.getInstance();
    private final JLabel status = new JLabel(" ");

    public SonicBridgeTopComponent() {
        setName(NbBundle.getMessage(SonicBridgeTopComponent.class, "CTL_SonicBridgeTopComponent"));
        setToolTipText(NbBundle.getMessage(SonicBridgeTopComponent.class, "HINT_SonicBridgeTopComponent"));
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Ingestion ---
        JPanel ingest = new JPanel(new GridLayout(0, 1, 4, 4));
        ingest.setBorder(BorderFactory.createTitledBorder(
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.section.link")));

        JButton folderButton = new JButton(
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.button.folder"));
        folderButton.addActionListener(e -> linkFromFolder());

        JButton csvButton = new JButton(
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.button.csv"));
        csvButton.addActionListener(e -> linkFromCsv());

        ingest.add(folderButton);
        ingest.add(csvButton);

        // --- Playback ---
        JPanel playback = new JPanel();
        playback.setLayout(new BoxLayout(playback, BoxLayout.Y_AXIS));
        playback.setBorder(BorderFactory.createTitledBorder(
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.section.playback")));

        JButton stopButton = new JButton(
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.button.stop"));
        stopButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        stopButton.addActionListener(e -> player.stop());

        JLabel volLabel = new JLabel(
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.label.volume"));
        volLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider volume = new JSlider(0, 100, Math.round(player.getVolume() * 100));
        volume.setAlignmentX(Component.LEFT_ALIGNMENT);
        volume.setMajorTickSpacing(25);
        volume.setPaintTicks(true);
        volume.setPaintLabels(true);
        volume.addChangeListener(e -> player.setVolume(volume.getValue() / 100f));

        playback.add(stopButton);
        playback.add(Box.createVerticalStrut(8));
        playback.add(volLabel);
        playback.add(volume);

        content.add(ingest);
        content.add(Box.createVerticalStrut(10));
        content.add(playback);
        content.add(Box.createVerticalStrut(10));

        status.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));

        add(content, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(260, 320));
    }

    /**
     * Resolve the current graph, or null (with a status message) if no
     * workspace/graph is available.
     */
    private Graph currentGraph() {
        GraphController gc = Lookup.getDefault().lookup(GraphController.class);
        if (gc == null) {
            return null;
        }
        GraphModel model = gc.getGraphModel();
        if (model == null) {
            setStatus(NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.status.noGraph"));
            return null;
        }
        return model.getGraph();
    }

    private void linkFromFolder() {
        final Graph graph = currentGraph();
        if (graph == null) {
            setStatus(NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.status.noGraph"));
            return;
        }

        JFileChooser chooser = new JFileChooser(lastDir());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.button.folder"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File dir = chooser.getSelectedFile();
        rememberLastDir(dir);

        setStatus(NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.status.working"));
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return AudioImportController.loadFromDirectory(graph, dir);
            }

            @Override
            protected void done() {
                reportLinked(this);
            }
        }.execute();
    }

    private void linkFromCsv() {
        final Graph graph = currentGraph();
        if (graph == null) {
            setStatus(NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.status.noGraph"));
            return;
        }

        JFileChooser chooser = new JFileChooser(lastDir());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv", "tsv", "txt"));
        chooser.setDialogTitle(
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.button.csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File csv = chooser.getSelectedFile();
        rememberLastDir(csv.getParentFile());

        // Ask which columns hold the node and the audio filename.
        final int nodeCol = promptColumn("SonicBridge.prompt.nodeColumn", 0);
        if (nodeCol < 0) {
            return;
        }
        final int audioCol = promptColumn("SonicBridge.prompt.audioColumn", 1);
        if (audioCol < 0) {
            return;
        }
        final boolean header = JOptionPane.showConfirmDialog(this,
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.prompt.header"),
                NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.button.csv"),
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

        setStatus(NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.status.working"));
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return AudioImportController.loadFromCsv(graph, csv, nodeCol, audioCol, header);
            }

            @Override
            protected void done() {
                reportLinked(this);
            }
        }.execute();
    }

    /** Prompt for a zero-based column index; returns -1 if cancelled/invalid. */
    private int promptColumn(String messageKey, int defaultValue) {
        String input = JOptionPane.showInputDialog(this,
                NbBundle.getMessage(SonicBridgeTopComponent.class, messageKey),
                String.valueOf(defaultValue));
        if (input == null) {
            return -1;
        }
        try {
            int v = Integer.parseInt(input.trim());
            return v < 0 ? -1 : v;
        } catch (NumberFormatException e) {
            setStatus(NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.status.badColumn"));
            return -1;
        }
    }

    /** Read the SwingWorker result and update the status label on the EDT. */
    private void reportLinked(SwingWorker<Integer, Void> worker) {
        int linked = 0;
        try {
            linked = worker.get();
        } catch (Exception ex) {
            setStatus(NbBundle.getMessage(SonicBridgeTopComponent.class, "SonicBridge.status.error"));
            return;
        }
        setStatus(NbBundle.getMessage(SonicBridgeTopComponent.class,
                "SonicBridge.status.linked", linked));
    }

    private void setStatus(final String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            status.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> status.setText(text));
        }
    }

    private File lastDir() {
        String path = NbPreferences.forModule(sonicBridge.class)
                .get(PlayAudioTool.AUDIO_DIRECTORY_PREF, null);
        return path != null ? new File(path) : null;
    }

    private void rememberLastDir(File dir) {
        if (dir != null) {
            NbPreferences.forModule(sonicBridge.class)
                    .put(PlayAudioTool.AUDIO_DIRECTORY_PREF, dir.getAbsolutePath());
        }
    }

    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
    }

    void writeProperties(java.util.Properties p) {
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        // no persisted UI state for now
    }
}
