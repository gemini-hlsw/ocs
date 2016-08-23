package edu.gemini.spModel.core;

import java.util.logging.Logger;

public enum Affiliate {

    ARGENTINA("Argentina", "AR"),
    AUSTRALIA("Australia", "AU"),
    BRAZIL("Brazil", "BR"),
    CANADA("Canada", "CA"),
    CHILE("Chile", "CL"),
    KOREA("Republic of Korea", "KR"),
    UNITED_KINGDOM("United Kingdom", "UK", false),
    UNITED_STATES("United States", "US"),

    GEMINI_STAFF("Gemini Staff"),
    UNIVERSITY_OF_HAWAII("University of Hawaii", "UH"),;

    private static final Logger LOG = Logger.getLogger(Affiliate.class.getName());

    public final String displayValue;
    public final String isoCode;
    public final boolean isActive;

    Affiliate(final String displayValue) {
        this(displayValue, null);
    }

    Affiliate(final String displayValue, final String code) {
        this(displayValue, code, true);
    }

    Affiliate(final String displayValue, final String isoCode, final boolean isActive) {
        this.displayValue = displayValue;
        this.isoCode = isoCode;
        this.isActive = isActive;
    }

    /**
     * Returns the affiliate associated with the given string, if any, by comparing the given string with the various
     * probable string representations of the defined affiliates. This method is intended for data migration and will
     * be removed in a future version. Don't use Strings!
     * @return the matching Affiliate, otherwise null
     */
    @Deprecated
    public static Affiliate fromString(final String s) {
        if (s != null) {
            final String lcs = s.trim();
            for (final Affiliate a : values()) {
                if (lcs.equalsIgnoreCase(a.name()) || lcs.equalsIgnoreCase(a.displayValue) || lcs.equalsIgnoreCase(a.isoCode))
                    return a;
            }
            LOG.warning("Unknown affiliate: " + s);
        }
        return null;
    }

}

