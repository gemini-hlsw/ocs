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
 *
 *  2017-12-15: Modified according to the latest read time measurements.
 */
public final class GnirsReadoutTime {
    private static final double DHS_WRITE_TIME = 8.5;

    //Maps from the read mode to an array containing the overhead per coadd per frame
    private static final Map<GNIRSParams.ReadMode, Double> map;

    static{
        Map<ReadMode, Double> tmp = new HashMap<>();
        tmp.put(ReadMode.VERY_BRIGHT, 0.19);
        tmp.put(ReadMode.BRIGHT, 0.69);
        tmp.put(ReadMode.FAINT, 11.14);
        tmp.put(ReadMode.VERY_FAINT, 22.31);
        map = Collections.unmodifiableMap(tmp);
    }

    public static double getDhsWriteTime () {
        return DHS_WRITE_TIME;
    }

    public static double getReadoutOverhead(ReadMode readMode, int coadds) {
        Double overheads= map.get(readMode);
        return overheads * coadds;
    }

    public static double getReadoutOverheadPerCoadd(ReadMode readMode) {
        Double overheads= map.get(readMode);
        return overheads;
    }

}
