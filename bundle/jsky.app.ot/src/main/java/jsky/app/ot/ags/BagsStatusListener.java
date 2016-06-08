package jsky.app.ot.ags;

import edu.gemini.pot.sp.SPNodeKey;

@FunctionalInterface
public interface BagsStatusListener {
    void bagsStatusChanged(final SPNodeKey obsKey, final BagsState oldStatus, final BagsState newStatus);
}
