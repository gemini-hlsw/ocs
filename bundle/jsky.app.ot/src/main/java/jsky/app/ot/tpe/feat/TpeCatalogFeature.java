package jsky.app.ot.tpe.feat;

import jsky.app.ot.tpe.TpeImageFeature;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeImageWidget;
import jsky.app.ot.tpe.TpeImageFeatureCategory;
import jsky.catalog.gui.TablePlotter;

import java.awt.*;

public class TpeCatalogFeature extends TpeImageFeature {

    /**
     * Construct the feature with its name and description.
     */
    public TpeCatalogFeature() {
        super("Catalog", "Show or hide catalog symbols.");
    }

    /**
     * Receive notification that the feature has been unloaded.  Subclasses
     * should override if interested, but call super.unloaded().
     */
    @Override
    public void unloaded() {
        if (_iw != null) {
            TablePlotter p = _iw.plotter();
            if (p != null) {
                p.setVisible(false);
            }
        }
        super.unloaded();
    }

    /**
     * Reinitialize.  Override if additional initialization is required.
     */
    @Override
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);
        TablePlotter p = _iw.plotter();
        if (p != null) {
            p.setVisible(true);
        }
    }

    public void draw(Graphics g, TpeImageInfo tii) { }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.target;
    }
}

