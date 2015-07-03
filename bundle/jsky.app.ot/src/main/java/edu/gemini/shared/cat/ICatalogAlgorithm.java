package edu.gemini.shared.cat;

/**
 * Defines the interface that catalog filtering algorithms must implement.
 */
@Deprecated
public interface ICatalogAlgorithm {

    /**
     * Represents the sensor type of the instrument this algorithm is used for.
     * This can be used to group certain algorithms and perform actions over them
     */
    enum SensorType {
        UNDEFINED, //No specific type
        NIR, //Near-Infrared
        OPTICAL;
        public static SensorType DEFAULT = UNDEFINED;
    }


    /**
     * Gets the name of the algorithm.
     */
    String getName();

    /**
     * Gets descriptive information about the algorithm.
     */
    String getDescription();

    /**
     * Gets the guide star type options for the stars selected by this
     * algorithm.
     */
    String[] getStarTypeOptions();

    /**
     * Gets the default star type.
     */
    String getDefaultStarType();

    /**
     * Gets the parameters that should be used for a search with this
     * alogorithm.
     */
    CatalogSearchParameters getParameters();

    /**
     * Returns this algorithm sensor type. This can be used to perform certain
     * operations that apply to certain algorithms only.
     */
    SensorType getType();
}
