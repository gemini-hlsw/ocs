package jsky.app.ot.tpe;

/**
 * An interface supported by clients of the TpeManager who want
 * to be informed of when a position editor is opened.
 */
@Deprecated
public interface TpeManagerWatcher {
    /** The position editor has been opened, closed. */
    public void tpeOpened(TelescopePosEditor tpe);
}

