package edu.gemini.itc.web.servlets;

import edu.gemini.itc.michelle.MichelleParameters;
import edu.gemini.itc.web.ITCMultiPartParser;
import edu.gemini.itc.shared.Parameters;
import edu.gemini.itc.shared.PlottingDetails;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.itc.web.html.MichellePrinter;

import java.io.PrintWriter;

/**
 * ITC Michelle servlet.
 */
public final class ITCMichelleServlet extends ITCServlet {
    public static final String VERSION = "4.2";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "Michelle";

    public ITCMichelleServlet() {
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
        return getTitle() + " " + getVersion() + " - ITCMichelleServlet accepts form data and performs ITC calculation for Michelle.";
    }

    /**
     * Supply the body content for the html document.
     */
    public void writeOutput(ITCMultiPartParser mpp, PrintWriter out) {
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");
        final Parameters p  = ITCRequest.parameters(mpp);
        final MichelleParameters ip = ITCRequest.michelleParameters(mpp);
        final PlottingDetails pdp = ITCRequest.plotParameters(mpp);
        final MichellePrinter printer = new MichellePrinter(p, ip, pdp, out);
        printer.writeOutput();
    }
}
