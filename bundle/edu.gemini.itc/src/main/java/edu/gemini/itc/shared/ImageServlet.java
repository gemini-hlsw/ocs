// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Image servlet for ITC. The instrument Servlet adds a tag the points to .
 * this servlet for each Image it wants to send the user. This servlet reads
 * the tag, opens the file and sends it to the user using a servlet output
 * stream.
 */
public final class ImageServlet extends HttpServlet {
    private final String IMG = "img";
    private final String TXT = "txt";

    private String _filename;
    private String _type;
    //private HttpSession _sessionObject = null;

    //public ITCImageFileIO ServFileIO = new ITCImageFileIO();

    /**
     * Called by server when an image is requested.
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException {

        ITCImageFileIO ServFileIO = new ITCImageFileIO();
        _filename = request.getParameter("filename");
        _type = request.getParameter("type");
        //_sessionObject = request.getSession(true);
        //System.out.println(" Session is over after" +_sessionObject.getMaxInactiveInterval());
        if (_type.equals(IMG)) {
            // set the content type to image png
            response.setContentType("image/png");
            //response.setHeader("Cache-Control", "no-cache");
            //System.out.println("Image");
        } else if (_type.equals(TXT)) {
            response.setContentType("text/plain");
            //System.out.println("txt");
        } else {
            response.setContentType("text/plain");
            //System.out.println("other");
        }


        //create a Stream to pass the image through
        ServletOutputStream out = response.getOutputStream();

        ServFileIO.sendFiletoServOut(_filename, out);
        //ServFileIO.sendFiletoServOut((Image)_sessionObject.getAttribute(_filename),out);

        out.flush();
        out.close();
    }

}
