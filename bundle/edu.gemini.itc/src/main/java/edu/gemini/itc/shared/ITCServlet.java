// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

// $Id: ITCServlet.java,v 1.8 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

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
    public abstract void writeOutput(HttpServletRequest req, PrintWriter out)
            throws Exception;

    /**
     * Called by server when form is submitted.
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * Called by server when form is submitted.
     */
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
            throws ServletException, IOException {

//if (request.getContentLength()>MAX_CONTENT_LENGTH){
        //response.sendError(response.SC_REQUEST_ENTITY_TOO_LARGE );
//response.setContentType("text/html");
//PrintWriter out = response.getWriter();
//throw new IOException("File too large");
//response.sendRedirect("http://www.gemini.edu/");
//return;
//} else {
//

        ServerInfo.setServerName(request.getServerName());
        ServerInfo.setServerPort(request.getServerPort());
        //response.setHeader("Cache-Control", "no-cache");
        response.setContentType("text/html");
        //////////////////////////////////////////////////////////////////////////////////
        //Enumeration enum = request.getHeaderNames();
        //String key;
        //while (enum.hasMoreElements()) {
        //  	key = (String) enum.nextElement();
        //	log(key + " = " + request.getHeader(key));
        //	}
        /////////////////////////////////////////////////////////////////////////////////

        long start = System.currentTimeMillis();

        Calendar now = Calendar.getInstance();
        //System.out.println(now.getTime());
        log("USER: " + request.getRemoteHost() + " DATE: " + now.getTime() + " SIZE: " + request.getContentLength());
        log("HTML: " + request.getHeader("Referer"));
        //if (request.getProtocol().compareTo("HTTP/1.1") == 0) System.out.println("HTTP/1.1");
        //response.setHeader("Last-Modified", new Long(now.getTime().getTime()).toString());
        //response.setHeader("Cache-Control", "no-cache");
        //response.setHeader("Expires","Tues, 01 Jan 2003 00:00:00 GMT");
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

            writeOutput(request, out);  // perform calculation and write results


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
//}
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
