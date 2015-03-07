package edu.gemini.itc.flamingos2;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCServlet;
import edu.gemini.itc.shared.Recipe;

import java.io.PrintWriter;

/**
 * ITC F2 servlet.
 */
public final class ITCflamingos2Servlet extends ITCServlet {
    public static final String VERSION = "1.0";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "Flamingos-2";

    public ITCflamingos2Servlet() {
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
     * Returns the instrument name
     */
    public String getInst() {
        return INSTRUMENT;
    }

    /**
     * Describes the purpose of the servlet.
     * Used by Java Web Server Administration Tool.
     */
    public String getServletInfo() {
        return getTitle() + " " + getVersion() + " - ITCflamingos2Servlet accepts form data and performs ITC calculation for Flamingos-2.";
    }

    /**
     * Supply the body content for the html document.
     */
    public void writeOutput(ITCMultiPartParser mpp, PrintWriter out) {
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");
        Recipe recipe = new Flamingos2Recipe(mpp, out);
        recipe.writeOutput();
    }
}
