package edu.gemini.shared.cat;

/**
 * A default catalog algorithm that simply selects the first few stars returned
 * by a catalog.
 */
@Deprecated
public class DefaultCatalogAlgorithm extends AbstractCatalogAlgorithm {

    private static final int LIMIT = 3;

    private static final String NAME = "Default";

    private static final String DESC = "This algorithm selects the first three stars returned by the server.";

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
