// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: WaterTransmissionVisitor.java,v 1.4 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.ITCConstants;

/**
 * The WaterTransmissionVisitor is designed to adjust the SED for
 * water in the atmosphere.
 */
public final class WaterTransmissionVisitor extends TransmissionElement {
    private static final String FILENAME = "water_trans";
    //private static final String NIRI_FILENAME = "nearIR_trans_";

    /**
     * Constructs transmission visitor for water vapor.
     */
    public WaterTransmissionVisitor(int skyTransparencyWater)
            throws Exception {
        setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" + FILENAME +
                                skyTransparencyWater + ITCConstants.DATA_SUFFIX);
    }

    public WaterTransmissionVisitor(int skyTransparencyWater, double airMass,
                                    String file_name, String site, String wavelenRange)
            throws Exception {
        //System.out.println ("SkyValue:" + skyTransparencyWater +"Airmass:" +
        // airMass);
                
           String _airmassCategory;
           String _transmissionCategory = "";

                
        if (wavelenRange.equals(ITCConstants.VISIBLE))  {      
        if (skyTransparencyWater == 1) {
            if (airMass <= 1.25) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "20_" + "10" +
                                        ITCConstants.DATA_SUFFIX);
            } else if (airMass > 1.25 && airMass <= 1.75) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "20_" + "15" +
                                        ITCConstants.DATA_SUFFIX);
            } else if (airMass > 1.75) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "20_" + "20" +
                                        ITCConstants.DATA_SUFFIX);
            }
        } else if (skyTransparencyWater == 2) {
            if (airMass <= 1.25) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "50_" + "10" +
                                        ITCConstants.DATA_SUFFIX);
            } else if (airMass > 1.25 && airMass <= 1.75) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "50_" + "15" +
                                        ITCConstants.DATA_SUFFIX);
            } else if (airMass > 1.75) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "50_" + "20" +
                                        ITCConstants.DATA_SUFFIX);
            }
        } else if (skyTransparencyWater == 3) {
            if (airMass <= 1.25) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "80_" + "10" +
                                        ITCConstants.DATA_SUFFIX);
            } else if (airMass > 1.25 && airMass <= 1.75) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "80_" + "15" +
                                        ITCConstants.DATA_SUFFIX);
            } else if (airMass > 1.75) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "80_" + "20" +
                                        ITCConstants.DATA_SUFFIX);
            }
        } else if (skyTransparencyWater == 4) {
            if (airMass <= 1.25) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "100_" + "10" +
                                        ITCConstants.DATA_SUFFIX);
            } else if (airMass > 1.25 && airMass <= 1.75) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "100_" + "15" +
                                        ITCConstants.DATA_SUFFIX);
            } else if (airMass > 1.75) {
                setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" +
                                        file_name + "100_" + "20" +
                                        ITCConstants.DATA_SUFFIX);
            }
        }
        } else {
            if (airMass <= 1.25)
                _airmassCategory="10";
            else if (airMass > 1.25 && airMass <= 1.75)
                _airmassCategory="15";
            else
                _airmassCategory="20";
            
            if (skyTransparencyWater == 1) 
                _transmissionCategory="20_";
            else if (skyTransparencyWater == 2)
                _transmissionCategory="50_";
            else if (skyTransparencyWater == 3)
                _transmissionCategory="80_";
            else if (skyTransparencyWater == 4)
                _transmissionCategory="100_";
            
            setTransmissionSpectrum("/HI-Res/" + site + wavelenRange + ITCConstants.TRANSMISSION_LIB + "/"
               +file_name + _transmissionCategory + _airmassCategory + ITCConstants.DATA_SUFFIX);
        }
        
        

    }

    public String toString() {
        return ("WaterTransmission");
    }
}
