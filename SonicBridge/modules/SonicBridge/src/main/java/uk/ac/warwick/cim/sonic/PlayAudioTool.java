/*
 * SonicBridge - "Play Audio" visualization tool.
 *
 * Adds a cassette button to the graph-window toolbar. While the tool is
 * selected, clicking a node resolves that node's "audio" attribute against the
 * configured audio directory and plays it on a background thread via
 * AudioPlayer. Clicking empty space stops playback.
 *
 * The Preview API renders a static image and has no click model, so node-click
 * interaction lives here in the Tools SPI (the same mechanism Gephi's own
 * Edit / HeatMap tools use).
 */
package uk.ac.warwick.cim.sonic;

import java.io.File;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.gephi.graph.api.Node;
import org.gephi.tools.spi.NodeClickEventListener;
import org.gephi.tools.spi.Tool;
import org.gephi.tools.spi.ToolEventListener;
import org.gephi.tools.spi.ToolSelectionType;
import org.gephi.tools.spi.ToolUI;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * A visualization tool that plays the audio linked to a clicked node.
 *
 * @author Iain Emsley (SonicBridge)
 */
@ServiceProvider(service = Tool.class)
public class PlayAudioTool implements Tool {

    /** The node attribute holding the (relative or absolute) audio filename. */
    static final String AUDIO_ATTRIBUTE = "audio";

    /**
     * Preference key shared with AudioNodes so the tool resolves the same
     * directory the renderer draws from.
     */
    static final String AUDIO_DIRECTORY_PREF = "ImageNodes.imageDirectory";

    private final AudioPlayer player = AudioPlayer.getInstance();

    @Override
    public void select() {
        // nothing to set up; listeners are wired in getListeners()
    }

    @Override
    public void unselect() {
        // stop any clip when the user switches away from this tool
        player.stop();
    }

    @Override
    public ToolEventListener[] getListeners() {
        return new ToolEventListener[]{
            new NodeClickEventListener() {
                @Override
                public void clickNodes(Node[] nodes) {
                    if (nodes == null || nodes.length == 0) {
                        player.stop();
                        return;
                    }
                    playNode(nodes[0]);
                }
            }
        };
    }

    /**
     * Resolve a node's audio file and hand it to the background player.
     */
    private void playNode(Node node) {
        Object attr = node.getAttribute(AUDIO_ATTRIBUTE);
        if (attr == null) {
            return;
        }
        String name = attr.toString().trim();
        if (name.isEmpty()) {
            return;
        }

        File file = new File(name);
        if (!file.isAbsolute()) {
            // relative names resolve against the configured audio directory,
            // matching how AudioNodes/AudioItem load the cassette icon target.
            String dir = NbPreferences.forModule(sonicBridge.class)
                    .get(AUDIO_DIRECTORY_PREF, null);
            if (dir != null && !dir.isEmpty()) {
                file = new File(dir, name);
            }
        }
        player.play(file);
    }

    @Override
    public ToolUI getUI() {
        return new ToolUI() {
            @Override
            public JPanel getPropertiesBar(Tool tool) {
                JPanel panel = new JPanel();
                panel.add(new JLabel(
                        NbBundle.getMessage(PlayAudioTool.class,
                                "PlayAudioTool.propertiesBar.hint")));
                return panel;
            }

            @Override
            public Icon getIcon() {
                // cassette icon bundled under src/main/resources
                java.net.URL url = PlayAudioTool.class.getResource(
                        "/uk/ac/warwick/cim/sonic/resources/cassette.png");
                return url != null ? new ImageIcon(url) : null;
            }

            @Override
            public String getName() {
                return NbBundle.getMessage(PlayAudioTool.class, "PlayAudioTool.name");
            }

            @Override
            public String getDescription() {
                return NbBundle.getMessage(PlayAudioTool.class, "PlayAudioTool.description");
            }

            @Override
            public int getPosition() {
                return 250;
            }
        };
    }

    @Override
    public ToolSelectionType getSelectionType() {
        return ToolSelectionType.SELECTION;
    }
}
