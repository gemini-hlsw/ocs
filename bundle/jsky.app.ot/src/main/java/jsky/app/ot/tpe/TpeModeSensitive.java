package jsky.app.ot.tpe;

import edu.gemini.shared.util.immutable.Option;

/**
 * An interface that identifies a TPE element that is sensitive to
 * {@link TpeMode mode} changes.
 */
public interface TpeModeSensitive {

    /**
     * Called whenever the TPE mode is changed.
     *
     * @param mode new mode
     *
     * @param arg optional argument that depends upon the mode; in particular
     * for {@link TpeMode#CREATE}, <code>arg</code> is the
     * {@link TpeCreatableItem} in use
     */
    void handleModeChange(TpeMode mode, Option<Object> arg);
}