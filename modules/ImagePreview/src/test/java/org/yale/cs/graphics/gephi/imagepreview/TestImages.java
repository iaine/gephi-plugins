/*
 * Test support utilities for the Image Preview plugin.
 *
 * Licensed under the GNU General Public License Version 3.
 */
package org.yale.cs.graphics.gephi.imagepreview;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Small helpers shared by the plugin tests.
 * <p>
 * Tests avoid committing binary image assets to the repository: instead a
 * throwaway PNG is generated on disk inside a JUnit-managed temporary folder.
 */
final class TestImages {

    private TestImages() {
    }

    /**
     * Writes a solid-colour PNG of the given size into {@code dir} and returns
     * the created file.
     *
     * @param dir      target directory (must exist)
     * @param fileName name of the file to create, e.g. {@code "node.png"}
     * @param width    image width in pixels
     * @param height   image height in pixels
     * @return the file that was written
     * @throws IOException if the image cannot be written
     */
    static File writePng(File dir, String fileName, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(10, 120, 200));
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        File out = new File(dir, fileName);
        ImageIO.write(img, "png", out);
        return out;
    }
}
