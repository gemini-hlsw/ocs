package edu.gemini.spModel.guide;

public enum GuideStarValidation {

    /** Guide star is valid in the given context. */
    VALID,

    /** Guide star is <em>not</em> valid in the given context. */
    INVALID,

    /** Validity cannot be checked because guide star or base coordinates are undefined. */
    UNDEFINED

    ;

    /** Return the most pessimistic value. */
    public GuideStarValidation and(GuideStarValidation other) {
        return ordinal() < other.ordinal() ? other : this;
    }

}
