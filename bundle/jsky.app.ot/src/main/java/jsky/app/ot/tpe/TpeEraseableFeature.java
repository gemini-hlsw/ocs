package jsky.app.ot.tpe;

/**
 * This is an interface supported by TpeImageFeatures that can erase
 * one or more items (such as target positions).
 */
public interface TpeEraseableFeature {
    /**
     * Erase an item, returning true if successful.
     */
    boolean erase(TpeMouseEvent evt);
}

