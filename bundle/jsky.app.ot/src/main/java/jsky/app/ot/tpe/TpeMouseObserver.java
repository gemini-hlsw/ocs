package jsky.app.ot.tpe;

/**
 * An interface supported by TpeImageWidget clients that which to
 * be notified for each mouse event.
 *
 * @see TpeImageWidget
 */
public interface TpeMouseObserver {
    /**
     * Notification that a new mouse event has arrived.
     */
    void tpeMouseEvent(TpeImageWidget iw, TpeMouseEvent tme);
}

