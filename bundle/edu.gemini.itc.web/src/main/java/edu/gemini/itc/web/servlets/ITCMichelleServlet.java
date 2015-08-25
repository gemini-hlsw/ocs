package edu.gemini.itc.web.servlets;

/**
 * ITC Michelle servlet.
 */
public final class ITCMichelleServlet extends ITCServlet {
    public static final String VERSION = "4.2";
    public static final String INSTRUMENT = "Michelle";

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
