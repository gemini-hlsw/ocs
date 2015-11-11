package jsky.app.ot.ags;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.Option;

@FunctionalInterface
public interface BagsStatusListener {
    void bagsStatusChanged(final ISPObservation obs, final Option<BagsStatus> oldStatus, final Option<BagsStatus> newStatus);
}
