// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//

package edu.gemini.itc.gmos;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import java.util.Enumeration;

import edu.gemini.itc.shared.ITCServlet;
import edu.gemini.itc.shared.Recipe;
import edu.gemini.itc.shared.ITCMultiPartParser;

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
public final class ITCgmosServlet
        extends ITCServlet {
    public static final String VERSION = "5.0";
    public static final String TITLE = "Gemini Integration Time Calculator";
    public static final String INSTRUMENT = "GMOS";

    public ITCgmosServlet() {
		super ();
    }
    
    /** Returns a title */
    public String getTitle() {
        return TITLE;
    }

    /** Returns a version of this servlet */
    public String getVersion() {
        return VERSION;
    }

    /** Returns the Instrument name*/
    public String getInst() {
        return INSTRUMENT;
    }

    /**
     * Describes the purpose of the servlet.
     * Used by Java Web Server Administration Tool.
     */
    public String getServletInfo() {
        return getTitle() + " " + getVersion() + " - ITCgmosServlet accepts form data and performs ITC calculation for Gmos.";
    }

    /** Supply the body content for the html document. */
    public void writeOutput(HttpServletRequest r, PrintWriter out)
            throws Exception {
        // Construct recipe from the request.
        // Pass in same PrintWriter instead of getting another one from
        // the servlet request in case you get a different one each
        // time.  HTML document was already started with one PrintWriter.
        out.println("<a href = \"http://www.gemini.edu/sciops/instruments/integration-time-calculators/itc-help\"> Click here for help with the results page.</a>");

//      Recipe recipe = new GmosRecipe(r, out); // parses form data

        Recipe recipe;

        if (r.getContentType().startsWith("multipart/form-data")) {
            //System.out.println("Max: " + MAX_CONTENT_LENGTH);
            recipe = new GmosRecipe(new ITCMultiPartParser(r, MAX_CONTENT_LENGTH), out);
        } else
            recipe = new GmosRecipe(r, out); // parses form data


        // Perform calculation, write the output to the web page.
        recipe.writeOutput();
    }
}
