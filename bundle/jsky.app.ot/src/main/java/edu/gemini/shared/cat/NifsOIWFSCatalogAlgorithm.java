// Copyright 2004 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: NifsOIWFSCatalogAlgorithm.java 47186 2012-08-02 16:54:23Z swalker $
//

package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting a NIFS OI WFS star.
 * <p><i>
 * "It should not be possible to identify a guide star in the field that is 100% vignetted by the NIFS
 * pickoff probe, which means that the area for guide star searches should be more complicated than
 * an inner and outer radius.  Plus, it should depend on which mode is being used - if Altair is being
 * used, then the AOWFS will have the standard search field, and the OIWFS will have a 1' radius plus
 * the probe vignetting limitation.  if Altair is not being used then the OIWFS search field should
 * be 1.'5 in radius with the probe vignetting limitation."</i>
 */
public final class NifsOIWFSCatalogAlgorithm extends OIWFSCatalogAlgorithm {
    private static final String NAME = "NIFS OIWFS w/o field lens";
    private static final String DESC = "Selects target for the NIFS OIWFS w/o field lens.";

    /**
     * Default constructor.
     */
    public NifsOIWFSCatalogAlgorithm() {
        this(false);
    }

    /**
     * Initialize based on the given flag.
     * @param isAltair true if the observation contains an Altair component
     */
    public NifsOIWFSCatalogAlgorithm(boolean isAltair) {
        super(NAME, DESC, 0.22, 1.0, -2.0, 14.5, Magnitude.Band.K);
    }

    /**
     * This algorithm is for a NIR Sensor, so let's override the default type
     */
    public SensorType getType() {
        return SensorType.NIR;
    }
}
