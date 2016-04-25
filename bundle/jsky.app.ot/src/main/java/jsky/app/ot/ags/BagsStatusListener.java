package jsky.app.ot.ags;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.shared.util.immutable.Option;

@FunctionalInterface
public interface BagsStatusListener {
    void bagsStatusChanged(final SPNodeKey key, final Option<BagsStatus> oldStatus, final Option<BagsStatus> newStatus);
}
