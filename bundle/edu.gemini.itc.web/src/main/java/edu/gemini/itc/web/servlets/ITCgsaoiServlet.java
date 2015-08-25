package edu.gemini.itc.web.servlets;

/**
 * ITC GSAOI servlet.
 */
public final class ITCgsaoiServlet extends ITCServlet {
    public static final String VERSION = "4.2";
    public static final String INSTRUMENT = "GSAOI";

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
