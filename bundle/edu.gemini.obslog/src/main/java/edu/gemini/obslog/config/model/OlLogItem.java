package edu.gemini.obslog.config.model;

//
// Gemini Observatory/AURA
// $Id: OlLogItem.java,v 1.2 2005/03/17 06:04:33 gillies Exp $
//

/**
 * A possible entry in the Observing Log must have one of these defining its characteristics.
 */
public interface OlLogItem {

    public static final String DEFAULT_ENTRY = "Not Set";

    /**
     * Return the entry key.  Used to refer to this entry but may not be used in the
     * obslog presentation.
     */
    public String getKey();

    /**
     * Return the column heading that matches entries of this type.
     */
    public String getColumnHeading();

    /**
     * Return the low-level item name that should be given to the data from the sequence.
     *
     * @return the item name
     */
    public String getProperty();

    /**
     * Return the sequence name for this entry.  This is the value used to match data in ObservationData.
     */
    public String getSequenceName();

    /**
     * Should this item be included in the table columns.  Some values are only used for calculation of
     * other values.
     *
     * @return true if it should be shown
     */
    public boolean isVisible();

}

