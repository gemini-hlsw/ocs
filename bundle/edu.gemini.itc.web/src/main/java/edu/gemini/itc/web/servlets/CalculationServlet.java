package edu.gemini.itc.web.servlets;

import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.ITCMultiPartParser;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.itc.web.html.*;
import edu.gemini.spModel.core.Version;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * Base class for ITC servlets.  Derive servlets for each instrument or
 * instrument configuration.  Override the writeOutput() method to
 * follow a recipe and perform a calculation.
 * Some of the look of the output document is controled from here.
 */
public final class CalculationServlet extends HttpServlet {

    private static final String HELP_URL        = "http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help";
    private static final String TITLE           = "Gemini Integration Time Calculator";

    private static final int MAX_CONTENT_LENGTH = 1000000;  // Max file size 1MB

    /**
     * Called by server when form is submitted.
     */
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * Called by server when form is submitted.
     */
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        ServerInfo.setServerName(request.getServerName());
        ServerInfo.setServerPort(request.getServerPort());
        response.setContentType("text/html");

        final long start = System.currentTimeMillis();

        final Calendar now = Calendar.getInstance();
        log("USER: " + request.getRemoteHost() + " DATE: " + now.getTime() + " SIZE: " + request.getContentLength());
        log("HTML: " + request.getHeader("Referer"));

        final PrintWriter out = response.getWriter();
        openDocument(out);  // Write start of html document

        try {
            if (request.getContentLength() > MAX_CONTENT_LENGTH) {
                log("MAXFILE: " + request.getRemoteHost() + " BYTES: " + request.getContentLength() + " DATE: " + now.getTime());
                throw new Exception("File upload exceeds server limit of " + MAX_CONTENT_LENGTH / 1000000 + " MB. Resubmit with a smaller size. ");
            }

            final String contentType = request.getContentType();
            final ITCRequest c;
            if (contentType != null && contentType.contains("multipart/form-data")) {
                c = ITCRequest.from(new ITCMultiPartParser(request, MAX_CONTENT_LENGTH));
            } else {
                c = ITCRequest.from(request);
            }

            final String instrument = ITCRequest.instrumentName(c);
            out.println("<H2>" + TITLE + "<br>" + instrument + " - " + Version.current + "</H2>");
            out.println("<a href = \"" + HELP_URL + "\"> Click here for help with the results page.</a>");


            writeOutput(c, out);
            closeDocument(out); // Write close of html document

        } catch (Exception e) {
            log("", e);
            closeDocument(out, e);  // close and show exception message

        } finally {
            out.close();
        }


        final long end = System.currentTimeMillis();

        log("CALCTIME: " + (end - start));
    }

    /**
     * Write opening of html document
     */
    private void openDocument(final PrintWriter out) {
        out.println("<HTML><HEAD><TITLE>");
        out.println(TITLE);
        out.println("</TITLE></HEAD>");
        out.println("<BODY text='#000000' bgcolor='#ffffff'>");
    }

    /**
     * Provides HTML output depending on selected instrument as parsed from form request.
     */
    private void writeOutput(final ITCRequest r, final PrintWriter out) {
        final InstrumentDetails ip  = ITCRequest.instrumentParameters(r);
        final ItcParameters p       = ITCRequest.parameters(r, ip);

        final PrinterBase printer;
        if      (ip instanceof AcquisitionCamParameters) printer = new AcqCamPrinter(p, (AcquisitionCamParameters) ip, out);
        else if (ip instanceof Flamingos2Parameters)     printer = new Flamingos2Printer(p, (Flamingos2Parameters) ip, ITCRequest.plotParameters(r), out);
        else if (ip instanceof GmosParameters)           printer = new GmosPrinter(p, (GmosParameters) ip, ITCRequest.plotParameters(r), out);
        else if (ip instanceof GhostParameters)          printer = new GhostPrinter(p, (GhostParameters) ip, ITCRequest.plotParameters(r), out);
        else if (ip instanceof GnirsParameters)          printer = new GnirsPrinter(p, (GnirsParameters) ip, ITCRequest.plotParameters(r), out);
        else if (ip instanceof GsaoiParameters)          printer = new GsaoiPrinter(p, (GsaoiParameters) ip, out);
        else if (ip instanceof MichelleParameters)       printer = new MichellePrinter(p, (MichelleParameters) ip, ITCRequest.plotParameters(r), out);
        else if (ip instanceof NifsParameters)           printer = new NifsPrinter(p, (NifsParameters) ip, ITCRequest.plotParameters(r), out);
        else if (ip instanceof NiriParameters)           printer = new NiriPrinter(p, (NiriParameters) ip, ITCRequest.plotParameters(r), out);
        else if (ip instanceof TRecsParameters)          printer = new TRecsPrinter(p, (TRecsParameters) ip, ITCRequest.plotParameters(r), out);
        else    throw new RuntimeException("Instrument not implemented.");

        printer.writeOutput();
    }

    /**
     * Write closing of html document
     */
    private void closeDocument(final PrintWriter out) {
        out.println("</BODY></HTML>");
    }

    /**
     * This is called when there is an exception in the middle of parsing
     * form data.
     */
    private void closeDocument(final PrintWriter out, final Exception e) {
        out.println("<pre>");
        out.println(e.getMessage() + "<br>");
        out.println("</pre>");
        out.println("</BODY></HTML>");
        out.close();
    }
}
