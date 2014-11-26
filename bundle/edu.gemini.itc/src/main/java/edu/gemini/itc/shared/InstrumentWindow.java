// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;


import edu.gemini.itc.shared.TransmissionElement;

/**
 *  This is just a basic transmission element for user selectable windows
 *  on instruments.
 */

public class InstrumentWindow extends TransmissionElement {
    String windowName;

    public InstrumentWindow(String resource, String windowName) throws Exception {
        super(resource);
        this.windowName = windowName;

    }

    public String toString() {
        return "User Selectable Window: " + windowName;
    }

}
