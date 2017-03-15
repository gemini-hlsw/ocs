package edu.gemini.auxfile.api;

import java.util.EventListener;

/**
 * A client may provide a transfer listener in order to be kept up-to-date as
 * a file is fetched or stored.
 */
public interface AuxFileTransferListener extends EventListener {

    /**
     * Provides notification that a transfer to or from the remote machine has
     * progressed.
     *
     * @param evt contains details of the transfer and its progress
     *
     * @return <code>true</code> to continue the transfer, <code>false</code>
     * to stop it
     */
    boolean transferProgressed(AuxFileTransferEvent evt);
}
