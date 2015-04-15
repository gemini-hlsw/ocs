package edu.gemini.itc.web.servlets;

import edu.gemini.itc.shared.ServerInfo;
import edu.gemini.itc.web.ITCMultiPartParser;
import edu.gemini.itc.web.ITCRequest;

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
public abstract class ITCServlet extends HttpServlet {

    public static int MAX_CONTENT_LENGTH = 1000000;  // Max file size 1MB

    public ITCServlet() {
        super();
    }

    /**
     * Returns a title
     */
    public abstract String getTitle();

    /**
     * Returns version of the servlet.
     */
    public abstract String getVersion();

    /**
     * Returns the Instrument
     */
    public abstract String getInst();

    /**
     * Subclasses supply the body content for the html document.
     */
    public abstract void writeOutput(ITCRequest mpp, PrintWriter out);

    /**
     * Called by server when form is submitted.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * Called by server when form is submitted.
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

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
    protected void openDocument(PrintWriter out) {
        out.println("<HTML><HEAD><TITLE>");
        out.println(getTitle() + " " + getVersion());
        out.println("</TITLE></HEAD>");
        out.println("<BODY text='#000000' bgcolor='#ffffff'>");
        out.println("<H2>" + getTitle() + "<br>" + getInst() + " version " + getVersion() + "</H2>");
    }

    /**
     * Write closing of html document
     */
    protected void closeDocument(PrintWriter out) {
        out.println("</BODY></HTML>");
    }

    /**
     * This is called when there is an exception in the middle of parsing
     * form data.
     */
    protected void closeDocument(PrintWriter out, Exception e) {
        out.println("<pre>");
        out.println(e.getMessage() + "<br>");
        out.println("</pre>");
        out.println("</BODY></HTML>");
        out.close();
    }
}
