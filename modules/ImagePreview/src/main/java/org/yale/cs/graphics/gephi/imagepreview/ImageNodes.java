/*
Copyright 2012 Yale Computer Graphics Group
Authors : Yitzchak Lockerman
Website : http://graphics.cs.yale.edu/

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright 2012 Yale Computer Graphics Group. All rights reserved.

The contents of this file are subject to the terms of  the GNU
General Public License Version 3 only ("GPL" or "License").
You may not use this file except in compliance with the
License. You can obtain a copy of the License at /gpl-3.0.txt.
See the License for the specific language governing permissions and limitations
under the License.  When distributing the software, include this License Header
Notice in each file and include the License file at /gpl-3.0.txt.
If applicable, add the following below the License Header, with the fields
enclosed by brackets [] replaced by your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"

Contributor(s): Totetmatt (0.9.x transition), 0.11.x Java2D/PDFBox migration

This file is based on, and meant to be used with, Gephi. (http://gephi.org/)
*/

package org.yale.cs.graphics.gephi.imagepreview;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.gephi.preview.api.CanvasSize;
import org.gephi.preview.api.G2DTarget;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PDFTarget;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.api.SVGTarget;
import org.gephi.preview.plugin.items.NodeItem;
import org.gephi.preview.plugin.renderers.NodeRenderer;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.Renderer;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.w3c.dom.Element;

/**
 * A service that renders Nodes as images.
 * <p>
 * This class works in conjunction with {@link ImageItem} and
 * {@link NodeImageItemBuilder}.
 * <p>
 * Rendering targets in Gephi 0.10+ are Java2D ({@link G2DTarget}), SVG
 * ({@link SVGTarget}) and PDFBox ({@link PDFTarget}). The old Processing target
 * has been removed.
 *
 * @author Yitzchak Lockerman (Yale Computer Graphics Group)
 */
@ServiceProvider(service = Renderer.class, position = 200)
public class ImageNodes implements Renderer {

    static final String IMAGE_DESCRIPTION = "ImageNodes.property.imageDescription";
    static final String IMAGE_DIRECTORY = "ImageNodes.property.path";
    static final String IMAGE_OPACITY = "ImageNodes.property.opacity";
    static final String CATEGORY_NODE_IMAGE = "Node Images";

