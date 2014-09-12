package jsky.app.ot.util;

import javax.security.auth.Subject;
import javax.swing.*;

/**
 * Do any background work in {@link #doInBackgroundWithSubject()}.
 * @Deprecated // not needed anymore
 */
public abstract class AuthSwingWorker<T, V> extends SwingWorker<T, V> {

    protected AuthSwingWorker() {

    }

    @Override
    protected final T doInBackground() throws Exception {
        return doInBackgroundWithSubject();
    }

    /**
     * Execute any background tasks in this method.
     *
     * @return computed value
     */
    protected abstract T doInBackgroundWithSubject() throws Exception;
}
