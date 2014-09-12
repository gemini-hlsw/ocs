//
// $Id: AbstractXferCommand.java 711 2006-12-28 13:38:20Z shane $
//

package edu.gemini.dataman.xfer;

import edu.gemini.util.ssh.*;

import edu.gemini.dataman.context.DatamanServices;
import edu.gemini.dataman.context.XferConfig;
import edu.gemini.dataman.util.*;
import edu.gemini.spModel.dataset.DatasetLabel;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.ConnectException;
import scala.util.Try;
import scala.util.Success;
import scala.util.Failure;
import scala.runtime.BoxedUnit;

/**
 * Base class for commands that transfer datasets to remote machines.
 */
abstract class AbstractXferCommand implements DatasetCommand {
    private static final Logger LOG = Logger.getLogger(AbstractXferCommand.class.getName());

    private final DatamanServices _services;
    private final DatasetLabel _label;
    private final File _file;
    private final XferConfig _config;
    private boolean _overwrite = true;

    private SshExecSession _ssh;

    protected AbstractXferCommand(DatamanServices services, DatasetLabel label, File file, XferConfig config) {
        _services = services;
        _label = label;
        _file = file;
        _config = config;
    }

    public DatamanServices getServices() {
        return _services;
    }

    public DatasetLabel getLabel() {
        return _label;
    }

    public File getFile() {
        return _file;
    }

    public XferConfig getXferConfig() {
        return _config;
    }

    public synchronized boolean getOverwrite() {
        return _overwrite;
    }

    public synchronized void setOverwrite(boolean overwrite) {
        _overwrite = overwrite;
    }

    public String formatXferInfo() {
        try {
            return String.format("%s -> %s@%s:%s",
                                            _file.getPath(), _config.getUser(),
                                          _config.getHost(), _config.getDestDir());
        } catch (NullPointerException ex) {
            return "";
        }
    }

    protected String getTempFilePath() {
        StringBuilder buf = new StringBuilder();
        String remoteDir = _config.getTempDir();
        buf.append(remoteDir);
        if (!remoteDir.endsWith("/")) buf.append('/');
        buf.append(_file.getName());
        return buf.toString();
    }

    protected String getDestFilePath() {
        StringBuilder buf = new StringBuilder();
        String remoteDir = _config.getDestDir();
        buf.append(remoteDir);
        if (!remoteDir.endsWith("/")) buf.append('/');
        buf.append(_file.getName());
        return buf.toString();
    }

