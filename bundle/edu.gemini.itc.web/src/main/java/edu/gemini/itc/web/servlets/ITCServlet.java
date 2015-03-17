package edu.gemini.itc.web.servlets;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ServerInfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    public abstract void writeOutput(ITCMultiPartParser mpp, PrintWriter out);

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

        long start = System.currentTimeMillis();

        Calendar now = Calendar.getInstance();
        log("USER: " + request.getRemoteHost() + " DATE: " + now.getTime() + " SIZE: " + request.getContentLength());
        log("HTML: " + request.getHeader("Referer"));
        PrintWriter out = response.getWriter();

        openDocument(out);  // Write start of html document

        try {
            //System.out.println("content: " + request.getContentLength());
            if (request.getContentLength() > MAX_CONTENT_LENGTH) {
                log("MAXFILE: " + request.getRemoteHost() + " BYTES: " + request.getContentLength() + " DATE: " + now.getTime());
                BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
                try {
                    in.skip(request.getContentLength());
                } catch (java.io.IOException e) {
                }

                throw new Exception("File upload exceeds server limit of " + MAX_CONTENT_LENGTH / 1000000 + " MB. Resubmit with a smaller size. ");
            }

            // TODO: this is going to be the future version:
//            final String contentType = request.getContentType();
//            final ParameterContainer c;
//            switch (contentType) {
//                case "multipart/form-data":
//                    c = ParameterContainer$.MODULE$.from(new ITCMultiPartParser(request, MAX_CONTENT_LENGTH));
//                    break;
//                default:
//                    c = ParameterContainer$.MODULE$.from(request);
//            }
//            writeOutput(c, out);

            // TODO: for now we only support multipart requests; ALL forms in the current web app are multipart!:
            final ITCMultiPartParser mpp = new ITCMultiPartParser(request, MAX_CONTENT_LENGTH);
            writeOutput(mpp, out);

        } catch (Exception e) {
            e.printStackTrace();
            closeDocument(out, e);  // close and show exception message
            System.gc();
            return;
        }

        closeDocument(out); // Write close of html document

        out.close();

        long end = System.currentTimeMillis();

        log("CALCTIME: " + (end - start));
        System.gc();
    }

    /**
     * Write opening of html document
     */
    protected void openDocument(PrintWriter out) {
        out.println("<HTML><HEAD><TITLE>");
        out.println(getTitle() + " " + getVersion());
        out.println("</TITLE></HEAD>");
        out.println("<BODY text='#000000' bgcolor='#ffffff'>");
        out.println("<H2>" + getTitle() +
                //" (DEVELOPMENT SERVER)" +
                "<br>" + getInst() + " version " + getVersion() + "</H2>");
    }

    /**
     * Write closing of html document
     */
    protected void closeDocument(PrintWriter out) {
        out.println("</BODY></HTML>");
    }

    /**
     * This is called when there is an exception in the middle of parsing
     * form data.  It prints diagnostic message and closes out the
     * html document.
     */
    protected void closeDocument(PrintWriter out, Exception e) {
        out.println("<pre>");
        out.println(e.getMessage() + "<br>");

        // temporary for debugging
        //out.println("<p><hr>Debugging<br>");
        log("Exception!!: " + e.getMessage() + e.getCause());
        //e.printStackTrace(out);

        // close out html docuemnt
        out.println("</pre>");
        out.println("</BODY></HTML>");
        out.close();
    }
}
