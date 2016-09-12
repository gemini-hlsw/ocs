package jsky.app.ot.tpe;

/**
 * An interface supported by TpeImageWidget clients that which to
 * be notified when the view changes.
 *
 * @see TpeImageWidget
 */
public interface TpeViewObserver {
    /**
     * Notify that the view has changed.
     */
    void tpeViewChange(TpeImageWidget iw);
}
