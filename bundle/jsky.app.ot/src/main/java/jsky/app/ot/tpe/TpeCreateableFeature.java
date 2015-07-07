package jsky.app.ot.tpe;

/**
 * This is an interface supported by TpeImageFeatures that can create
 * multiple items (such as target positions).
 */
public interface TpeCreateableFeature {
    /**
     * Return the label that should be on the create button.
     */
    TpeCreateableItem[] getCreateableItems();
}

