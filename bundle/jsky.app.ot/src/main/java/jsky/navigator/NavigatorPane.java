package jsky.navigator;

import diva.canvas.GraphicsPane;
import diva.canvas.CanvasLayer;
import jsky.catalog.gui.TablePlotter;

/**
 * A Diva GraphicsPane with a layer added for plotting catalog symbols.
 */
public class NavigatorPane extends GraphicsPane {

    /** A layer on which to draw catalog symbols */
    private final SymbolLayer _symbolLayer = new SymbolLayer();

    /**
     * Initialize a new NavigatorPane, which is a Diva GraphicsPane with a layer added
     * for catalog symbols.
     */
    public NavigatorPane() {
        _initNewLayer(_symbolLayer);
        _rebuildLayerArray();
    }

    /**
     * Return the layer to use to draw teh catalog symbols.
     */
    public SymbolLayer getSymbolLayer() {
        return _symbolLayer;
    }

    /** Set the object used to draw catalog symbols */
    public void setPlotter(TablePlotter plotter) {
        _symbolLayer.setPlotter(plotter);

    }

    /**
     * Rebuild the array of layers for use by iterators.
     * Override superclass to include the new layer.
     */
    @Override
    protected void _rebuildLayerArray() {
        _layers = new CanvasLayer[] {
                _foregroundEventLayer,
                _symbolLayer,
                _overlayLayer,
                _foregroundLayer,
                _backgroundLayer,
                _backgroundEventLayer};
    }
}

