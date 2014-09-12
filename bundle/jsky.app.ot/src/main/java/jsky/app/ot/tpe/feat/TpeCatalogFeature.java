// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TpeCatalogFeature.java 21629 2009-08-21 20:27:31Z swalker $
//
package jsky.app.ot.tpe.feat;

import jsky.app.ot.tpe.TpeImageFeature;
import jsky.app.ot.tpe.TpeImageInfo;
import jsky.app.ot.tpe.TpeImageWidget;
import jsky.app.ot.tpe.TpeImageFeatureCategory;
import jsky.catalog.gui.TablePlotter;
import jsky.navigator.Navigator;

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
     * should override if interrested, but call super.unloaded().
     */
    public void unloaded() {
        if (_iw != null) {
            Navigator nav = _iw.getNavigator();
            if (nav != null) {
                TablePlotter p = nav.getPlotter();
                if (p != null) {
                    p.setVisible(false);
                }
            }
        }
        super.unloaded();
    }

    /**
     * Reinitialize.  Override if additional initialization is required.
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);
        Navigator nav = _iw.getNavigator();
        if (nav != null) {
            TablePlotter p = nav.getPlotter();
            if (p != null) {
                p.setVisible(true);
            }
        }
    }

    /**
     */
    public void draw(Graphics g, TpeImageInfo tii) {
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.target;
    }
}

