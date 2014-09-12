// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: PWFSCatalogAlgorithm.java 47186 2012-08-02 16:54:23Z swalker $
//

package edu.gemini.shared.cat;

import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.catalog.api.RadiusLimits;
import edu.gemini.skycalc.Angle;
import edu.gemini.shared.skyobject.Magnitude;


/**
 * <em><b>This class is temporary at this location.</b></em>.  Crude first
 * pass at the algorithm for estimating the two Gemini PWFS stars.
 *
 * <p>Expects the output from a GSC I server.  Substitutes V band mag for
 * the (proper) R band mag since that's all GSC I has.  Doesn't consider
 * distance for ties in magnitude.
 */
public class PWFSCatalogAlgorithm extends AbstractCatalogAlgorithm {
    private static final String NAME = "Gemini PWFS";
    private static final String DESC = "Selects target for either of the two Gemini PWFS patrol fields.";

    /**
     * Create with the default settings.
     */
    public PWFSCatalogAlgorithm() {
        this(NAME, DESC, 5.3, 6.95, 9.0, 14.5, Magnitude.Band.R);
    }

    /**
     * Create with the given settings.
     *
     * @param name name of the algorithm.
     * @param description description for the catalog
     * @param minRadius minimum radius in arcmin
     * @param maxRadius maximum radius in arcmin
     * @param brightLimit bright limit for magnitude (include only stars fainter than this)
     * @param faintLimit faint limit for magnitude (include only stars brighter than this)
     * @param band
     */
    protected PWFSCatalogAlgorithm(String name, String description,
                                   double minRadius, double maxRadius,
                                   double brightLimit, double faintLimit, Magnitude.Band band) {
        super(name, description);
        setStarTypeOptions(new String[]{"WFS", "OIWFS", "AOWFS"});

        CatalogSearchParameters csp = new CatalogSearchParameters();
        csp.setRadiusLimits(new RadiusLimits(new Angle(minRadius, Angle.Unit.ARCMINS), new Angle(maxRadius, Angle.Unit.ARCMINS)));
        csp.setMagnitudeLimits(new MagnitudeLimits(band, new MagnitudeLimits.FaintnessLimit(faintLimit), new MagnitudeLimits.SaturationLimit(brightLimit)));
        setParameters(csp);
    }
}
