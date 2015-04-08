package edu.gemini.itc.web.servlets;

import edu.gemini.itc.web.ITCMultiPartParser;
import edu.gemini.itc.shared.Parameters;
import edu.gemini.itc.shared.PlottingDetails;
import edu.gemini.itc.trecs.TRecsParameters;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.itc.web.html.TRecsPrinter;

import java.io.PrintWriter;

/**
 * ITC TRecs servlet.
 */
public final class ITCTrecsServlet extends ITCServlet {

    public static final String VERSION = "4.0";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "T-ReCS";

    public ITCTrecsServlet() {
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
        return getTitle() + " " + getVersion() + " - ITCTrecsServlet accepts form data and performs ITC calculation for T-Recs.";
    }

    /**
     * Supply the body content for the html document.
     */
    public void writeOutput(ITCMultiPartParser mpp, PrintWriter out) {
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");
        final Parameters p  = ITCRequest.parameters(mpp);
        final TRecsParameters ip = ITCRequest.trecsParameters(mpp);
        final PlottingDetails pdp = ITCRequest.plotParameters(mpp);
        final TRecsPrinter printer = new TRecsPrinter(p, ip, pdp, out);
        printer.writeOutput();
    }
}
