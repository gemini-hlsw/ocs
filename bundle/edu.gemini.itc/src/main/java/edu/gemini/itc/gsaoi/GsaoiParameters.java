package edu.gemini.itc.gsaoi;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi.*;

/**
 * GSAOI parameters relevant for ITC.
 */
public final class GsaoiParameters implements InstrumentDetails {

    private final Filter filter;
    private final ReadMode readMode;

    public GsaoiParameters(final Filter filter, final ReadMode readMode) {
        this.filter = filter;
        this.readMode = readMode;
    }

    public Filter getFilter() {
        return filter;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

}
