package jsky.app.ot.tpe;

import edu.gemini.shared.util.immutable.Option;

/**
 * This interface should be supported by TpeImageFeatures that are
 * "draggable".
 */
public interface TpeDraggableFeature {
    /**
     * Start dragging the object.
     */
    Option<Object> dragStart(TpeMouseEvent evt, TpeImageInfo tii);

    /**
     * Drag to a new location.
     */
    void drag(TpeMouseEvent evt);

    /**
     * Stop dragging.
     */
    void dragStop(TpeMouseEvent evt);

    /**
     * Return true if the mouse is over an active part of this image feature
     * (so that dragging can begin there).
     */
    boolean isMouseOver(TpeMouseEvent evt);
}

