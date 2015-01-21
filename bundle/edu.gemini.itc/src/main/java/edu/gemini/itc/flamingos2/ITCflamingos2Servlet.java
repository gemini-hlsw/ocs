// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: ITCacqCamServlet.java,v 1.7 2003/11/21 14:31:02 shane Exp $
//

package edu.gemini.itc.flamingos2;

import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCServlet;
import edu.gemini.itc.shared.Recipe;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;

/**
 * This servlet accepts form data from the ITC html document.
 * <br></br>
 * Multithreading issues:
 * Normally a servlet could be called on multiple threads to process
 * simultaneous requests.  We could either take care to synchronize
 * properly or use the SingleThreadModel interface.
 * SingleThreadModel is a marker interface (i.e. has no methods) that tells
 * the server to use a separate instance of this servlet for each
 * request.  Use this for now.
 * Note that since this servlet is single threaded, any other
 * servlet wanting to call its methods should make an HTTP request
 * rather than calling the methods directly.
 */
public final class ITCflamingos2Servlet
        extends ITCServlet {
    public static final String VERSION = "1.0";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "Flamingos-2";

    public ITCflamingos2Servlet() {
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
        return getTitle() + " " + getVersion() + " - ITCflamingos2Servlet accepts form data and performs ITC calculation for Flamingos-2.";
    }

    /**
     * Supply the body content for the html document.
     */
    public void writeOutput(HttpServletRequest r, PrintWriter out)
            throws Exception {
        // Construct recipe from the request.
        // Pass in same PrintWriter instead of getting another one from
        // the servlet request in case you get a different one each
        // time.  HTML document was already started with one PrintWriter.
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");

//      Recipe recipe = new AcqCamRecipe(r, out); // parses form data


        Recipe recipe;

        if (r.getContentType().startsWith("multipart/form-data")) {
            recipe = new Flamingos2Recipe(new ITCMultiPartParser(r, MAX_CONTENT_LENGTH), out);
        } else
            recipe = new Flamingos2Recipe(r, out); // parses form data

        // Perform calculation, write the output to the web page.
        recipe.writeOutput();
    }
}
