//
// $Id: InitialGsaXferCommand.java 227 2005-10-27 14:29:10Z shane $
//

package edu.gemini.dataman.xfer;

import edu.gemini.dataman.context.DatamanServices;
import edu.gemini.dataman.context.GsaXferConfig;
import edu.gemini.util.ssh.SshExecSession;
import edu.gemini.util.ssh.SshCommandResult;
import edu.gemini.spModel.dataset.DatasetLabel;

import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This {@link AbstractXferCommand} extension is used
 * to do the initial transfer of datasets to the GSA transfer directory.  This
 * is a "best effort" transfer to the GSA.  The GSA state is not updated if
 * the copy fails, and no attempt to verify the ingestion is made.
 */
final class InitialGsaXferCommand extends AbstractXferCommand {
    private static final Logger LOG = Logger.getLogger(InitialGsaXferCommand.class.getName());

    public InitialGsaXferCommand(DatamanServices services, DatasetLabel label,
                                 File file, GsaXferConfig config) {
        super(services, label, file, config);
    }

    private void logXferProblem(String msg, Exception ex) {
        String tmp = "Initial GSA xfer problem " + formatXferInfo();
        if (msg != null) tmp += ": " + msg;
        LOG.log(Level.INFO, tmp, ex);
    }

    private boolean _confirmCopy(SshExecSession ssh) {
        StringBuilder cmd = new StringBuilder();
        appendExistenceCheck(cmd);
        try {
            appendMd5Check(cmd);
        } catch (Exception ex) {
            logXferProblem("could not calculate MD5", ex);
            return false;
        }

        SshCommandResult res;
        try {
            res = exec(ssh, cmd.toString());
        } catch (Exception ex) {
            logXferProblem("could not confirm copy", ex);
            return false;
        }

        if (!res.success()) {
            logXferProblem("could not confirm copy (" + res.output() + ")", null);
        }
        return res.success();
    }

    private boolean _mv(SshExecSession ssh) {
        StringBuilder cmd = new StringBuilder();
        appendChmod(cmd, "664");
        appendChgrp(cmd, getGsaXferConfig().getCadcGroup());
        appendMv(cmd);

        SshCommandResult res;
        try {
            res = exec(ssh, cmd.toString());
        } catch (Exception ex) {
            logXferProblem("problem moving temp file", ex);
            return false;
        }
        if (!res.success()) {
            logXferProblem("problem moving tmp file (" + res.output() + ")", null);
        }
        return res.success();
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

    @Override
    protected boolean postXfer() {
        SshExecSession ssh;

        try {
            ssh = getSshSession();
        } catch (Exception ex) {
            logXferProblem("problem establishing ssh connection", ex);
            return false;
        }

        if (!_confirmCopy(ssh)) return false;
        return _mv(ssh);
    }

    protected GsaXferConfig getGsaXferConfig() {
    	return (GsaXferConfig) getXferConfig();
    }
}
