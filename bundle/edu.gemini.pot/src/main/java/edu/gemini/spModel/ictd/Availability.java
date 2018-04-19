package edu.gemini.spModel.ictd;

/**
 * Summarizes location, categorizing the various detailed location options
 * (MKO Cabinet, HBF Cabinet, etc.) into the options that are important for our
 * application.
 */
public enum Availability {

    /** Available for use. */
    Installed,

    /** Not installed, but at the summit and could be installed. */
    SummitCabinet,

    /** No longer on the summit and not immediately available. */
    Unavailable,

    /** Should be tracked by the ICTD but wasn't found in the database. */
    Missing,
    ;

    /**
     * Zero in the Monoid sense.  There is no more available option than
     * Installed, so for any Availability a, Installed |+| a = a |+| Installed = a.
     */
    public static final Availability Zero = Installed;

    /** Determines the least available of two availability instances. */
    public Availability plus(final Availability that) {
        return (ordinal() < that.ordinal()) ? that : this;
    }

}
