package jsky.app.ot.ags;

import edu.gemini.pot.sp.ISPObservation;

public interface BagsStatusListener {
    void bagsStatusChanged(final ISPObservation obs, final BagsStatus oldStatus, final BagsStatus newStatus);
}
