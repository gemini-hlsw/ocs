//
// $
//

package edu.gemini.dataman.gsa.query;

import edu.gemini.dataman.context.DatamanConfig;
import edu.gemini.util.ssh.SshExecSession;
import edu.gemini.util.ssh.SshExecSession$;
import edu.gemini.util.ssh.SshCommandResult;
import edu.gemini.util.ssh.SshException;

import java.util.*;
import java.net.ConnectException;
import scala.util.Try;
import scala.util.Success;
import scala.util.Failure;

/**
 * Determines whether a particular file is in the queued directory.
 */
public final class QueuedQuery {

    private DatamanConfig config;

    public QueuedQuery(DatamanConfig config) {
        this.config = config;
    }

    /**
     * Checks whether a particular file is in the GSA queue directory.
     *
     * @param filename name of the file, e.g., N20080714S0001.fits
     *
     * @return <code>true</code> if the file is in the queue directory waiting
     * for further processing, <code>false</code> otherwise
     *
     * @throws ConnectException if unable to connect to the GSA machine
     * @throws SshException if there is a problem running remote commands on
     * the GSA machine
     */
    public boolean isQueued(String filename) throws SshException {
        StringBuilder cmd = new StringBuilder();
        String destDir = config.getGsaXferConfig().getDestDir();
        cmd.append("ls -1 ").append(destDir);
        if (!destDir.endsWith("/")) cmd.append('/');
        cmd.append(filename);

        Try<SshCommandResult> commandResultTry = SshExecSession$.MODULE$.execute(config.getGsaXferConfig(), cmd.toString());
        if (commandResultTry.isSuccess()) {
            SshCommandResult commandResult = commandResultTry.get();
            String listing = commandResult.output();
            return ((listing != null) && listing.trim().endsWith(filename));
        }
        else {
            Failure<SshCommandResult> failure = (Failure<SshCommandResult>) commandResultTry;
            throw ((SshException) failure.exception());
        }
    }

    /**
     * Obtains all the files that are currently in the GSA queue directory
     * awaiting further processing by the GSA e-transfer code.
     *
     * @return the set of files sitting in the GSA queue directory awaiting
     * further processing
     *
     * @throws ConnectException if unable to connect to the GSA machine
     * @throws SshException if there is a problem running remote commands on
     * the GSA machine
     */
    public Set<String> getQueuedFiles() throws ConnectException, SshException {
        StringBuilder cmd = new StringBuilder();
        String destDir = config.getGsaXferConfig().getDestDir();
        cmd.append("ls -1 ").append(destDir);
        if (!destDir.endsWith("/")) cmd.append('/');
        cmd.append("*.fits");

        Set<String> res = new HashSet<String>();

        String[] files = null;
        Try<SshCommandResult> commandResultTry = SshExecSession$.MODULE$.execute(config.getGsaXferConfig(), cmd.toString());
        if (commandResultTry.isSuccess()) {
            SshCommandResult commandResult = commandResultTry.get();
            String listing = commandResult.output();
            if ((listing == null) || "".equals(listing.trim())) return res;
            files = listing.split("\\s");
        }

        if (files == null) return Collections.emptySet();

        for (String f : files) {
            int i = f.lastIndexOf("/");
            if (i >= 0) f = f.substring(i+1);
            res.add(f);
        }
        return res;
    }
}
