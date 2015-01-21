// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.TransmissionElement;

/**
 * This represents the transmission of the optics native to the X-Disp Prism.
 */
public class XDispersingPrism extends TransmissionElement {

    public XDispersingPrism(String directory, String xDispTransmissionFile) throws Exception {
        super(directory + "/" + Gnirs.getPrefix() + xDispTransmissionFile + Instrument.getSuffix());
    }

    public String toString() {
        return "Cross-Dispersing Prism";
    }
}

