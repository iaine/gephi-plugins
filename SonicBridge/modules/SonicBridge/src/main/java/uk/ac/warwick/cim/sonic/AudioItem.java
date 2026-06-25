/*
 * SonicBridge - preview item for an audio-linked node.
 *
 * Unlike the Image Preview plugin (which loads each file as a bitmap), an
 * audio file cannot be drawn directly. Instead every AudioItem renders a shared
 * "cassette" icon, while the actual audio path is kept so PlayAudioTool can
 * play it. The icon bitmap is loaded once from plugin resources and cached.
 */
package uk.ac.warwick.cim.sonic;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.gephi.preview.api.G2DTarget;
import org.gephi.preview.plugin.items.AbstractItem;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * An item that represents an audio file linked to a node.
 * <p>
 * The {@code source} string is the audio filename (relative to the configured
 * audio directory, or absolute). Rendering draws a cassette glyph rather than
 * the audio bytes; playback is handled separately by {@link PlayAudioTool} /
 * {@link AudioPlayer}.
 *
 * @author Iain Emsley (SonicBridge)
 */
public class AudioItem extends AbstractItem {

    public static final String AUDIO = "Audio";
    public static final String PROCESSING_DATA = "Processing_Data";
    public static final String PDF_DATA = "PDF_Data";

    /** Classpath location of the cassette icon used for every audio node. */
    private static final String ICON_RESOURCE =
            "/uk/ac/warwick/cim/sonic/resources/cassette.png";

    private static final Logger logger = Logger.getLogger(AudioItem.class.getName());

    // Cassette icon shared across all items, loaded lazily once.
    private static BufferedImage cachedIcon;
    private static boolean iconLoadAttempted;

    /**
     * @param source the audio filename this node links to
     */
    public AudioItem(String source) {
        super(source, AUDIO);
    }

    /** Load (once) and return the cassette icon, or null if unavailable. */
    private static synchronized BufferedImage loadIcon() {
        if (cachedIcon == null && !iconLoadAttempted) {
            iconLoadAttempted = true;
            URL url = AudioItem.class.getResource(ICON_RESOURCE);
            if (url != null) {
                try {
                    cachedIcon = ImageIO.read(url);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to load cassette icon", e);
                }
            } else {
                logger.log(Level.WARNING, "Cassette icon resource not found: {0}", ICON_RESOURCE);
            }
        }
        return cachedIcon;
    }

    /**
     * Prepare the cassette icon as a Processing image, caching the result on
     * this item for fast re-renders.
     *
     * @param target the current render target (unused but kept for parity).
     * @return a Processing image of the cassette, or null if unavailable.
     */
    public PImage renderProcessing(G2DTarget target) {
        PImage image = (PImage) data.get(PROCESSING_DATA);
        if (image == null) {
            BufferedImage icon = loadIcon();
            if (icon == null) {
                return null;
            }
            image = new PImage(icon.getWidth(), icon.getHeight(), PConstants.ARGB);
            icon.getRGB(0, 0, image.width, image.height, image.pixels, 0, image.width);
            image.updatePixels();
            data.put(PROCESSING_DATA, image);
        }
        return image;
    }

    /**
     * @return classpath URL string of the cassette icon for SVG embedding.
     */
    public String renderSVG() {
        URL url = AudioItem.class.getResource(ICON_RESOURCE);
        return url != null ? url.toString() : "";
    }

    /**
     * Prepare the cassette icon as an iText PDF image, cached on this item.
     *
     * @return an iText image of the cassette, or null if unavailable.
     */
    public com.itextpdf.text.Image renderPDF() {
        com.itextpdf.text.Image image = (com.itextpdf.text.Image) data.get(PDF_DATA);
        if (image == null) {
            URL url = AudioItem.class.getResource(ICON_RESOURCE);
            if (url == null) {
                return null;
            }
            try {
                image = Image.getInstance(url);
            } catch (BadElementException | MalformedURLException ex) {
                logger.log(Level.SEVERE, "Unable to load cassette icon for PDF", ex);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Unable to load cassette icon for PDF", ex);
            }
            if (image == null) {
                return null;
            }
            data.put(PDF_DATA, image);
        }
        return image;
    }
}
