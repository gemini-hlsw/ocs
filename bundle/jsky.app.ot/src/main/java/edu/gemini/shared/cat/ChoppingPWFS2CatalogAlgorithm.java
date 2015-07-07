package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * <em><b>This class is temporary at this location.</b></em>.  This class is exactly the same as
 * {@link PWFSCatalogAlgorithm } but with slightly different parameters.  Should be refactored I guess.
 *
 * <p>Expects the output from a GSC I server.  Substitutes V band mag for
 * the (proper) R band mag since that's all GSC I has.  Doesn't consider
 * distance for ties in magnitude.
 */
@Deprecated
public final class ChoppingPWFS2CatalogAlgorithm extends PWFSCatalogAlgorithm {
    private static final String NAME = "Gemini PWFS2 chopping";
    private static final String DESC = "Selects target for the Gemini PWFS2 patrol fields when chopping.";

    /**
     * Default constructor.
     */
    public ChoppingPWFS2CatalogAlgorithm() {
        this(NAME, DESC);
    }

    /**
     * Constructor that sets the algorithm search parameters
     */
    public ChoppingPWFS2CatalogAlgorithm(String name, String desc) {
        super(name, desc, 4.8, 6.95, 7.5, 13, Magnitude.Band.R);
    }
}
