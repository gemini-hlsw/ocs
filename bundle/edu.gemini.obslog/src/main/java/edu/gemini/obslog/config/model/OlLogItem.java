package edu.gemini.obslog.config.model;

/**
 * A possible entry in the Observing Log must have one of these defining its characteristics.
 */
public interface OlLogItem {

    String DEFAULT_ENTRY = "Not Set";

    /**
     * Return the entry key.  Used to refer to this entry but may not be used in the
     * obslog presentation.
     */
    String getKey();

    /**
     * Return the column heading that matches entries of this type.
     */
    String getColumnHeading();

    /**
     * Return the low-level item name that should be given to the data from the sequence.
     *
     * @return the item name
     */
    String getProperty();

    /**
     * Return the sequence name for this entry.  This is the value used to match data in ObservationData.
     */
    String getSequenceName();

    /**
     * Should this item be included in the table columns.  Some values are only used for calculation of
     * other values.
     *
     * @return true if it should be shown
     */
    boolean isVisible();

}

