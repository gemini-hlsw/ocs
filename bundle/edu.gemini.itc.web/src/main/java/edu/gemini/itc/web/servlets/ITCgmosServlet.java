package edu.gemini.itc.web.servlets;

/**
 * ITC GMOS servlet.
 */
public final class ITCgmosServlet extends ITCServlet {
    public static final String VERSION = "5.0";
    public static final String INSTRUMENT = "GMOS";

    /**
     * Returns a version of this servlet
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * Returns the Instrument name
     */
    public String getInst() {
        return INSTRUMENT;
    }

}
