//
// $Id: GsaXferCommand.java 292 2006-02-17 20:05:14Z shane $
//

package edu.gemini.dataman.xfer;

import edu.gemini.dataman.context.DatamanServices;
import edu.gemini.dataman.context.GsaXferConfig;
import edu.gemini.dataman.update.RecordUpdateCommand;
import edu.gemini.util.ssh.SshException;
import edu.gemini.util.ssh.SshExecSession;
import edu.gemini.util.ssh.SshCommandResult;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.GsaState;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This {@link edu.gemini.dataman.xfer.AbstractXferCommand} extension is used
 * to transfer datasets to the GSA transfer directory.  As it verifies the
 * copy, verifies the dataset can be ingested, and updates the GSA status of
 * the dataset as it progresses.
 */
final class GsaXferCommand extends AbstractXferCommand {
    private static final Logger LOG = Logger.getLogger(GsaXferCommand.class.getName());

    private static final String MAGIC_WORDS = "IS ready for ingestion";

    protected Level getExceptionLevel() {
        return Level.WARNING;
    }

    public GsaXferCommand(DatamanServices services, DatasetLabel label,
                          File file, GsaXferConfig config) {
        super(services, label, file, config);
    }

    private void logXferProblem(String msg, Exception ex) {
        String tmp = "GSA xfer problem " + formatXferInfo();
        if (msg != null) tmp += ": " + msg;
        LOG.log(getExceptionLevel(), tmp, ex);
    }

    private boolean updateGsaState(GsaState oldState, GsaState newState, Level logLevel) {
        // Log the transition.
        if (LOG.isLoggable(logLevel)) {
            String msg = String.format("Dataset %s (%s): start transition from %s to %s",
                    getLabel(), getFile().getName(), oldState, newState);
            LOG.log(logLevel, msg);
        }


        // Create the transition command.
        RecordUpdateCommand cmd = new RecordUpdateCommand(getServices(), getLabel());
        cmd.setGsaStatePrecond(oldState);
        cmd.setGsaState(newState);

        // Do the transition.
        boolean res = false;
        try {
             res = cmd.call();
        } catch (Exception ex) {
            logXferProblem("could not set GSA State", ex);
        }

        // Log the success or failure.
        if (LOG.isLoggable(logLevel) || (!res && logLevel.intValue() < Level.WARNING.intValue())) {
            String msg = String.format("Dataset %s (%s): end transition from %s to %s: %s",
                    getLabel(), getFile().getName(), oldState, newState,
                    res ? "SUCCESS" : "FAILED");

            if (!res && (logLevel.intValue() < Level.WARNING.intValue())) {
                logLevel = Level.WARNING;
            }
            LOG.log(logLevel, msg);
        }
        return res;
    }


    @Override
    protected boolean preXfer() {
        return updateGsaState(GsaState.PENDING, GsaState.COPYING, Level.INFO);
    }


    @Override
    protected boolean xferFile() {
        try {
            ftpFile();
        } catch (Exception ex) {
            logXferProblem(null, ex);
            updateGsaState(GsaState.COPYING, GsaState.COPY_FAILED, Level.WARNING);
            return false;
        }
        return true;
    }

    private boolean _confirmCopy(SshExecSession ssh)  {
        StringBuilder cmd = new StringBuilder();
        appendExistenceCheck(cmd);
        SshCommandResult res;
        try {
            appendMd5Check(cmd);
            res = exec(ssh, cmd.toString());
        } catch (Exception ex) {
            logXferProblem("could not confirm copy", ex);
            return false;
        }

        if (!res.success()) {
            logXferProblem(res.output(), null);
        }
        return res.success();
    }

    private void _cleanup(SshExecSession ssh) {
        StringBuilder cmd = new StringBuilder();
        appendRmTmp(cmd);
        try {
            exec(ssh, cmd.toString());
        } catch (SshException e) {
            logXferProblem("could not cleanup after copy", e);
        }
    }

    private boolean _confirmIngest(SshExecSession ssh) {
        StringBuilder cmd = new StringBuilder();
        GsaXferConfig conf = getGsaXferConfig();
        cmd.append("export CADC_ROOT=").append(conf.getCadcRoot()).append('\n');
        cmd.append("export DEFAULT_CONFIG_DIR=${CADC_ROOT}/config\n");
        cmd.append(conf.getMdIngestScript()).append(" -d --file=");
        cmd.append(getTempFilePath());
        SshCommandResult res;
        try {
            res = exec(ssh, cmd.toString());
        } catch (Exception ex) {
            logXferProblem("could not confirm ingest", ex);
            return false;
        }

        String output = res.output();
        if (!res.success()) {
            logXferProblem("mdIngest failed (" + output + ")", null);
            _cleanup(ssh);
            return false;
        }
        if (output == null) {
            logXferProblem("mdIngest produced no output", null);
            _cleanup(ssh);
            return false;
        }
        if (output.lastIndexOf(MAGIC_WORDS) == -1) {
            // mdIngest output is copious ... not writing it all for now.
            logXferProblem("mdIngest regected file", null);
            _cleanup(ssh);
            return false;
        }
        return true;
    }

    private boolean _finishXfer(SshExecSession ssh) {
        StringBuilder cmd = new StringBuilder();
        appendChmod(cmd, "664");
        appendChgrp(cmd, getGsaXferConfig().getCadcGroup());
        appendMv(cmd);

        SshCommandResult res;
        try {
            res = exec(ssh, cmd.toString());
        } catch (SshException e) {
            logXferProblem(null, e);
            return false;
        }
        if (!res.success()) {
            logXferProblem("could not move dataset (" + res.output() + ")", null);
        }
        return res.success();
    }

    @Override
    protected boolean postXfer()  {

        SshExecSession ssh;
        try {
            ssh = getSshSession();
        } catch (Exception ex) {
            // copy failed
            updateGsaState(GsaState.COPYING, GsaState.COPY_FAILED, Level.WARNING);
            return false;
        }

        if (_confirmCopy(ssh)) {
            // copy okay so set to VERIFYING
            if (!updateGsaState(GsaState.COPYING, GsaState.VERIFYING, Level.INFO)) return false;
        } else {
            // copy failed
            updateGsaState(GsaState.COPYING, GsaState.COPY_FAILED, Level.WARNING);
            return false;
        }

        if (_confirmIngest(ssh)) {
            // Move the script to the xfer directory.
            if (!_finishXfer(ssh)) {
                updateGsaState(GsaState.VERIFYING, GsaState.COPY_FAILED, Level.WARNING);
                return false;
            }

            // Update the state to QUEUED
            if (!updateGsaState(GsaState.VERIFYING, GsaState.QUEUED, Level.INFO)) return false;
        } else {
            // verify failed
            updateGsaState(GsaState.VERIFYING, GsaState.VERIFY_FAILED, Level.WARNING);
            return false;
        }

        return true;
    }

    protected GsaXferConfig getGsaXferConfig() {
    	return (GsaXferConfig) getXferConfig();
    }

}
