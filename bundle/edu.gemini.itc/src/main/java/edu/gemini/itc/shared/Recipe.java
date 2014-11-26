// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: Recipe.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

import java.io.PrintWriter;

/**
 * Interface for ITC recipes.
 * By convention a recipe constructor should take as arguments a
 * HttpServletRequest containing the form data and a PrintWriter
 * to which the calculation results are written.
 */
public interface Recipe {
    /**
     * Writes results of the recipe calculation.
     * Format should be suitable for inclusion inside a web page.
     */
    void writeOutput() throws Exception;
}
