//
// $Id: AuxFileListener.java 855 2007-05-22 02:52:46Z rnorris $
//

package edu.gemini.auxfile.api;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;

import java.time.Instant;
import java.io.File;
import java.util.Collection;

/**
 * An interface implemented by clients interested in being informed of of
 * events that happen in the auxfile server.
 */
public interface AuxFileListener {
    void filesDeleted(SPProgramID progId, Collection<String> filenames);
    void fileFetched(SPProgramID progId, File file);
    void fileStored(SPProgramID progId, File file);
    void descriptionUpdated(SPProgramID progId, String description, Collection<File> files);
	void checkedUpdated(SPProgramID progId, boolean newChecked, Collection<File> files);
    void lastEmailedUpdated(SPProgramID progId, Option<Instant> newLastEmailed, Collection<File> files);
}
