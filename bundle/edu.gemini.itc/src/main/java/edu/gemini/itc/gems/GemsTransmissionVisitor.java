// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: GemsTransmissionVisitor.java,v 1.1 2004/01/12 16:22:25 bwalls Exp $
//
package edu.gemini.itc.gems;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.ITCConstants;

/**
 * The GemsTransmissionVisitor is designed to adjust the SED for the
 * Tranmsission of the Gems optics.
 */
public final class GemsTransmissionVisitor extends TransmissionElement {

    /**
     * The GemsTrans constructor
     */
    public GemsTransmissionVisitor()
            throws Exception {

        setTransmissionSpectrum(Gems.GEMS_LIB + "/" +
                Gems.GEMS_PREFIX +
                Gems.GEMS_TRANSMISSION_FILENAME +
                ITCConstants.DATA_SUFFIX);
    }

    public String toString() {
        return ("GemsTransmissionVisitor");
    }
}
