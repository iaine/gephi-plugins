/*
 * Licensed under the GNU General Public License Version 3.
 */
package org.yale.cs.graphics.gephi.imagepreview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link ImageItem}.
 * <p>
 * The actual drawing paths need a live render target (Java2D {@code G2DTarget},
 * Batik SVG, or a PDFBox content stream) and are covered by the in-Gephi smoke
 * test. What we verify cheaply here is image loading, caching and the SVG href
 * construction. A throwaway PNG is generated into a JUnit {@code TemporaryFolder}
 * so no binary assets are committed.
 */
public class ImageItemTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void getImage_loadsBufferedImageFromDisk() throws Exception {
        File dir = tmp.newFolder("images");
        TestImages.writePng(dir, "node.png", 16, 24);

        ImageItem item = new ImageItem("node.png");
        BufferedImage image = item.getImage(dir);

        assertNotNull("Image should load from disk", image);
        assertEquals(16, image.getWidth());
        assertEquals(24, image.getHeight());
    }

    @Test
    public void getImage_cachesResult() throws Exception {
        File dir = tmp.newFolder("images");
        TestImages.writePng(dir, "node.png", 8, 8);

        ImageItem item = new ImageItem("node.png");
        BufferedImage first = item.getImage(dir);
        BufferedImage second = item.getImage(dir);

        assertNotNull(first);
        assertSame("Second call should return the cached instance", first, second);
        assertSame("Cache should be exposed under IMAGE_DATA",
            first, item.getData(ImageItem.IMAGE_DATA));
    }

    @Test
    public void getImage_missingFile_returnsNull() throws Exception {
        File dir = tmp.newFolder("images");
        // no file written

        ImageItem item = new ImageItem("does-not-exist.png");
        assertNull(item.getImage(dir));
    }

    @Test
    public void renderSVG_buildsAbsoluteFileUri() throws Exception {
        File dir = tmp.newFolder("images");
        File png = TestImages.writePng(dir, "node.png", 4, 4);

        ImageItem item = new ImageItem("node.png");
        String href = item.renderSVG(dir);

        assertTrue("href should be a file:// URI", href.startsWith("file://"));
        assertTrue("href should point at the absolute path of the image",
            href.endsWith(png.getAbsolutePath()));
    }

    @Test
    public void source_isExposed() {
        ImageItem item = new ImageItem("a/b/node.png");
        assertEquals("a/b/node.png", item.getSource());
    }
}
