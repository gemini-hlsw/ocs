package edu.gemini.shared.cat;

import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.catalog.api.RadiusLimits;
import edu.gemini.skycalc.Angle;
import edu.gemini.shared.skyobject.Magnitude;

/**
 * Base class for OIWFS catalog algorithms
 */
@Deprecated
public class OIWFSCatalogAlgorithm extends AbstractCatalogAlgorithm {

    /**
     * Base class for finding guide stars for instrument specific OIWFS
     *
     * @param name display name
     * @param desc description
     * @param minRadius minimum radius in arcmin
     * @param maxRadius maximum radius in arcmin
     * @param brightLimit bright limit for magnitude (include only stars fainter than this)
     * @param faintLimit faint limit for magnitude (include only stars brighter than this)
     * @param band band for which to apply the magnitude limits
     */
    public OIWFSCatalogAlgorithm(String name, String desc,
                                 double minRadius, double maxRadius,
                                 double brightLimit, double faintLimit, Magnitude.Band band) {
        super(name, desc);

        setStarTypeOptions(new String[]{"WFS", "OIWFS", "AOWFS"});

        CatalogSearchParameters csp = new CatalogSearchParameters();
        csp.setRadiusLimits(new RadiusLimits(new Angle(maxRadius, Angle.Unit.ARCMINS), new Angle(minRadius, Angle.Unit.ARCMINS)));
        csp.setMagnitudeLimits(new MagnitudeLimits(band, new MagnitudeLimits.FaintnessLimit(faintLimit), new MagnitudeLimits.SaturationLimit(brightLimit)));
        setParameters(csp);
    }
}
