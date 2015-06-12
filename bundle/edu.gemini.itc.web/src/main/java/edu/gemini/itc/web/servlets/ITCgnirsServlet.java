package edu.gemini.itc.web.servlets;

import edu.gemini.itc.shared.GnirsParameters;
import edu.gemini.itc.shared.Parameters;
import edu.gemini.itc.shared.PlottingDetails;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.itc.web.html.GnirsPrinter;

import java.io.PrintWriter;

/**
 * ITC GNIRS servlet.
 */
public final class ITCgnirsServlet extends ITCServlet {
    public static final String VERSION = "4.0";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "GNIRS";

    public ITCgnirsServlet() {
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
        return getTitle() + " " + getVersion() + " - ITCgnirsServlet accepts form data and performs ITC calculation for GNIRS.";
    }

    /**
     * Supply the body content for the html document.
     */
    public void writeOutput(final ITCRequest r, final PrintWriter out) {
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");
        final Parameters p  = ITCRequest.parameters(r);
        final GnirsParameters ip = ITCRequest.gnirsParameters(r);
        final PlottingDetails pdp = ITCRequest.plotParameters(r);
        final GnirsPrinter printer = new GnirsPrinter(p, ip, pdp, out);
        printer.writeOutput();
    }
}
