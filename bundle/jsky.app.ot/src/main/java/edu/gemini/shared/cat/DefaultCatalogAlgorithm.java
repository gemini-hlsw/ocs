// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DefaultCatalogAlgorithm.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.cat;

/**
 * A default catalog algorithm that simply selects the first few stars returned
 * by a catalog.
 */
public class DefaultCatalogAlgorithm extends AbstractCatalogAlgorithm {

    private static final int LIMIT = 3;

    private static final String NAME = "Default";

    private static final String DESC = "This algorithm selects the first three stars returned by the server.";

    //"Gemini (NIRI)",
    //"This algorithm selects WFS stars suitable for use with the NIRI instrument on the Gemini telescopes."

    private int _limit;

    /**
     * Constructs with the default limit.
     */
    public DefaultCatalogAlgorithm() {
        this(LIMIT);
    }

    /**
     * Constructs with the given limit.
     *
     * @param limit the number of stars to be selected from the server's output
     */
    public DefaultCatalogAlgorithm(int limit) {
        super(NAME, DESC);
        _limit = limit;
    }

    /**
     * Gets the number of stars that will be selected by the algorithm.
     */
    public int getLimit() {
        return _limit;
    }

    /**
     * Sets the number of stars that will be selected by the algorithm.
     */
    public void setLimit(int limit) {
        _limit = limit;
    }
}