    private static final Logger LOGGER = Logger.getLogger(ImageNodes.class.getName());

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(ImageNodes.class, "ImageNodes.name");
    }

    @Override
    public void render(Item item, RenderTarget target, PreviewProperties properties) {
        if (!(item instanceof ImageItem)) {
            return;
        }

        String imagesPath = properties.getValue(IMAGE_DIRECTORY);
        if (imagesPath == null || imagesPath.isEmpty()) {
            return;
        }

        File imagesDir = new File(imagesPath);
        if (!imagesDir.exists() || !imagesDir.isDirectory()) {
            return;
        }

        if (showNodes(properties)) {
            if (target instanceof G2DTarget) {
                renderImageG2D((ImageItem) item, (G2DTarget) target, properties, imagesDir);
            } else if (target instanceof SVGTarget) {
                renderImageSVG((ImageItem) item, (SVGTarget) target, properties, imagesDir);
            } else if (target instanceof PDFTarget) {
                renderImagePDF((ImageItem) item, (PDFTarget) target, properties, imagesDir);
            }
        }
    }

    public void renderImageG2D(ImageItem item, G2DTarget target,
                               PreviewProperties properties, File directory) {
        BufferedImage image = item.getImage(directory);
        if (image == null) {
            LOGGER.log(Level.WARNING, "Unable to load image: {0}", item.getSource());
            return;
        }

        // Params
        Float x = item.getData(NodeItem.X);
        Float y = item.getData(NodeItem.Y);
        Float size = item.getData(NodeItem.SIZE);

        float alpha = properties.getFloatValue(IMAGE_OPACITY) / 100f;
        if (alpha > 1f) {
            alpha = 1f;
        }

        Graphics2D graphics = target.getGraphics();
        Composite oldComposite = graphics.getComposite();
        if (alpha < 1f) {
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        // Draw centred on (x, y), scaled to the node size.
        int drawX = Math.round(x - size / 2f);
        int drawY = Math.round(y - size / 2f);
        int drawSize = Math.round(size);
        graphics.drawImage(image, drawX, drawY, drawSize, drawSize, null);

        graphics.setComposite(oldComposite);
    }

    public void renderImagePDF(ImageItem item, PDFTarget target,
                               PreviewProperties properties, File directory) {

        BufferedImage bufferedImage = item.getImage(directory);
        if (bufferedImage == null) {
            LOGGER.log(Level.WARNING, "Unable to load image: {0}", item.getSource());
            return;
        }

        Float x = item.getData(NodeItem.X);
        Float y = item.getData(NodeItem.Y);
        Float size = item.getData(NodeItem.SIZE);

        float alpha = properties.getFloatValue(IMAGE_OPACITY) / 100f;
        if (alpha > 1f) {
            alpha = 1f;
        }

        // Gephi 0.10+ migrated the PDF target from iText to Apache PDFBox.
        // The PDDocument is published into the properties by the exporter.
        PDDocument document = properties.getValue(PDFTarget.PDF_DOCUMENT);
        PDPageContentStream cb = target.getContentStream();

        try {
            cb.saveGraphicsState();

            if (alpha < 1f) {
                PDExtendedGraphicsState gState = new PDExtendedGraphicsState();
                gState.setNonStrokingAlphaConstant(alpha);
                gState.setStrokingAlphaConstant(alpha);
                cb.setGraphicsStateParameters(gState);
            }

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);

            // PDFBox uses a bottom-left origin and drawImage() positions the
            // image by its bottom-left corner, so we offset by half the size to
            // keep the node image centred on (x, y). The y axis is flipped to
            // match the rest of the PDF export (see NodeRenderer, which draws at -y).
            cb.drawImage(pdImage, x - size / 2f, -y - size / 2f, size, size);

            cb.restoreGraphicsState();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to add image to document: " + item.getSource(), ex);
        }
    }

    public void renderImageSVG(ImageItem item, SVGTarget target,
                               PreviewProperties properties, File directory) {

        // Params
        Float x = item.getData(NodeItem.X);
        Float y = item.getData(NodeItem.Y);
        Float size = item.getData(NodeItem.SIZE);

        float alpha = properties.getFloatValue(IMAGE_OPACITY) / 100f;
        if (alpha > 1) {
            alpha = 1;
        }

        Element nodeElem = target.createElement("image");
        nodeElem.setAttribute("class", "node");
        nodeElem.setAttribute("xlink:href", item.renderSVG(directory));
        nodeElem.setAttribute("x", "" + (x - size / 2));
        nodeElem.setAttribute("y", "" + (y - size / 2));

        nodeElem.setAttribute("width", size.toString());
        nodeElem.setAttribute("height", size.toString());
        nodeElem.setAttribute("style", "opacity: " + alpha);

        target.getTopElement(SVGTarget.TOP_NODES).appendChild(nodeElem);
    }

    @Override
    public void preProcess(PreviewModel previewModel) {
    }

    @Override
    public void postProcess(PreviewModel previewModel, RenderTarget renderTarget,
                            PreviewProperties properties) {
    }

    @Override
    public CanvasSize getCanvasSize(Item item, PreviewProperties properties) {
        return new CanvasSize();
    }

    @Override
    public PreviewProperty[] getProperties() {
        return new PreviewProperty[]{
            PreviewProperty.createProperty(this, "ImageNodes.property.enable", Boolean.class,
                NbBundle.getMessage(ImageNodes.class, "ImageNodes.property.enable.name"),
                NbBundle.getMessage(ImageNodes.class, "ImageNodes.property.enable.description"),
                CATEGORY_NODE_IMAGE).setValue(false),
            PreviewProperty.createProperty(this, IMAGE_DIRECTORY, String.class,
                NbBundle.getMessage(ImageNodes.class, IMAGE_DIRECTORY + ".name"),
                NbBundle.getMessage(ImageNodes.class, IMAGE_DIRECTORY + ".description"),
                CATEGORY_NODE_IMAGE, "ImageNodes.property.enable").setValue(new File(".").getAbsolutePath()),
            PreviewProperty.createProperty(this, IMAGE_OPACITY, Float.class,
                NbBundle.getMessage(NodeRenderer.class, "NodeRenderer.property.opacity.displayName"),
                NbBundle.getMessage(NodeRenderer.class, "NodeRenderer.property.opacity.description"),
                CATEGORY_NODE_IMAGE, "ImageNodes.property.enable").setValue(100f)};
    }

    private boolean showNodes(PreviewProperties properties) {
        return properties.getFloatValue(IMAGE_OPACITY) > 0
                && properties.getBooleanValue("ImageNodes.property.enable");
    }

    @Override
    public boolean isRendererForitem(Item item, PreviewProperties properties) {
        if (!(item instanceof ImageItem)) {
            return false;
        }
        return showNodes(properties)
                && item.getSource() != null
                && item.getSource() instanceof String;
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return itemBuilder instanceof NodeImageItemBuilder && showNodes(properties);
    }
}
