// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;

import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class Dichroic extends TransmissionElement {
    //private static final String FILENAME =
    //        "fixed_optics" + Instrument.getSuffix();
    private String _position;

    public Dichroic(String directory, String position, String channel) throws Exception {
        super(directory +"dichroic_" + position +"_"+ channel + Instrument.getSuffix());
	_position = position;
	System.out.println("Using dichroic: dichroic_"+position+"_"+channel+"_"+Instrument.getSuffix());

    }

    public String toString() {
        return "Dichroic Position: " + _position;
    }

}
