package edu.gemini.itc.operation;

import edu.gemini.itc.base.DefaultArraySpectrum;
import edu.gemini.itc.base.ITCConstants;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.shared.ExactCc;
import edu.gemini.shared.util.immutable.ImEither;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import java.util.logging.Logger;

/**
 * The CloudTransmissionVisitor is designed to adjust the SED for
 * clouds in the atmosphere.
 */
public final class CloudTransmissionVisitor {
    private static final String FILENAME = "cloud_trans";
    private CloudTransmissionVisitor() {
    }

    private static final Logger Log = Logger.getLogger( CloudTransmissionVisitor.class.getName() );

    /**
     * Constructs transmission visitor for clouds.
     */
    public static TransmissionElement create(final ImEither<ExactCc, SPSiteQuality.CloudCover> cc) {
        return cc.biFold(exactcc -> {
            if (exactcc.toExtinction() < 0.0) throw new IllegalArgumentException("Exact Cloud Cover must be >= zero magnitudes.");
            final double[][] data = new double[2][2];
            data[0][0] = 300.0;                      // x = wavelength
            data[0][1] = 30000.0;
            data[1][0] = Math.pow(10, exactcc.toExtinction()/-2.5); // y = transmission
            data[1][1] = data[1][0];
            Log.fine(String.format("Exact cloud transmission = %.2f mag = %.4f", exactcc.toExtinction(), data[1][0]));
            final TransmissionElement te;
            te = new TransmissionElement(new DefaultArraySpectrum(data));
            return te;
        }, ccEnum -> new TransmissionElement(ITCConstants.TRANSMISSION_LIB + "/" + FILENAME +
                "_" + ccEnum.sequenceValue() + ITCConstants.DATA_SUFFIX));
    }
}
