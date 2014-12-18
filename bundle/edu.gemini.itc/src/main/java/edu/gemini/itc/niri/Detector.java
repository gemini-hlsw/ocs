// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: Detector.java,v 1.3 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.niri;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;

@Deprecated
public class Detector extends TransmissionElement {
    private static final String FILENAME = "detector" + Instrument.getSuffix();

    public Detector(String directory) throws Exception {
        super(directory + Niri.INSTR_PREFIX + FILENAME);
    }

    public String toString() {
        return "Detector - 1024x1024-pixel ALADDIN InSb array";
    }
}
