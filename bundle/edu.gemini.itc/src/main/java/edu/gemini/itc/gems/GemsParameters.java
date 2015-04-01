package edu.gemini.itc.gems;

import edu.gemini.itc.shared.ITCParameters;

/**
 * This class holds the information from the Gems section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GemsParameters extends ITCParameters {

    private final double _avgStrehl;
    private final String _strehlBand;

    public GemsParameters(final double avgStreh, final String strehlBand) {
        _avgStrehl = avgStreh;
        _strehlBand = strehlBand;
    }

    public double getAvgStrehl() {
        return _avgStrehl;
    }

    public String getStrehlBand() {
        return _strehlBand;
    }

}
