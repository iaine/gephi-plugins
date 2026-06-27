/*
Copyright 2012 Yale Computer Graphics Group
Authors : Yitzchak Lockerman
Website : http://graphics.cs.yale.edu/

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright 2012 Yale Computer Graphics Group. All rights reserved.

The contents of this file are subject to the terms of the GNU
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.gephi.preview.plugin.items.AbstractItem;

/**
 * An Item that represents an image within the file system.
 * <p>
 * Each instance of <code>ImageItem</code> represents a single image file. When
 * the file is loaded the image data is cached in memory, allowing for faster
 * rendering in the future.
 * <p>
 * Since Gephi 0.10 the preview pipeline uses Java2D ({@code G2DTarget}) for the
 * on-screen / raster output and Apache PDFBox for PDF export. Both consume a
 * standard {@link BufferedImage}, so a single cached BufferedImage now serves
 * every target (the old Processing {@code PImage} path has been removed).
 *
 * @author Yitzchak Lockerman
 */
public class ImageItem extends AbstractItem {

    /**
     * The identifier (type) of this item.
     */
    public static final String IMAGE = "Image";

    /**
     * The data key for accessing the cached {@link BufferedImage}.
     */
    public static final String IMAGE_DATA = "Image_Data";

    private static final Logger LOGGER = Logger.getLogger(ImageItem.class.getName());

    /**
     * @param source The image filename
     */
    public ImageItem(String source) {
        super(source, IMAGE);
    }

    /**
     * Loads the image for this item, using the in-memory cache if available.
     * If the image has to be read from disk it is cached for subsequent calls.
     *
     * @param directory The folder to load images from.
     * @return A {@link BufferedImage} for this item, or {@code null} if it
     *         could not be loaded.
     */
    public BufferedImage getImage(File directory) {
        BufferedImage image = (BufferedImage) data.get(IMAGE_DATA);
        if (image == null) {
            if (source instanceof String) {
                File fullFile = new File(directory, (String) source);
                try {
                    image = ImageIO.read(fullFile);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Unable to load image: " + fullFile, e);
                }
            }

            if (image == null) {
                LOGGER.log(Level.WARNING, "Unable to load image: {0}", source);
                return null;
            }

            data.put(IMAGE_DATA, image);
        }

        return image;
    }

    /**
     * @param directory The folder to load images from.
     * @return The {@code xlink:href} value to be added to the SVG element to
     *         represent this image.
     */
    public String renderSVG(File directory) {
        return "file://" + new File(directory, (String) this.getSource()).getAbsolutePath();
    }
}
