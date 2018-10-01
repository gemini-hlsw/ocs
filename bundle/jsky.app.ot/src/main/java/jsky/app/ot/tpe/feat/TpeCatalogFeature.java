package jsky.app.ot.tpe.feat;

import edu.gemini.shared.util.immutable.ImOption;
import jsky.app.ot.tpe.TpeImageFeature;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeImageFeatureCategory;

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
        setVisible(false);
        super.unloaded();
    }

    @Override
    public void draw(Graphics g, TpeImageInfo tii) {
        // Drawing is handled by the plotter that the image widget maintains,
        // so we just need to make sure it is visible whenever we are asked to
        // draw.
        setVisible(true);
    }

    private void setVisible(final boolean isVisible) {
        ImOption.apply(_iw)
                .flatMap(iw -> ImOption.apply(iw.plotter()))
                .foreach(p -> p.setVisible(isVisible));
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.target;
    }
}

