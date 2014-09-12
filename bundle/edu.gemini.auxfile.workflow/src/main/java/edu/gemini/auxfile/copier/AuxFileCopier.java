package edu.gemini.auxfile.copier;

import edu.gemini.spModel.core.SPProgramID;

import java.io.File;

/**
 * Interface ISimpleCopyTask
 *
 * @author Nicolas A. Barriga
 *         Date: 5/31/12
 */
public interface AuxFileCopier {
    public boolean copy(SPProgramID progId, File file);
}
