package edu.gemini.itc.shared;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Image servlet for ITC. The instrument Servlet adds a tag the points to .
 * this servlet for each Image it wants to send the user. This servlet reads
 * the tag, opens the file and sends it to the user using a servlet output
 * stream.
 */
public final class ImageServlet extends HttpServlet {

    private static final Logger Log = Logger.getLogger(ImageServlet.class.getName());

    private static final String IMG = "img";
    private static final String TXT = "txt";

    /**
     * Called by server when an image or a result data file is requested.
     */
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        final String filename = request.getParameter("filename");
        final String type = request.getParameter("type");

        // set the content type of reply
        switch (type) {
            case IMG: response.setContentType("image/png");  break;
            case TXT: response.setContentType("text/plain"); break;
            default:  response.setContentType("text/plain");
        }

        try {
            // copy file to output stream
            ITCImageFileIO.sendFiletoServOut(filename, response.getOutputStream());

        } catch (FileNotFoundException e) {
            Log.log(Level.WARNING, "Unknown file requested: " + filename, e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (Exception e) {
            Log.log(Level.WARNING, "Problem with file: " + filename, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
