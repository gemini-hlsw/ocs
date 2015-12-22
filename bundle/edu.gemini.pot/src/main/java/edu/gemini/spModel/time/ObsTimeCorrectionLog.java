package edu.gemini.spModel.time;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioParseException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A log (ordered collection) of {@link ObsTimeCorrection} that should be
 * applied to correct the automatically calculated observing time.
 */
public final class ObsTimeCorrectionLog implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PARAM_SET_NAME = "timeCorrectionLog";

    private List<ObsTimeCorrection> _log = new ArrayList<>();

    public ObsTimeCorrectionLog() {
    }

    public ObsTimeCorrectionLog(ObsTimeCorrectionLog copy) {
        _log = new ArrayList<>(copy._log);
    }

    public ObsTimeCorrectionLog(ParamSet pset) throws PioParseException {
        List<ParamSet> corrections = pset.getParamSets(ObsTimeCorrection.PARAM_SET_NAME);
        if ((corrections == null) || (corrections.size() == 0)) return;

        for (ParamSet otc : corrections) {
            _log.add(new ObsTimeCorrection(otc));
        }
    }

    /**
     * Gets the number of {@link ObsTimeCorrection}s that are in this log.
     */
    public synchronized int size() {
        return _log.size();
    }

    /**
     * Adds the given {@link ObsTimeCorrection} to the log.
     *
     * @param correction correction to add to the list
     */
    public synchronized void add(ObsTimeCorrection correction) {
        _log.add(correction);
        Collections.sort(_log);
    }

    /**
     * Gets the complete log of corrections as an array (which the caller is
     * free to modify).
     *
     * @return corrections to apply, if any; an empty array if there are no
     * corrections
     */
    public synchronized ObsTimeCorrection[] getCorrections() {
        return _log.toArray(ObsTimeCorrection.EMPTY_ARRAY);
    }

    /**
     * Gets the sum of all the corrections with the given ChargeClass
     * as a value in milliseconds.
     */
    public synchronized long sumCorrections(ChargeClass chargeClass) {
        long res = 0;
        for (ObsTimeCorrection cur : _log) {
            if (cur.getChargeClass() == chargeClass) {
               res += cur.getCorrection().getMilliseconds();
            }
        }
        return res;
    }

    /**
     * Sums all the corrections into a single ObsTimeCharges instance.
     */
    public synchronized ObsTimeCharges sumCorrections() {
        long res[] = new long[ChargeClass.values().length];
        for (ObsTimeCorrection cur : _log) {
            ChargeClass cclass = cur.getChargeClass();
            res[cclass.ordinal()] += cur.getCorrection().getMilliseconds();
        }

        ObsTimeCharge[] otcA = new ObsTimeCharge[res.length];
        for (ChargeClass cc : ChargeClass.values()) {
            int i = cc.ordinal();
            otcA[i] = new ObsTimeCharge(res[i], cc);
        }
        return new ObsTimeCharges(otcA);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet res = factory.createParamSet(PARAM_SET_NAME);

        for (ObsTimeCorrection otc : _log) {
            res.addParamSet(otc.toParamSet(factory));
        }

        return res;
    }

    // Can't really do equals and hashCode for mutable objects
    public boolean hasSameLog(ObsTimeCorrectionLog that) {
        if (this == that) return true;
        if (that == null) return false;
        return _log.equals(that._log);
    }
}
