// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: F6Optics.java,v 1.4 2004/01/12 16:46:25 bwalls Exp $
//
package edu.gemini.itc.niri;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.TransmissionElement;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class F6Optics extends TransmissionElement {
    private static final String FILENAME = Niri.getPrefix() +
            "f6_optics" + Instrument.getSuffix();

    public F6Optics(String directory) throws Exception {
        super(directory + FILENAME);
    }

    public String toString() {// return "F6Optics - " + FILENAME; }
        return "Camera: f6";
    }
}
