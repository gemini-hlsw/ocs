package edu.gemini.itc.gsaoi;

import edu.gemini.itc.gems.GemsParameters;
import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi.*;

/**
 * GSAOI parameters relevant for ITC.
 */
public final class GsaoiParameters implements InstrumentDetails {

    private final Filter filter;
    private final ReadMode readMode;
    private final GemsParameters gems;

    public GsaoiParameters(final Filter filter, final ReadMode readMode, final GemsParameters gems) {
        this.filter   = filter;
        this.readMode = readMode;
        this.gems     = gems;
    }

    public Filter getFilter() {
        return filter;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

    public GemsParameters getGems() {
        return gems;
    }

}
