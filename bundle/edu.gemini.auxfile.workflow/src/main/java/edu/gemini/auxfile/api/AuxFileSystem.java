//
// $Id: AuxFileSystem.java 855 2007-05-22 02:52:46Z rnorris $
//

package edu.gemini.auxfile.api;

import edu.gemini.spModel.core.SPProgramID;

import java.io.File;
import java.util.Collection;

/**
 * The public interface to the auxiliary file system.
 */
public interface AuxFileSystem {

    /**
     * Gets the auxiliary file information associated with the given file,
     * if it exists.  Returns <code>null</code> if the file doesn't exist.
     *
     * @param programId id of the program whose file should be listed
     *
     * @param fileNames names of the files whose information should be retrieved
     *
     * @return information about the indicated files; an empty collection if
     * there are no such files
     *
     * @throws AuxFileException if there are any problems with the request
     */
    Collection<AuxFile> list(SPProgramID programId, Collection<String> fileNames)
            throws AuxFileException;

    /**
     * Gets a listing of all the files associated with a particular program, if
     * any.  Returns an empty list if there are no such files.
     *
     * @param programId id of the program whose files should be listed
     *
     * @return Collection of the files associated with the identified program; an
     * empty Collection if there are no such files
     *
     * @throws AuxFileException if there are any problems with the request
     */
    Collection<AuxFile> listAll(SPProgramID programId)
            throws AuxFileException;

    /**
     * Deletes a particular file.
     *
     * @param programId id of the program whose file should be deleted
     *
     * @param fileNames names of the file to delete
     *
     * @return <code>true</code> if successful and the files are deleted by this
     * call; <code>false</code> if any of the files do not exist or cannot be
     * deleted
     *
     * @throws AuxFileException if there are any problems with the request
     */
    boolean delete(SPProgramID programId, Collection<String> fileNames)
            throws AuxFileException;

    /**
     * Deletes all the files associated with a particular program.
     *
     * @param programId id of the program whose files should be deleted
     *
     * @return <code>true</code> if all the files (if any) are deleted by this
     * call; <code>false</code> if there are no files to delete or if there is
     * a problem deleting one or more files
     *
     * @throws AuxFileException if there are any problems with the request
     */
    boolean deleteAll(SPProgramID programId)
            throws AuxFileException;

    /**
     * Fetches the indicated remote file, storing it in the
     * <code>localFile</code>.  The content of <code>localFile</code>, if it
     * exists, will be replaced by the content of the remote file.
     *
     * @param programId id of the program whose file will be fetched
     *
     * @param remoteFileName name of the remote file to fetch
     *
     * @param localFile local file that will be written with the content of the
     * remote file
     *
     * @param listener optional listener object that will be kept up-to-date
     * as the file is fetched (optional, may be <code>null</code>)
     *
     * @return <code>true</code> if the file exists and was fetched;
     * <code>false</code> otherwise
     *
     * @throws AuxFileException if there are any problems with the request
     */
    boolean fetch(SPProgramID programId, String remoteFileName, File localFile, AuxFileTransferListener listener)
            throws AuxFileException;

    /**
     * Stores the <code>localFile</code> remotely, using the name
     * <code>remoteFileName</code>.  The content of the remote file with this
     * name, if it exists, will be replaced by the content of local file.
     *
     * @param programId id of the program with which the local file will be
     * associated
     *
     * @param remoteFileName name that the file should have on the remote
     * machine
     *
     * @param localFile file to transfer to the remote machine
     *
     * @param listener optional listener object that will be kept up-to-date
     * as the file is stored (optional, may be <code>null</code>)
     *
     * @throws AuxFileException if there are any problems with the request
     */
    void store(SPProgramID programId, String remoteFileName, File localFile, AuxFileTransferListener listener)
            throws AuxFileException;

    /**
     * Sets the description associated with a remote file.  (Note that the
     * description is part of the {@link AuxFile} returned by a listing.  See
     * {@link #list} and {@link #listAll}).
     *
     * @param programId id of the program with which the file is associated
     *
     * @param fileNames names of the files on the remote machine whose
     * description should be set
     *
     * @param newDescription new description that the file should have
     *
     * @throws AuxFileException if there are any problems with the request
     */
    void setDescription(SPProgramID programId, Collection<String> fileNames, String newDescription)
            throws AuxFileException;

    void setChecked(SPProgramID programId, Collection<String> fileNames, boolean newChecked)
    		throws AuxFileException;


}


