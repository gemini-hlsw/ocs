package edu.gemini.itc.web.servlets;

import edu.gemini.itc.shared.AcquisitionCamParameters;
import edu.gemini.itc.shared.Parameters;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.itc.web.html.AcqCamPrinter;

import java.io.PrintWriter;

/**
 * ITC Acq Cam servlet.
 */
public final class ITCacqCamServlet extends ITCServlet {
    public static final String VERSION = "4.0";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "Acquisition Camera";

    public ITCacqCamServlet() {
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
        return getTitle() + " " + getVersion() + " - ITCacqCamServlet accepts form data and performs ITC calculation for the Acquisition Camera.";
    }

    /**
     * Supply the body content for the html document.
     */
    public void writeOutput(final ITCRequest r, final PrintWriter out) {
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");
        final Parameters p  = ITCRequest.parameters(r);
        final AcquisitionCamParameters ip = ITCRequest.acqCamParameters(r);
        final AcqCamPrinter printer = new AcqCamPrinter(p, ip, out);
        printer.writeOutput();
    }
}
