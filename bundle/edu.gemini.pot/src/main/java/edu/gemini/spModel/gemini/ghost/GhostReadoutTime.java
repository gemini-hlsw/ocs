/**
 * $Id: GhostsReadoutTime.java 38186 2011-10-24 13:21:33Z swalker $
 */

package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.config2.Config;

/** Maps Ghost instrument parameters to readout times */
public class GhostReadoutTime {

    /* readRate -> SLW: 10. us/pix  MED: 5 us/pix  FST: 2 us/pix
       formula  -> rows*ptime*binx/2 + (binn/(binx*biny*4.))*readRate
                   rows=6144   binn=6144*6160  ptime=300
     */
    private static double calcReadOut(int binx, int biny, int readRate) {
        return 6144*300*binx/2 + (37847040/(binx*biny*4.))*readRate;
    }



    /**
     * Return the amount of time it takes in seconds to readout an image, based on the
     * configuration in the sequence and any custom ROI settings.
     *
     * @param config the current configuration
     */

    public static double getReadoutOverhead(Config config) {
        final GhostReadNoiseGain readMode                = (GhostReadNoiseGain) config.getItemValue(null);
        // I understand RED is the limiting factor
        final GhostBinning              bin        = (GhostBinning) config.getItemValue(Ghost.RED_BINNING_KEY());
        return calcReadOut(bin.getSpectralBinning(), bin.getSpectralBinning(), readMode.getReadRate())/1000000.0;
    }

}
