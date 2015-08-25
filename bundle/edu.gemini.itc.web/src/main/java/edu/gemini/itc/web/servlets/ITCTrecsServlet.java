package edu.gemini.itc.web.servlets;

/**
 * ITC TRecs servlet.
 */
public final class ITCTrecsServlet extends ITCServlet {

    public static final String VERSION = "4.0";
    public static final String INSTRUMENT = "T-ReCS";

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
