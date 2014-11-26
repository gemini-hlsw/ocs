// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: AltairBackgroundVisitor.java,v 1.1 2004/01/12 16:22:25 bwalls Exp $
//
package edu.gemini.itc.altair;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.ITCConstants;

/**
 * The AltairBackgroundVisitor class is designed to adjust the SED for the
 * background given off by altair.
 */
public class AltairBackgroundVisitor implements SampledSpectrumVisitor {
    
    private String _filename_base;
    
    private ArraySpectrum _altairBack = null;
    
    /**
     * Constructs AltairBackgroundVisitor.
     */
    public AltairBackgroundVisitor() throws Exception {
        
        _altairBack = new DefaultArraySpectrum(
                Altair.ALTAIR_LIB + "/" +
                Altair.ALTAIR_PREFIX +
                Altair.ALTAIR_BACKGROUND_FILENAME +
                ITCConstants.DATA_SUFFIX);
    }
    
    
    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) throws Exception {

	System.out.println("Applying Altair background file : "+Altair.ALTAIR_LIB + "/" + Altair.ALTAIR_PREFIX + Altair.ALTAIR_BACKGROUND_FILENAME + ITCConstants.DATA_SUFFIX);

        for (int i=0; i < sed.getLength(); i++) {
            sed.setY(i, _altairBack.getY(sed.getX(i))+sed.getY(i));
        }
    }
    
    
    public String toString() {
        return "AltairBackgroundVisitor ";
    }
}
