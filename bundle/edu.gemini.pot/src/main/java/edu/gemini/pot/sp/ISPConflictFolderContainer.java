package edu.gemini.pot.sp;

/**
 *
 */
public interface ISPConflictFolderContainer {

    String CONFLICT_FOLDER_PROP = "ConflictFolder";

    ISPConflictFolder getConflictFolder();

    void setConflictFolder(ISPConflictFolder folder) throws SPNodeNotLocalException, SPTreeStateException;

    void removeConflictFolder();
}
