package edu.gemini.obslog.core;

import edu.gemini.pot.sp.SPComponentType;

import java.io.Serializable;

//
// Gemini Observatory/AURA
// $Id: OlSegmentType.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
// Patt
//

/**
 * Patterned after {@link edu.gemini.pot.sp.SPComponentType SPComponentType}.
 */
public final class OlSegmentType implements Serializable {
    static final long serialVersionUID = 1L;

    private static final String UNKNOWN_VALUE = "unknown";

    private String _narrowType;

    static public final OlSegmentType UNKNOWN_TYPE = new OlSegmentType(UNKNOWN_VALUE);

    /**
     * Default constructor, creates
     */
    public OlSegmentType() {
        _narrowType = UNKNOWN_VALUE;
    }

    /**
     * Construct with the broad type and narrow type of the segment.
     *
     * @param narrowType specific type of log -- like "gmos"
     * @throws IllegalArgumentException if null or other unreasonable value is provided.
     */
    public OlSegmentType(String narrowType) throws IllegalArgumentException {
        if (narrowType == null) throw new IllegalArgumentException();

        _narrowType = narrowType;
    }

    /**
     * Construct a new OlSegmentType from the <code>instType</code> of the instrument.
     *
     * @param instType an {@link SPComponentType} usually from the instrument being logged
     */
    public OlSegmentType(SPComponentType instType) {
        // Ignore the broad type
        _narrowType = instType.narrowType;
    }

    /**
     * The narrow type is returned as a <code>String</code>
     *
     * @return the narrow type
     */
    public String getType() {
        return _narrowType;
    }

    /**
     * Overrides to agree with the redefinition of <code>equals</code>.
     */
    public int hashCode() {
        return _narrowType.hashCode();
    }

    /**
     * Overrides to provide semantic equivalence instead of default,
     * reference, equivalence.
     */
    public boolean equals(Object o) {

        if (!(o instanceof OlSegmentType)) return false;

        OlSegmentType that = (OlSegmentType) o;

        // The idea here is that the first part of one of the narrow types
        // must match.   This is so that GMOS matches GMOSSouth and GMOSNorth
        boolean test1 = _narrowType.startsWith(that._narrowType);
        boolean test2 = that._narrowType.startsWith(_narrowType);

        return test1 || test2;
    }

    /**
     * Overrides to expose the broad and narrow type.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(this.getClass().getName());
        buf.append(", narrowType=[").append(_narrowType).append(']');
        return buf.toString();
    }

}
