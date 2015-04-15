package edu.gemini.itc.web;

import edu.gemini.itc.shared.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Scanner;


/*
 * MultipartTestServlet.java
 */

public class MultipartTestServlet extends HttpServlet {

    /**
     * Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

    }

    /**
     * Destroys the servlet.
     */
    public void destroy() {

    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request  servlet request
     * @param response servlet response
     */
    protected void processRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, java.io.IOException {

        response.setContentType("text/html");
        java.io.PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Servlet</title>");
        out.println("</head>");
        out.println("<body>");

        if (request.getContentLength() < 1000000) {
            try {
                final ITCMultiPartParser parser = new ITCMultiPartParser(request, MAX_CONTENT_LENGTH);
                final ITCRequest r = ITCRequest.from(parser);
                ObservationDetails odp = ITCRequest.observationParameters(r);
                ObservingConditions ocp = ITCRequest.obsConditionParameters(r);
                SourceDefinition sdp = ITCRequest.sourceDefinitionParameters(r);
                TelescopeDetails tp = ITCRequest.teleParameters(r);
                AcquisitionCamParameters acp = ITCRequest.acqCamParameters(r);

                out.println(odp.toString());
                out.println("<br>");
                out.println(ocp.toString());
                out.println("<br>");
                out.println(sdp.toString());
                out.println("<br>");
                out.println(tp.toString());
                out.println("<br>");
                out.println(acp.toString());

                String txtFileName = "";
                final Iterator it = parser.getFileNames();
                while (it.hasNext()) {
                    txtFileName = (String) it.next();
                    System.out.println(parser.getTextFile(txtFileName) + "<br>");
                }
                //Use this Code in Sed Factory to create a Textfile Reader for the String.
                try (final Scanner scan = DatFile.scan(sdp.getUserDefinedSpectrum())) {
                    while (scan.hasNext()) {
                        System.out.println("x: " + scan.nextDouble() + "\ny: " + scan.nextDouble());
                    }
                }

            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }

        } else {
            out.println("ERROR: File size (" + (new Integer(request.getContentLength())).doubleValue() / MAX_CONTENT_LENGTH +
                    " MB)exceeds 1MB limit. Please resubmit with at smaller file.<br>");
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
                while (in.readLine() != null) { /* intentionally empty */}
            }
        }


        out.println("</body>");
        out.println("</html>");

        out.close();
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "MultiPartTestServlet";
    }

    private static int MAX_CONTENT_LENGTH = 1000000;  // Max file size 1M
}
