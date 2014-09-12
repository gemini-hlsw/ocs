//
// $Id: BaseCopyXferCommand.java 227 2005-10-27 14:29:10Z shane $
//

package edu.gemini.dataman.xfer;

import edu.gemini.util.ssh.SshExecSession;
import edu.gemini.util.ssh.SshCommandResult;
import edu.gemini.dataman.context.XferConfig;
import edu.gemini.dataman.context.DatamanServices;
import edu.gemini.spModel.dataset.DatasetLabel;

import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This {@link AbstractXferCommand} extension is used to copy datasets from the
 * summit working storage to the base facility.  It doesn't update dataset
 * status in the database or do much reporting if it fails.  This is considered
 * a non-critical copy.
 */
final class BaseCopyXferCommand extends AbstractXferCommand {
    private static final Logger LOG = Logger.getLogger(BaseCopyXferCommand.class.getName());

    public BaseCopyXferCommand(DatamanServices services, DatasetLabel label,
                               File datasetFile, XferConfig config) {
        super(services, label, datasetFile, config);
    }

    private void logXferProblem(String msg, Exception ex) {
        String tmp = "Base facility xfer problem " + formatXferInfo();
        if (msg != null) tmp += ": " + msg;
        LOG.log(Level.INFO, tmp, ex);
    }

    protected boolean xferFile() {
        try {
            ftpFile();
        } catch (Exception ex) {
            logXferProblem(null, ex);
            return false;
        }
        return true;
    }

    protected boolean postXfer() {
        StringBuilder cmd = new StringBuilder();
        appendExistenceCheck(cmd);
        try {
            appendMd5Check(cmd);
        } catch (Exception ex) {
            logXferProblem("could not calculate MD5", ex);
            return false;
        }

        appendChmod(cmd, "664");
        appendMv(cmd);

        SshCommandResult res;
        try {
            SshExecSession ssh = getSshSession();
            res = exec(ssh, cmd.toString());
        } catch (Exception ex) {
            logXferProblem("problem with ssh to base facility", ex);
            return false;
        }

        if (!res.success()) {
            logXferProblem("problem running base copy command (" + res.output() + ")", null);
        }
        return res.success();
    }
}
