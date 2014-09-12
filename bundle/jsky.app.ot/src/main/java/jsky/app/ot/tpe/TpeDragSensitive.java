//
// $
//

package jsky.app.ot.tpe;

import edu.gemini.spModel.obs.context.ObsContext;

/**
 * An interface that identifies a TPE element that is sensitive to dragging.
 * For example, it might display itself differently when dragging a target.
 */
public interface TpeDragSensitive {
    /**
     * Called when an item is dragged in the TPE.
     *
     * @param dragObject object being dragged
     * @param context observation context
     */
    void handleDragStarted(Object dragObject, ObsContext context);

    /**
     * Called when the user stops dragging an item in them TPE.
     *
     * @param context observation context
     */
    void handleDragStopped(ObsContext context);
}
