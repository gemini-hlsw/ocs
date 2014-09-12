package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;


/**
 * Base class for AOWFS catalog algorithms. This class behaves
 * exactly as the OIWFS algorithms, but sets the guide stars as "AOWFS" ones.
 */
public class AOWFSCatalogAlgorithm extends OIWFSCatalogAlgorithm {

    /**
     * Base class for finding guide stars for instrument specific AOWFS
     *
     * @param name display name
     * @param desc description
     * @param minRadius minimum radius in arcmin
     * @param maxRadius maximum radius in arcmin
     * @param brightLimit bright limit for magnitude (include only stars fainter than this)
     * @param faintLimit faint limit for magnitude (include only stars brighter than this)
     * @param band
     */
    public AOWFSCatalogAlgorithm(String name, String desc,
                                 double minRadius, double maxRadius,
                                 double brightLimit, double faintLimit, Magnitude.Band band) {
        super(name, desc, minRadius, maxRadius, brightLimit, faintLimit, band);
    }
}
