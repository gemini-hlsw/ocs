package edu.gemini.itc.web.servlets;

import edu.gemini.itc.gems.GemsParameters;
import edu.gemini.itc.gsaoi.GsaoiParameters;
import edu.gemini.itc.shared.Parameters;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.itc.web.html.GsaoiPrinter;

import java.io.PrintWriter;

/**
 * ITC GSAOI servlet.
 */
public final class ITCgsaoiServlet extends ITCServlet {
    public static final String VERSION = "4.2";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "GSAOI";

    public ITCgsaoiServlet() {
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
        return getTitle() + " " + getVersion() + " - ITCGsaoiServlet accepts form data and performs ITC calculation for Gsaoi.";
    }

    /**
     * Supply the body content for the html document.
     */
    public void writeOutput(final ITCRequest r, final PrintWriter out) {
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");
        final Parameters p  = ITCRequest.parameters(r);
        final GsaoiParameters ip = ITCRequest.gsaoiParameters(r);
        final GemsParameters altair = ITCRequest.gemsParameters(r);
        final GsaoiPrinter printer = new GsaoiPrinter(p, ip, altair, out);
        printer.writeOutput();
    }
}
