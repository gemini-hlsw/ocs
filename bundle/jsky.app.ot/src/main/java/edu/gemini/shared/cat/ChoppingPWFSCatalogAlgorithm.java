package edu.gemini.shared.cat;

import edu.gemini.shared.skyobject.Magnitude;

/**
 * <em><b>This class is temporary at this location.</b></em>.  This class is exactly the same as
 * {@link edu.gemini.shared.cat.PWFSCatalogAlgorithm } but with slightly different parameters.  Should be refactored I guess.
 *
 * <p>Expects the output from a GSC I server.  Substitutes V band mag for
 * the (proper) R band mag since that's all GSC I has.  Doesn't consider
 * distance for ties in magnitude.
 */
@Deprecated
public final class ChoppingPWFSCatalogAlgorithm extends PWFSCatalogAlgorithm {
    private static final String NAME = "Gemini PWFS chopping";
    private static final String DESC = "Selects target for either of the two Gemini PWFS patrol fields when chopping.";

    /**
     * Default constructor.
     */
    public ChoppingPWFSCatalogAlgorithm() {
        this(NAME, DESC);
    }

    /**
     * Constructor that sets the algorithm search parameters
     */
    public ChoppingPWFSCatalogAlgorithm(String name, String desc) {
        // REL-346:  PWFS1 chopping, 5.3, 6.95, 13 ## Change max radius
        super(name, desc, 5.3, 6.95, 7.5, 13, Magnitude.Band.R);
    }
}