    protected void ftpFile() throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("start file xfer for '" + _file.getName() + '\'');
        }

        String desDir = _config.getTempDir();
        long startTime = System.currentTimeMillis();
        Try<BoxedUnit> result = SftpSession$.MODULE$.copy(_config, _file, desDir);
        long xferTime = System.currentTimeMillis() - startTime;

        if (result.isSuccess()) {
            LOG.info(String.format("Copied the file %s to %s@%s:%s in %d ms",
                    _file.getPath(), _config.getUser(), _config.getHost(), desDir,
                    xferTime));
        } else {
            LOG.log(Level.INFO, "Could not xfer file " + formatXferInfo(), ((Failure<BoxedUnit>) result).exception());
            throw new IOException("Could not xfer file " + formatXferInfo());
        }
    }

    protected void appendExistenceCheck(StringBuilder cmd) {
        String tmpFile  = getTempFilePath();
        if (!getOverwrite()) {
            cmd.append("if [ -f ").append(getDestFilePath()).append(" ]; then\n");
            cmd.append("    echo \"file exists\"\n");
            cmd.append("    rm -f ").append(tmpFile).append('\n');
            cmd.append("    echo [exit 1]");
            cmd.append("    exit 1\n");
            cmd.append("fi\n");
        }
    }

    protected void appendMd5Check(StringBuilder cmd) throws InterruptedException, IOException {
        String md5 = DatamanFileUtil.md5HexString(_file);

        String tmpFile  = getTempFilePath();

        // Compare checksums
        cmd.append("case `uname -s` in\n");
        cmd.append("    Linux | SunOS)\n");
        cmd.append("        set `md5sum ").append(tmpFile).append("`\n");
        cmd.append("        md5=$1\n");
        cmd.append("        ;;\n");
        cmd.append("    Darwin)\n");
        cmd.append("        md5=`md5 -q ").append(tmpFile).append("`\n");
        cmd.append("        ;;\n");
        cmd.append("    *)\n");
        cmd.append("        md5=\"unknown\"\n");
        cmd.append("        ;;\n");
        cmd.append("esac\n");
        cmd.append("if [ \"").append(md5).append("\" != \"").append("$md5").append("\" ]; then\n");
        cmd.append("    echo \"checksums do not match\"\n");
        cmd.append("    rm -f ").append(tmpFile).append('\n');
        cmd.append("    echo [exit 2]");
        cmd.append("    exit 2\n");
        cmd.append("fi\n");
    }

    protected void appendMv(StringBuilder cmd) {
        String tmpFile  = getTempFilePath();
        String destFile = getDestFilePath();

        cmd.append("mv -f ").append(tmpFile).append(' ').append(destFile).append('\n');
        appendExitStatusCheck(cmd, "could not move the file");
    }

    protected void appendRmTmp(StringBuilder cmd) {
        String tmpFile  = getTempFilePath();
        cmd.append("rm -f ").append(tmpFile).append('\n');
    }

    protected void appendChmod(StringBuilder cmd, String perms) {
        String filePath = getTempFilePath();
        cmd.append("chmod ").append(perms).append(' ').append(filePath).append('\n');
        appendExitStatusCheck(cmd, "could not chmod the file");
    }

    protected void appendChgrp(StringBuilder cmd, String group) {
        String filePath = getTempFilePath();
        cmd.append("chgrp ").append(group).append(' ').append(filePath).append('\n');
        appendExitStatusCheck(cmd, "could not chgrp the file");
    }

    protected void appendExitStatusCheck(StringBuilder cmd, String failure) {
        String tmpFile  = getTempFilePath();
        cmd.append("EX=$?\n");
        cmd.append("if [ $EX -ne 0 ]; then \n");
        cmd.append("    echo ").append(failure).append('\n');
        cmd.append("    rm -f ").append(tmpFile).append('\n');
        cmd.append("    echo [exit $EX]\n");
        cmd.append("    exit $EX\n");
        cmd.append("fi\n");
    }

//        cmd = cmd.replace("\"", "\\\"");
//        StringBuilder buf = new StringBuilder(cmd.length() + 10);
//        buf.append("sh -c \"").append(cmd).append("\"");

//        System.out.println("*****************");
//        System.out.println(cmd);
//        System.out.println("---");

//        SshSession.CommandResult res = ssh.execute(cmd);
//        System.out.println(res);
//        System.out.println("*****************");
//        return res;

    protected SshCommandResult exec(SshExecSession ssh, String cmd)
            throws SshException {
        Try<SshCommandResult> result = ssh.execute(cmd);
        if (result.isSuccess())
            return result.get();
        else {
            Failure<SshCommandResult> failure = (Failure<SshCommandResult>) result;
            throw ((SshException) (failure.exception()));
        }
    }

    protected SshExecSession getSshSession() throws SshException {
        if (_ssh != null) return _ssh;

        Try<SshExecSession> sshExecSessionTry = SshExecSession.connect(_config);
        if (sshExecSessionTry.isFailure()) {
            Failure<SshExecSession> result = (Failure<SshExecSession>) sshExecSessionTry;
            throw (SshException) result.exception();
        }
        else {
          _ssh = sshExecSessionTry.get();
          return _ssh;
        }
    }

    public Boolean call() throws Exception {

        try {
            if (!preXfer() || !xferFile() || !postXfer()) return false;

        } catch (Exception ex) {
            LOG.warning("Unhandled problem transfering " + formatXferInfo());
            throw ex;

        } finally {
            if (_ssh != null) {
                _ssh.disconnect();
            }
        }

        return true;
    }

    protected boolean preXfer() {
        return true;
    }

    protected abstract boolean xferFile();

    protected boolean postXfer() {
        return true;
    }

    public boolean scheduleXfer() {
        return DatasetCommandProcessor.INSTANCE.add(this);
    }
}
