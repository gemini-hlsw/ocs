// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
//
package edu.gemini.itc.nifs;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;

/**
 * This represents the transmission of the optics for the NIFS Pickoff mirror when IFU is not used.  Probably wont need this.
 */
public class NifsPickoffMirror extends TransmissionElement {
    
    public NifsPickoffMirror(String directory, String mirrorTransmissionFile) throws Exception {
        super(directory + "/" + Nifs.getPrefix() + mirrorTransmissionFile + Instrument.getSuffix());
    }
    
    public String toString() { return "(mirror) no prism"; }
}

