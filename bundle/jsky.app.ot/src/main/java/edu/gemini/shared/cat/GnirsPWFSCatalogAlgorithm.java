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
public final class GnirsPWFSCatalogAlgorithm extends PWFSCatalogAlgorithm {
    private static final String NAME = "GNIRS PWFS2";
    private static final String DESC = "Selects target Gemini PWFS2 with GNIRS.";

    /**
     * Default constructor.
     */
    public GnirsPWFSCatalogAlgorithm() {
        this(NAME, DESC);
    }

    /**
     * Constructor that sets the algorithm search parameters
     */
    public GnirsPWFSCatalogAlgorithm(String name, String desc) {
        super(name, desc, 4.8, 6.95, 9.0, 14.5, Magnitude.Band.R);
    }


}
