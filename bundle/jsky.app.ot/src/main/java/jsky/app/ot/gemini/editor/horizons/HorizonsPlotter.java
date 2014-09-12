package jsky.app.ot.gemini.editor.horizons;

import edu.gemini.horizons.api.EphemerisEntry;
import edu.gemini.pot.sp.ISPNode;
import jsky.app.ot.tpe.TelescopePosEditor;
import jsky.app.ot.tpe.TpeImageWidget;
import jsky.app.ot.tpe.TpeManager;

import java.util.List;

/**
 * Just a simple class with one static method to plot the ephemeris in the TPE.
 * Must be a nicer way to set up the HorizonsFeature on the TPE but I ran out of
 * time to finding it. Anyway, the "addFeature" command will  do nothing if the
 * feature already exists, so the overhead here is fairly minimum
 */
public class HorizonsPlotter {

    public static TpeHorizonsFeature _horizons = new TpeHorizonsFeature("Horizons", "Non Sidereal Objects Feature based on Horizons");

    public static void plot(ISPNode node, List<EphemerisEntry> data) {
        TelescopePosEditor tpe = TpeManager.open();
        tpe.reset(node);
        TpeImageWidget iw = tpe.getImageWidget();
        _horizons.setEphemeris(data);
        iw.addFeature(_horizons);
    }
}
