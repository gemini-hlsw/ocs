// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: NDFilterWheel.java,v 1.3 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.nici;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;

/**
 * Neutral density color wheel?
 * This class exists so that the client can specify a ND filter number
 * instead of specifying the data file name specifically.
 */
public class NDFilterWheel extends TransmissionElement {
    private static final String FILENAME = Nici.getPrefix() +
            "ndfilt_";
    private String _ndFilter;
    
    /**
     * @param ndFilter Should be one of <ul>
     * <li> clear
     * <li> NDa </li>
     * <li> NDb </li>
     * <li> NDc </li>
     * <li> NDd </li>
     * </ul>
     */
    public NDFilterWheel(String ndFilter, String dir) throws Exception {
        super(dir + FILENAME + ndFilter + Instrument.getSuffix());
        _ndFilter = ndFilter;
    }
    
    public String toString() {
        return "Neutral Density Filter - " + _ndFilter;
    }
}
