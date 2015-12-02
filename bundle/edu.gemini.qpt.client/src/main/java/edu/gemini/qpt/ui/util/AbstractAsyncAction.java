package edu.gemini.qpt.ui.util;

import edu.gemini.util.security.auth.keychain.KeyChain;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * An action that spawns a worker thread to perform its work asynchronously. In all other
 * ways identical to AbstractAction.
 * @author rnorris
 */
@SuppressWarnings("serial")
public abstract class AbstractAsyncAction extends AbstractAction {

    protected final KeyChain authClient;

    public AbstractAsyncAction(KeyChain authClient) {
        super();
        this.authClient = authClient;
    }

    public AbstractAsyncAction(String name, Icon icon, KeyChain authClient) {
        super(name, icon);
        this.authClient = authClient;
    }

    public AbstractAsyncAction(String name, KeyChain authClient) {
        super(name);
        this.authClient = authClient;
    }

    public final void actionPerformed(final ActionEvent e) {
        new Thread(getName() + " Thread") {
            public void run() {
                try {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                    }
                    asyncActionPerformed(e);
                } catch (Throwable t) {
                    t.printStackTrace(); // TODO: something
                }
            }
        }.start();
    }

    protected String getName() {
        return (String) getValue(AbstractAction.NAME);
    }

    /**
     * Subclasses should put their implementation here. The method will be invoked from a
     * non-UI thread.
     * @param e
     */
    protected abstract void asyncActionPerformed(ActionEvent e);

}
