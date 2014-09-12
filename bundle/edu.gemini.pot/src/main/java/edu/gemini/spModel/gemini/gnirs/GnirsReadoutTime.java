package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.spModel.gemini.gnirs.GNIRSParams.ReadMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class GnirsReadoutTime
 *
 * @author Nicolas A. Barriga
 *         Date: Oct 21, 2010
 */
public final class GnirsReadoutTime {
    //Maps from the read mode to an array containing the overhead per frame and the overhead per coadd per frame
    private static final Map<GNIRSParams.ReadMode, double[]> map;

    static{
        Map<ReadMode, double[]> tmp = new HashMap<ReadMode, double[]>();
        tmp.put(ReadMode.VERY_BRIGHT, new double[]{0.8, 0.14});
        tmp.put(ReadMode.BRIGHT, new double[]{0.5, 0.7});
        tmp.put(ReadMode.FAINT, new double[]{2.8, 11.0});
        tmp.put(ReadMode.VERY_FAINT, new double[]{5.0, 21.9});
        map = Collections.unmodifiableMap(tmp);
    }


    static double getReadoutOverhead(ReadMode readMode, int coadds) {
        double[] overheads= map.get(readMode);
        return overheads[0] + overheads[1] * coadds;
    }
}
