package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCServlet;
import edu.gemini.itc.shared.Recipe;

import java.io.PrintWriter;

/**
 * ITC GMOS servlet.
 */
public final class ITCgmosServlet extends ITCServlet {
    public static final String VERSION = "5.0";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "GMOS";

    public ITCgmosServlet() {
        super();
    }

    /**
     * Returns a title
     */
    public String getTitle() {
        return TITLE;
    }

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

    /**
     * Describes the purpose of the servlet.
     * Used by Java Web Server Administration Tool.
     */
    public String getServletInfo() {
        return getTitle() + " " + getVersion() + " - ITCgmosServlet accepts form data and performs ITC calculation for Gmos.";
    }

    /**
     * Supply the body content for the html document.
     */
    public void writeOutput(final ITCMultiPartParser mpp, final PrintWriter out) {
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");
        final Recipe recipe = new GmosRecipe(mpp, out);
        recipe.writeOutput();
    }
}
