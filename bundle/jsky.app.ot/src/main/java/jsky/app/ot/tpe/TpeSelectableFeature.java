package jsky.app.ot.tpe;

/**
 * This is an interface supported by TpeImageFeatures that support
 * selecting individual items (such as target positions).
 */
public interface TpeSelectableFeature {
    /**
     * Select an item, returning it if successful.  Return null if nothing
     * is selected.
     */
    Object select(TpeMouseEvent evt);
}

