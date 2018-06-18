package edu.gemini.auxfile.server;

import edu.gemini.auxfile.api.AuxFile;
import edu.gemini.auxfile.api.AuxFileException;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;

import java.time.Instant;
import java.util.Collection;

/**
 *
 */
public abstract class AuxFileServerDecorator implements AuxFileServer {
    private final AuxFileServer delegate;

    protected AuxFileServerDecorator(AuxFileServer delegate) {
        this.delegate = delegate;
    }

    @Override
    public Collection<AuxFile> list(SPProgramID progId, Collection<String> fileNames) throws AuxFileException {
        return delegate.list(progId, fileNames);
    }

    @Override
    public Collection<AuxFile> listAll(SPProgramID progId) throws AuxFileException {
        return delegate.listAll(progId);
    }

    @Override
    public boolean delete(SPProgramID progId, Collection<String> fileNames) throws AuxFileException {
        return delegate.delete(progId, fileNames);
    }

    @Override
    public boolean deleteAll(SPProgramID progId) throws AuxFileException {
        return delegate.deleteAll(progId);
    }

    @Override
    public AuxFileChunk fetchChunk(SPProgramID progId, String fileName, int chunkNumber, int chunkSize, long timestamp) throws AuxFileException {
        return delegate.fetchChunk(progId, fileName, chunkNumber, chunkSize, timestamp);
    }

    @Override
    public String storeChunk(SPProgramID progId, String fileName, AuxFileChunk chunk, String token) throws AuxFileException {
        return delegate.storeChunk(progId, fileName, chunk, token);
    }

    @Override
    public void setDescription(SPProgramID progId, Collection<String> fileNames, String newDescription) throws AuxFileException {
        delegate.setDescription(progId, fileNames, newDescription);
    }

    @Override
    public void setChecked(SPProgramID progId, Collection<String> fileNames, boolean newChecked) throws AuxFileException {
        delegate.setChecked(progId, fileNames, newChecked);
    }

    @Override
    public void setLastEmailed(SPProgramID progId, Collection<String> fileNames, Option<Instant> newLastEmailed) throws AuxFileException {
        delegate.setLastEmailed(progId, fileNames, newLastEmailed);
    }
}
