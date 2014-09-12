//
// $Id: GsaVigilante.java 702 2006-12-17 14:18:56Z shane $
//

package edu.gemini.dataman.gsa;

import edu.gemini.dataman.context.DatamanContext;

import java.security.Principal;
import java.util.Set;
import java.util.Timer;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The GsaVigilante is used to manage the transfer process to the GSA.
 * Specifically it periodically checks the transfer directory checking for
 * datasets with the following
 * {@link edu.gemini.spModel.dataset.GsaState GsaStates}, and performs the
 * indicated action:
 *
 * <ul>
 * <li>{@link edu.gemini.spModel.dataset.GsaState#PENDING}: Verify that the file
 * isn't in the e-transfer system and then copy it to the queueing
 * directory.</li>
 *
 * <li>{@link edu.gemini.spModel.dataset.GsaState#COPY_FAILED}: Reset to PENDING
 * and do the same thing as though the file had been in state PENDING.  This
 * effectively retries the copy.</li>
 *
 * <li>{@link edu.gemini.spModel.dataset.GsaState#QUEUED}: Ask the GSA for the
 * current status of the file and update the GsaState as appropriate.</li>
 *
 * <li>{@link edu.gemini.spModel.dataset.GsaState#TRANSFERRING}: Ask the GSA for
 * the current status of the file and update the GsaState as appropriate.</li>
 *
 * <li>{@link edu.gemini.spModel.dataset.GsaState#TRANSFER_ERROR}: Same as for
 * QUEUED.  The idea is that transfer errors are temporary.  We expect them to
 * go away eventually and keep trying until they do or the user resets the state
 *  to PENDING to try a new copy.</li>
 *
 * </ul>
 */
public final class GsaVigilante {
    private static final Logger LOG = Logger.getLogger(GsaVigilante.class.getName());
    private static final long DEFAULT_PERIOD = 30 * 60 * 1000;

    private DatamanContext _ctx;
    private Timer _scanTimer;
    private final Set<Principal> _user;

    public GsaVigilante(DatamanContext ctx, Set<Principal> user) {
        _ctx = ctx;
        _user = user;
    }

    /**
     * Starts the vigilante, turning on periodic scanning of the ODB and
     * GSA transfer directory.
     */
    public synchronized void start() {
        LOG.log(Level.FINE, "GsaVigilante.start()");

        if (_scanTimer != null) return;

        long period = _ctx.getConfig().getOdbScanTime();
        if (period <= 0) period = DEFAULT_PERIOD;
        
         _scanTimer = new Timer("GsaVigilante", true);
        _scanTimer.schedule(new GsaVigilanteTask(_ctx, _user), 0, period);
    }

    /**
     * Stops the vigilante, turning off periodic scanning of the ODB.
     */
    public synchronized void stop() {
        LOG.log(Level.FINE, "GsaVigilante.stop()");

        if (_scanTimer == null) return;
        _scanTimer.cancel();
        _scanTimer = null;
    }
}
