// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: GemsBackgroundVisitor.java,v 1.1 2004/01/12 16:22:25 bwalls Exp $
//
package edu.gemini.itc.gems;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.ITCConstants;

/**
 * The GemsBackgroundVisitor class is designed to adjust the SED for the
 * background given off by gems.
 */
public class GemsBackgroundVisitor implements SampledSpectrumVisitor {

    private ArraySpectrum _gemsBack = null;

    /**
     * Constructs GemsBackgroundVisitor.
     */
    public GemsBackgroundVisitor() throws Exception {

        _gemsBack = new DefaultArraySpectrum(
                Gems.GEMS_LIB + "/" +
                        Gems.GEMS_PREFIX +
                        Gems.GEMS_BACKGROUND_FILENAME +
                        ITCConstants.DATA_SUFFIX);
    }


    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) throws Exception {
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i, _gemsBack.getY(sed.getX(i)) + sed.getY(i));
        }
    }


    public String toString() {
        return "GemsBackgroundVisitor ";
    }
}
