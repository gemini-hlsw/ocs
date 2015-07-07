package jsky.app.ot.tpe;

/**
 * An interface supported by TpeImageWidget clients that wish to
 * know when the image info is updated.
 */
public interface TpeImageInfoObserver {
    /**
     * Notify that image info has been updated.
     */
    void imageInfoUpdate(TpeImageWidget iw, TpeImageInfo tii);
}

