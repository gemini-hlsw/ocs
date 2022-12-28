package edu.gemini.auxfile.copier.impl;

import edu.gemini.auxfile.copier.CopyConfig;
import edu.gemini.util.ssh.SftpSession;
import edu.gemini.auxfile.copier.AuxFileCopier;
import edu.gemini.auxfile.copier.AuxFileType;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.util.ssh.SftpSession$;
import scala.runtime.BoxedUnit;
import scala.util.Failure;
import scala.util.Try;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class AuxFileCopierImpl implements AuxFileCopier {

    private static final Logger LOG = Logger.getLogger(AuxFileCopierImpl.class.getName());
    private final Map<AuxFileType, CopyConfig> _configs;

    public AuxFileCopierImpl(Map<AuxFileType, CopyConfig> configs) {
        if (configs == null) throw new NullPointerException("configs == null");
        _configs = configs;
    }

    @Override
    public boolean copy(SPProgramID progId, File file) {
        final CopyConfig config = _configs.get(AuxFileType.getFileType(file));

        // Make sure program id conforms to what was expected.
        final String desDir = config.getDestDir(progId);
        if (desDir == null) {
            LOG.log(Level.WARNING, "Auxfile uploaded for program with unexpected id: " + progId.toString());
            return true;
        }

        // If the file got removed, forget about it.
        if (!file.exists()) return true;

        // Do the copy using sftp.
        if (!config.isEnabled()) {
            LOG.log(Level.INFO, "Auxfile copy disabled, skipping copy of " + file.getName());
            return true;
        }

        final Try<BoxedUnit> result = SftpSession$.MODULE$.copy(config, file, desDir);
        if (result.isFailure()) {
            final Failure<BoxedUnit> failure = (Failure<BoxedUnit>) result;
            final String msg = "Could not copy file " + file.getName();
            LOG.log(Level.SEVERE, msg, failure.exception());
            return false;
        }

        final StringBuilder buf = new StringBuilder();
        buf.append("Copied the file ");
        buf.append(file.getPath()).append(" to ");
        buf.append(config.getUser()).append("@");
        buf.append(config.getHost()).append(":");
        buf.append(desDir);
        LOG.info(buf.toString());

        return true;
    }
}
