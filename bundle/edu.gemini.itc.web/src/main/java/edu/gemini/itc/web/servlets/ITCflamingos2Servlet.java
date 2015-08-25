package edu.gemini.itc.web.servlets;

/**
 * ITC F2 servlet.
 */
public final class ITCflamingos2Servlet extends ITCServlet {
    public static final String VERSION = "1.0";
    public static final String INSTRUMENT = "Flamingos-2";

    /**
     * Returns a version of this servlet
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * Returns the instrument name
     */
    public String getInst() {
        return INSTRUMENT;
    }

}
