package edu.gemini.itc.web.servlets;

/**
 * ITC Acq Cam servlet.
 */
public final class ITCacqCamServlet extends ITCServlet {
    public static final String VERSION = "4.0";
    public static final String INSTRUMENT = "Acquisition Camera";

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
