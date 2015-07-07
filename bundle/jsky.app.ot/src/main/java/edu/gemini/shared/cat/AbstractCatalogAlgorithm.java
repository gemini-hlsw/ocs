package edu.gemini.shared.cat;

/**
 * Provides an abstract implementation of the ICatalogAlgorithm interface
 * upon which subclasses may be based.  Subclasses must implement the
 * {@link ICatalogAlgorithm#getParameters}.
 */
@Deprecated
public abstract class AbstractCatalogAlgorithm implements ICatalogAlgorithm {

    public static final String DEFAULT_STAR_TYPE = "Guide Star";

    private String _name;

    private String _desc;

    private String[] _typeOptions;

    private String _defaultType;

    protected CatalogSearchParameters _csp;

    /**
     * Constructs with the name and description.
     */
    public AbstractCatalogAlgorithm(String name, String description) {
        _name = name;
        _desc = description;
    }

    /**
     * Gets the name of the algorithm.
     */
    public String getName() {
        return _name;
    }

    /**
     * Gets descriptive information about the algorithm.
     */
    public String getDescription() {
        return _desc;
    }

    /**
     * Gets the guide star type options for the stars selected by this algorithm.
     */
    public String[] getStarTypeOptions() {
        if (_typeOptions == null) {
            _typeOptions = new String[]{DEFAULT_STAR_TYPE};
        }
        int sz = _typeOptions.length;
        String[] opts = new String[sz];
        System.arraycopy(_typeOptions, 0, opts, 0, sz);
        return opts;
    }

    /**
     * Sets the guide star type options.  The array is owned by the class after
     * this call and should not be modified by the caller.
     */
    protected void setStarTypeOptions(String[] opts) {
        _typeOptions = opts;
    }

    /**
     * Gets the default star type.
     */
    public String getDefaultStarType() {
        if (_defaultType == null) {
            if (_typeOptions == null) {
                return DEFAULT_STAR_TYPE;
            } else {
                return _typeOptions[0];
            }
        }
        return _defaultType;
    }

    /**
     * Sets the default star type.  Ideally, this should probably be one of the
     * type options.
     */
    protected void setDefaultStarType(String defaultType) {
        _defaultType = defaultType;
    }

    /**
     * Returns a copy of the CatalogSearchParameters desired by this algorithm.
     */
    public CatalogSearchParameters getParameters() {
        if (_csp == null) {
            _csp = new CatalogSearchParameters();
        }
        return (CatalogSearchParameters) _csp.clone();
    }

    /**
     * Sets the CatalogSearchParameters.  The parameters are subsequently owned
     * by this class and should not be modified by the caller.
     */
    protected void setParameters(CatalogSearchParameters csp) {
        _csp = csp;
    }

    /**
     * Overrides <code>toString()</code> to return the algorithm name.
     */
    public String toString() {
        return _name;
    }

    public SensorType getType() {
        return SensorType.DEFAULT;
    }

}
