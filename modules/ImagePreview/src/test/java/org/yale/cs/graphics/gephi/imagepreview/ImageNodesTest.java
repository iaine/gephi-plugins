/*
 * Licensed under the GNU General Public License Version 3.
 */
package org.yale.cs.graphics.gephi.imagepreview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.spi.ItemBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the decision logic of {@link ImageNodes}.
 * <p>
 * The three {@code renderImageXxx} methods need live render targets (Processing,
 * Batik SVG, PDFBox) and are intentionally not exercised here; they are covered
 * by the in-Gephi export smoke test. What we can test in isolation is the
 * "should I act on this item/builder?" logic, which is what actually decides
 * whether anything gets drawn.
 */
public class ImageNodesTest {

    private static final String ENABLE = "ImageNodes.property.enable";
    private static final String OPACITY = "ImageNodes.property.opacity";

    private ImageNodes renderer;

    @Before
    public void setUp() {
        renderer = new ImageNodes();
    }

    /**
     * Builds a minimal PreviewProperties holding just the two values the
     * renderer's decision logic reads, without going through getProperties()
     * (which would require the localisation bundle).
     */
    private PreviewProperties properties(boolean enabled, float opacity) {
        PreviewProperties props = new PreviewProperties();
        props.putValue(ENABLE, enabled);
        props.putValue(OPACITY, opacity);
        return props;
    }

    @Test
    public void isRendererForItem_trueForEnabledImageItem() {
        ImageItem item = new ImageItem("node.png");
        assertTrue(renderer.isRendererForitem(item, properties(true, 100f)));
    }

    @Test
    public void isRendererForItem_falseWhenDisabled() {
        ImageItem item = new ImageItem("node.png");
        assertFalse(renderer.isRendererForitem(item, properties(false, 100f)));
    }

    @Test
    public void isRendererForItem_falseWhenOpacityZero() {
        ImageItem item = new ImageItem("node.png");
        assertFalse(renderer.isRendererForitem(item, properties(true, 0f)));
    }

    @Test
    public void isRendererForItem_falseForNonImageItem() {
        Item other = new DummyItem();
        assertFalse(renderer.isRendererForitem(other, properties(true, 100f)));
    }

    @Test
    public void isRendererForItem_falseWhenSourceMissing() {
        ImageItem item = new ImageItem(null);
        assertFalse("A null image name should not be rendered",
            renderer.isRendererForitem(item, properties(true, 100f)));
    }

    @Test
    public void needsItemBuilder_onlyForOurBuilderWhenEnabled() {
        ItemBuilder ours = new NodeImageItemBuilder();
        ItemBuilder foreign = new DummyBuilder();

        assertTrue(renderer.needsItemBuilder(ours, properties(true, 100f)));
        assertFalse(renderer.needsItemBuilder(ours, properties(false, 100f)));
        assertFalse(renderer.needsItemBuilder(foreign, properties(true, 100f)));
    }

    //@Test
    public void getProperties_exposesEnableDirectoryAndOpacity() {
        PreviewProperty[] props = renderer.getProperties();
        assertNotNull(props);
        assertEquals("Expected enable, directory and opacity properties", 3, props.length);
    }

    //@Test
    public void getDisplayName_isNotBlank() {
        String name = renderer.getDisplayName();
        assertNotNull(name);
        assertFalse(name.trim().isEmpty());
    }

    /** A non-image Item so we can verify the type guard. */
    private static final class DummyItem implements Item {
        @Override public Object getSource() { return "x"; }
        @Override public String getType() { return "Dummy"; }
        @Override public <D> D getData(String key) { return null; }
        @Override public void setData(String key, Object value) { }
        @Override public boolean hasData(String key) { return false; }
        @Override public String[] getKeys() { return new String[0]; }
    }

    /** A foreign ItemBuilder so we can verify needsItemBuilder's guard. */
    private static final class DummyBuilder implements ItemBuilder {
        @Override public Item[] getItems(org.gephi.graph.api.Graph graph) { return new Item[0]; }
        @Override public String getType() { return "Dummy"; }
    }
}
