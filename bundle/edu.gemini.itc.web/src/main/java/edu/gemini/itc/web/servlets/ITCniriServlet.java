package edu.gemini.itc.web.servlets;

import edu.gemini.itc.shared.NiriParameters;
import edu.gemini.itc.shared.Parameters;
import edu.gemini.itc.shared.PlottingDetails;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.itc.web.html.NiriPrinter;

import java.io.PrintWriter;

/**
 * ITC NIRI servlet.
 */
public final class ITCniriServlet extends ITCServlet {
    public static final String VERSION = "4.2";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "NIRI";

    public ITCniriServlet() {
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
        return getTitle() + " " + getVersion() + " - ITCNiriServlet accepts form data and performs ITC calculation for Niri.";
    }

    /**
     * Supply the body content for the html document.
     */
    public void writeOutput(final ITCRequest r, PrintWriter out) {
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");
        final Parameters params     = ITCRequest.parameters(r);
        final NiriParameters niri   = ITCRequest.niriParameters(r);
        final PlottingDetails plot  = ITCRequest.plotParameters(r);
        final NiriPrinter printer = new NiriPrinter(params, niri, plot, out);
        printer.writeOutput();

    }
}
