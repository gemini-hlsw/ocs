// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: FixedOptics.java,v 1.3 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.niri;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;

/**
 * This represents the transmission of the optics native to the camera.
 */
@Deprecated
public class FixedOptics extends TransmissionElement {
    private static final String FILENAME = Niri.getPrefix() +
            "fixed_optics" + Instrument.getSuffix();

    public FixedOptics(String directory) throws Exception {
        super(directory + FILENAME);

    }

    public String toString() {// return "FixedOptics - " + FILENAME; }
        return "Fixed Optics";
    }

}
