/**
 * $Id: GhostsReadoutTime.java 38186 2011-10-24 13:21:33Z swalker $
 */

package edu.gemini.spModel.gemini.ghost;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.ghost.GhostType.*;
import edu.gemini.spModel.gemini.ghost.GhostType.DetectorManufacturer;

import java.util.HashMap;
import java.util.Map;

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
        final GhostType.ReadMode readMode                = (GhostType.ReadMode) config.getItemValue(GhostType.ReadMode.KEY);
        final GhostType.DetectorManufacturer detMan      = (GhostType.DetectorManufacturer) config.getItemValue(GhostType.DetectorManufacturer.KEY);
        final GhostType.Binning              xBin        = (GhostType.Binning) config.getItemValue(InstGhost.X_BIN_KEY);
        final GhostType.Binning              yBin        = (GhostType.Binning) config.getItemValue(InstGhost.Y_BIN_KEY);
        return calcReadOut(xBin.getValue(),yBin.getValue(), readMode.getReadRate())/1000000.0;
        /*System.out.println("readmode displayValue" + readMode.displayValue());
        System.out.println("detMan value" + detMan.displayValue() + "  " + detMan.getManufacter());
        System.out.println("bining, x: " + xBin.getValue() + " y: " + yBin.getValue());
        final GhostReadoutKey key = new GhostReadoutKey(readMode, yBin.getValue(), xBin.getValue(), detMan);
        final Double d = map.get(key);
         */

    }

}
