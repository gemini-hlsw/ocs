//
// $Id: NightObsTimes.java 6811 2005-12-06 12:51:58Z shane $
//

package edu.gemini.spModel.obs;

import edu.gemini.skycalc.ObservingNight;
import edu.gemini.spModel.time.ObsTimes;

import java.io.Serializable;

/**
 * Pairing of a {@link ObservingNight night} with the {@link ObsTimes observing
 * times} associated with the night.
 */
public class NightObsTimes implements Serializable {
    private ObservingNight _night;
    private ObsTimes _times;

    public NightObsTimes(ObservingNight night, ObsTimes times) {
        _night = night;
        _times = times;
    }

    public ObservingNight getNight() {
        return _night;
    }

    public ObsTimes getTimes() {
        return _times;
    }
}
