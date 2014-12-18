// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: TelescopeTransmissionVisitor.java,v 1.3 2004/01/12 16:53:55 bwalls Exp $
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.ITCConstants;

/**
 * The TelescopeTransmissionVisitor is designed to adjust the SED for the
 * Telesope Tranmsission.
 */
public final class TelescopeTransmissionVisitor extends TransmissionElement {
    // These constants define the different mirror surfaces used
    public static final String ALUMINUM = "aluminium";
    public static final String SILVER    = "silver";
    public static final String UP = "up";
    public static final String UP_GS = "upGS";
    public static final String SIDE = "side";
    public static final String SIDE_GS = "sideGS";
    
    private static final String _COATING = "_coating_";
    
    /**
     * The TelTrans constructor takes two arguments: one detailing what
     * type of coating is used, and the other detailing how many mirrors
     * should be used.
     */
    public TelescopeTransmissionVisitor(String coating, String issPort)
    throws Exception {
        String fileName;
        if (coating.equals(ALUMINUM)) {
            fileName = "al" + _COATING;
        } else if (coating.equals(SILVER)) {
            fileName = "ag" + _COATING;
        } else {
            throw new Exception("Unknown mirror material: " + coating);
        }
        
        if (issPort.equals(UP)) {
            fileName += UP;
        } else if (issPort.equals(SIDE)) {
            fileName += SIDE;
        } else if (issPort.equals(UP_GS)||issPort.equals(SIDE_GS)) {
            fileName = "";
        } else {
            throw new Exception("Unknown iss port: " + issPort);
        }
        
        //if GS use new mirror transmision files
        
        if (issPort.equals(UP_GS)&&coating.equals("silver")){
            fileName = ITCConstants.GS_TELESCOPE_TRANSMISSION_FILENAME_BASE;
            fileName += "_ag1_al1";
        } else if (issPort.equals(UP_GS)&&coating.equals("aluminium")){
            fileName = ITCConstants.GS_TELESCOPE_TRANSMISSION_FILENAME_BASE;
            fileName += "_al2";
        } else if (issPort.equals(SIDE_GS)&&coating.equals("silver")){
            fileName = ITCConstants.GS_TELESCOPE_TRANSMISSION_FILENAME_BASE;
            fileName += "_ag1_al2";
        } else if (issPort.equals(SIDE_GS)&&coating.equals("aluminium")){
            fileName = ITCConstants.GS_TELESCOPE_TRANSMISSION_FILENAME_BASE;
            fileName += "_al3";
        }
        
        setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" + fileName +
        ITCConstants.DATA_SUFFIX);
    }
    
    public String toString() {
        return ("TelescopeTransmission");
    }
}
