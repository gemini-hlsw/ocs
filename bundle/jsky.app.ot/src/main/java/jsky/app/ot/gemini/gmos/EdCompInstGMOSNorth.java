package jsky.app.ot.gemini.gmos;

import edu.gemini.spModel.gemini.gmos.InstGmosNorth;

/**
 * Class EdCompInstGMOSNorth. This class is of not much use currently, because EdCompInstGMOS has everything set-up for
 * GMOS-N, and in EdCompInstGMOSSouth it is re-customized for GMOS-S.
 *
 * @author Nicolas A. Barriga
 *         Date: 4/25/11
 */
public class EdCompInstGMOSNorth extends EdCompInstGMOS<InstGmosNorth> {
    public EdCompInstGMOSNorth() {
        super();
    }

    @Override protected void init() {
        super.init();

        // Add the property change listeners defined in InstGmosCommon.
        getDataObject().addPropertyChangeListener(InstGmosNorth.FPUNIT_PROP.getName(), updateParallacticAnglePCL);
        getDataObject().addPropertyChangeListener(InstGmosNorth.FPUNIT_PROP.getName(), relativeTimeMenuPCL);
    }
}
