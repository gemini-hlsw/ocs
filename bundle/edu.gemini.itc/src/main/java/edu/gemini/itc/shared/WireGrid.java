// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//
package edu.gemini.itc.shared;


/**
 * This is just a basic transmission element for polarimetry wire grid elements
 * on instruments.
 */

public class WireGrid extends TransmissionElement {


    public WireGrid(String resource) throws Exception {
        super(resource);
    }

    public String toString() {
        return "Wire Grid Transmission ";
    }

}
