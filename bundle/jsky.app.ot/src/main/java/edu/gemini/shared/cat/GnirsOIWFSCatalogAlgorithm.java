// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: GnirsOIWFSCatalogAlgorithm.java 47126 2012-08-01 15:40:43Z swalker $
//

package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * Algorithm for suggesting GNIRS OI WFS stars.
 *
 */
public class GnirsOIWFSCatalogAlgorithm extends OIWFSCatalogAlgorithm {
    private static final String NAME = "GNIRS OIWFS";
    private static final String DESC = "Selects target for the GNIRS OIWFS";

    /**
     * Default constructor.
     */
    public GnirsOIWFSCatalogAlgorithm() {
        super(NAME, DESC, 0.2, 1.5, 0.0, 14.0, Magnitude.Band.K);
    }


    @Override
    public ICatalogAlgorithm.SensorType getType() {
        return ICatalogAlgorithm.SensorType.NIR;
    }
}
