//
// $
//

package jsky.app.ot.tpe;

/**
 * An interface supported by features that can perform an action when
 * double-clicked in browse mode.
 */
public interface TpeActionableFeature {

    /**
     * Notifies the feature to perform its action.
     */
    void action(TpeMouseEvent tme);
}
