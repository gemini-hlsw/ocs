// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: CloudTransmissionVisitor.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.ITCConstants;

/**
 * The CloudTransmissionVisitor is designed to adjust the SED for
 * clouds in the atmosphere.
 */
public final class CloudTransmissionVisitor extends TransmissionElement {
    private static final String FILENAME = "cloud_trans";

    /**
     * Constructs transmission visitor for clouds.
     */
    public CloudTransmissionVisitor(int skyTransparencyCloud)
            throws Exception {
        setTransmissionSpectrum(ITCConstants.TRANSMISSION_LIB + "/" + FILENAME +
                                skyTransparencyCloud + ITCConstants.DATA_SUFFIX);
    }

    public String toString() {
        return ("CloudTransmission");
    }
}
