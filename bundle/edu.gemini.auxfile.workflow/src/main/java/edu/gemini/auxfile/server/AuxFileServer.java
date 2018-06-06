package edu.gemini.auxfile.server;

import edu.gemini.auxfile.api.AuxFile;
import edu.gemini.auxfile.api.AuxFileException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.immutable.Option;

import java.time.Instant;
import java.util.Collection;

public interface AuxFileServer {
    Collection<AuxFile> list(SPProgramID progId, Collection<String> fileNames)
            throws AuxFileException;

    Collection<AuxFile> listAll(SPProgramID progId)
            throws AuxFileException;

    boolean delete(SPProgramID progId, Collection<String> fileNames)
            throws AuxFileException;

    boolean deleteAll(SPProgramID progId)
            throws AuxFileException;

    AuxFileChunk fetchChunk(SPProgramID progId, String fileName,
                         int chunkNumber, int chunkSize, long timestamp)
            throws AuxFileException;

    String storeChunk(SPProgramID progId, String fileName, AuxFileChunk chunk, String token)
            throws AuxFileException;

    void setDescription(SPProgramID progId, Collection<String> fileNames, String newDescription)
            throws AuxFileException;

    void setChecked(SPProgramID progId, Collection<String> fileNames, boolean newChecked)
			throws AuxFileException;

    void setLastEmailed(SPProgramID progId, Collection<String> fileNames, Option<Instant> newLastEmailed)
            throws AuxFileException;
}
