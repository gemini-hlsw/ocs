// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
// $Id: ColorFilter.java,v 1.3 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.nici;

import java.util.HashMap;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;

/**
 * This class represents the transmission of a color filter on the
 * acquisition camera.
 */
public final class ColorFilter extends TransmissionElement {
    private static final String FILENAME = Nici.getPrefix() +
            "colfilt_";
    
    /** These are the supported filter wavebands */
    public static final String[] SUPPORTED_FILTERS = {"B", "V", "R", "I"};
    
    // Effective wavelength in nm for each color.
    // This is stored in this class rather than the data file because
    // storing this extra number in the data file means a spectrum can
    // no longer be constructed from the file.  That would complicate things.
    private static final int[] EFFECTIVE_WAVELENGTH = {420, 550, 670, 870};
    
    // maps waveband name to effective wavelength
    private static HashMap _bandNameToWavelengthMap;
    
    static {
        _bandNameToWavelengthMap = new HashMap();
        for (int i = 0; i < SUPPORTED_FILTERS.length; ++i) {
            _bandNameToWavelengthMap.put(SUPPORTED_FILTERS[i],
                    new Integer(EFFECTIVE_WAVELENGTH[i]));
        }
    }
    
    // The waveband of this filter.  Its only identifying information.
    private String _waveband; // U, V, ...
    
    /**
     * Constructs a color filter.
     * @param color 1=red, no other colors supported at this time
     */
    public ColorFilter(String band, String directory) throws Exception {
        band = band.toUpperCase();
        
        if (_bandNameToWavelengthMap.get(band) == null) {
            throw new Exception("Color Filter band " + band + " not supported");
        }
        
        _waveband = band;
        
        setTransmissionSpectrum(directory + Nici.getPrefix() +
                FILENAME + band + Instrument.getSuffix());
    }
    
    /**
     * Returns the effective observing wavelength in nm.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        return getEffectiveWavelength(_waveband);
    }
    
    /**
     * Returns the effective observing wavelength (in nm) given waveband.
     * If waveband is not supported, returns -1.
     */
    public static int getEffectiveWavelength(String waveband) {
        waveband = waveband.toUpperCase();
        Integer wavelength = (Integer) _bandNameToWavelengthMap.get(waveband);
        if (wavelength == null) return -1;
        return wavelength.intValue();
    }
    
    public String toString() {
        return "Filter - " + _waveband;
    }
}
