package jsky.app.ot.ags;

import edu.gemini.pot.sp.SPNodeKey;

@FunctionalInterface
public interface BagsStateListener {
    void bagsStateChanged(final SPNodeKey obsKey, final BagsState oldStatus, final BagsState newStatus);
}
