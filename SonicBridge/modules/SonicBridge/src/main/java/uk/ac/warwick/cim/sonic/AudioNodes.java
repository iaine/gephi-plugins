/*
 * SonicBridge - preview renderer for audio nodes.
 *
 * Draws a cassette icon at each node that has a linked audio file. Adapted from
 * the Gephi Image Preview plugin (Yale Computer Graphics Group, GPLv3), but the
 * bitmap is a fixed cassette glyph rather than a per-node image, since audio has
 * no visual form. Playback is handled by PlayAudioTool, not here.
 */
package uk.ac.warwick.cim.sonic;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import java.awt.geom.AffineTransform;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.preview.api.*;
import org.gephi.preview.plugin.items.NodeItem;
import org.gephi.preview.plugin.renderers.NodeRenderer;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.Renderer;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.w3c.dom.Element;
import processing.core.PImage;

/**
 * A renderer that draws a cassette icon on every audio-linked node.
 *
 * @author Iain Emsley (SonicBridge), after Yale Computer Graphics Group
 */
@ServiceProvider(service = Renderer.class, position = 200)
public class AudioNodes implements Renderer {

    final static String ENABLE = "ImageNodes.property.enable";
    final static String IMAGE_OPACITY = "ImageNodes.property.opacity";
    final static String IMAGE_SCALE = "ImageNodes.property.scale";
    final static String CATEGORY_NODE_IMAGE = "Node Images";

    private static final Logger logger = Logger.getLogger(AudioNodes.class.getName());

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(AudioNodes.class, "ImageNodes.name");
    }

    @Override
    public void render(Item item, RenderTarget target, PreviewProperties properties) {
        if (!(item instanceof AudioItem)) {
            return;
        }
        if (!showNodes(properties)) {
            return;
        }
        if (target instanceof G2DTarget) {
            renderImageProcessing((AudioItem) item, (G2DTarget) target, properties);
        } else if (target instanceof SVGTarget) {
            renderImageSVG((AudioItem) item, (SVGTarget) target, properties);
        } else if (target instanceof PDFTarget) {
            renderImagePDF((AudioItem) item, (PDFTarget) target, properties);
        }
    }

    public void renderImageProcessing(AudioItem item, G2DTarget target, PreviewProperties properties) {
        PImage image = item.renderProcessing(target);
        if (image == null) {
            logger.log(Level.WARNING, "Unable to load cassette icon for: {0}", item.getSource());
            return;
        }

        Float x = item.getData(NodeItem.X);
        Float y = item.getData(NodeItem.Y);
        Float size = item.getData(NodeItem.SIZE);

        int alpha = (int) ((properties.getFloatValue(IMAGE_OPACITY) / 100f) * 255f);
        if (alpha > 255) {
            alpha = 255;
        }

        // Preserve aspect ratio, scaled by node size and the Image Scale property.
        int aspectRatio = size.intValue() * image.width / image.height;
        int newHeight = (int) (aspectRatio * properties.getFloatValue(IMAGE_SCALE));
        int newWidth = (int) (size.intValue() * properties.getFloatValue(IMAGE_SCALE));
        image.resize(newHeight, newWidth);

        target.getGraphics().drawImage(image.getImage(),
                AffineTransform.getTranslateInstance(x - (image.width / 2), y - (image.height / 2)),
                null);
    }

    public void renderImagePDF(AudioItem item, PDFTarget target, PreviewProperties properties) {
        Image image = item.renderPDF();
        if (image == null) {
            logger.log(Level.WARNING, "Unable to load cassette icon for: {0}", item.getSource());
            return;
        }

        Float x = item.getData(NodeItem.X);
        Float y = item.getData(NodeItem.Y);
        Float size = item.getData(NodeItem.SIZE);

        float alpha = properties.getFloatValue(IMAGE_OPACITY) / 100f;

        PdfContentByte cb = target.getContentByte();
        if (alpha < 1f) {
            cb.saveState();
            PdfGState gState = new PdfGState();
            gState.setFillOpacity(alpha);
            gState.setStrokeOpacity(alpha);
            cb.setGState(gState);
        }

        image.setAbsolutePosition(x - size / 2, -y - size / 2);
        image.scaleToFit(size, size);
        try {
            cb.addImage(image);
        } catch (DocumentException ex) {
            logger.log(Level.SEVERE, "Unable to add cassette icon to PDF: " + item.getSource(), ex);
        }

        if (alpha < 1f) {
            cb.restoreState();
        }
    }

    public void renderImageSVG(AudioItem item, SVGTarget target, PreviewProperties properties) {
        Float x = item.getData(NodeItem.X);
        Float y = item.getData(NodeItem.Y);
        Float size = item.getData(NodeItem.SIZE);

        float alpha = properties.getFloatValue(IMAGE_OPACITY) / 100f;
        if (alpha > 1) {
            alpha = 1;
        }

        Element nodeElem = target.createElement("image");
        nodeElem.setAttribute("class", "node");
        nodeElem.setAttribute("xlink:href", item.renderSVG());
        nodeElem.setAttribute("x", "" + (x - size / 2));
        nodeElem.setAttribute("y", "" + (y - size / 2));
        nodeElem.setAttribute("width", size.toString());
        nodeElem.setAttribute("height", size.toString());
        nodeElem.setAttribute("style", "opacity: " + alpha);

        target.getTopElement(SVGTarget.TOP_NODES).appendChild(nodeElem);
    }

    @Override
    public void preProcess(PreviewModel previewModel) {
        // No directory needed: the cassette icon ships in plugin resources.
    }

    @Override
    public PreviewProperty[] getProperties() {
        return new PreviewProperty[]{
            PreviewProperty.createProperty(this, ENABLE, Boolean.class,
            NbBundle.getMessage(AudioNodes.class, "ImageNodes.property.enable.name"),
            NbBundle.getMessage(AudioNodes.class, "ImageNodes.property.enable.description"),
            CATEGORY_NODE_IMAGE).setValue(false),
            PreviewProperty.createProperty(this, IMAGE_OPACITY, Float.class,
            NbBundle.getMessage(NodeRenderer.class, "NodeRenderer.property.opacity.displayName"),
            NbBundle.getMessage(NodeRenderer.class, "NodeRenderer.property.opacity.description"),
            CATEGORY_NODE_IMAGE, ENABLE).setValue(100f),
            PreviewProperty.createProperty(this, IMAGE_SCALE, Float.class,
            NbBundle.getMessage(AudioNodes.class, IMAGE_SCALE + ".name"),
            NbBundle.getMessage(AudioNodes.class, IMAGE_SCALE + ".description"),
            CATEGORY_NODE_IMAGE, ENABLE).setValue(1f)};
    }

    private boolean showNodes(PreviewProperties properties) {
        return properties.getFloatValue(IMAGE_OPACITY) > 0
                && properties.getBooleanValue(ENABLE);
    }

    @Override
    public boolean isRendererForitem(Item item, PreviewProperties properties) {
        if (!(item instanceof AudioItem)) {
            return false;
        }
        return showNodes(properties)
                && item.getSource() != null
                && item.getSource() instanceof String
                && !((String) item.getSource()).isEmpty();
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return itemBuilder instanceof sonicBridge && showNodes(properties);
    }

    @Override
    public CanvasSize getCanvasSize(Item item, PreviewProperties pp) {
        return new CanvasSize();
    }

    @Override
    public void postProcess(PreviewModel pm, RenderTarget rt, PreviewProperties pp) {
        // Nothing to clean up after rendering.
    }
}
