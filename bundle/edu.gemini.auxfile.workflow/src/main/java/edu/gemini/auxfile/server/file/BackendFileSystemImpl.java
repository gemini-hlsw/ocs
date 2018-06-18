//
// $Id: BackendFileSystemImpl.java 893 2007-07-19 19:43:20Z swalker $
//

package edu.gemini.auxfile.server.file;

import edu.gemini.auxfile.api.AuxFile;
import edu.gemini.auxfile.api.AuxFileException;
import edu.gemini.auxfile.server.AuxFileChunk;
import edu.gemini.auxfile.server.AuxFileServer;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class BackendFileSystemImpl implements AuxFileServer {
    private static final Logger LOG = Logger.getLogger(BackendFileSystemImpl.class.getName());

    private static final long MAX_SINGLE_FILE_SIZE = 1024 * 1024 *  250;
    private static final long MAX_ALL_FILE_SIZES   = 1024 * 1024 * 2000;


    private AuxFile _list(SPProgramID progId, File f) throws AuxFileException {
        if (!f.exists()) return null;
        final MetaData md = getMetaData(progId, f.getName());
        return new AuxFile(progId, f, md.getDescription(), md.isChecked(), md.getLastEmailed());
    }

    @Override
    public Collection<AuxFile> list(SPProgramID progId, Collection<String> fileNames) throws AuxFileException {
        List<AuxFile> afList = new ArrayList<AuxFile>();

        for (String fileName : fileNames) {
            File f = FileManager.instance().getProgramFile(progId, fileName);
            AuxFile af = _list(progId, f);
            if (af != null) afList.add(af);
        }
        return afList;
    }

    @Override
    public Collection<AuxFile> listAll(SPProgramID progId) throws AuxFileException {
        List<AuxFile> afList = new ArrayList<AuxFile>();
        File progDir = FileManager.instance().getProgramDir(progId);
        File[] files = progDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
        if (files == null) return afList;

        for (File f : files) {
            afList.add(_list(progId, f));
        }
        return afList;
    }

    @Override
    public boolean delete(SPProgramID progId, Collection<String> fileNames) {
        FileManager man = FileManager.instance();

        boolean res = true;
        for (String fileName : fileNames) {

            // Delete the file itself.
            File f = man.getProgramFile(progId, fileName);
            if (!f.delete()) {
                res = false;
                continue;
            }

             // Delete the meta file if it exists.
            f = man.getMetaFile(progId, fileName);
            if (f.exists() && !f.delete()) {
                res = false;
                continue;
            }

            // Delete any left over temporary transfer files.
            List<File> xferList = man.getAllTransferFiles(progId, fileName);
            for (File xf : xferList) {
                if (!xf.delete()) {
                    res = false;
                }
            }
        }
        return res;
    }

    @Override
    public boolean deleteAll(SPProgramID progId) {
        File dir = FileManager.instance().getProgramDir(progId);
        return FileUtil.deleteDir(dir);
    }

    @Override
    public AuxFileChunk fetchChunk(SPProgramID progId, String fileName, int chunkNumber, int chunkSize, long timestamp)
            throws AuxFileException {
        // Compare timestamps, if necessary.
        File f = FileManager.instance().getProgramFile(progId, fileName);
        if (!f.exists()) return null;

        long actualTimestamp = f.lastModified();
        if ((timestamp > 0) && (actualTimestamp != timestamp)) {
            throw new AuxFileException("file updated during fetch: " + progId + ", " + fileName);
        }

        // Read the file chunk.
        int startPos = chunkSize * chunkNumber;
        byte[] chunk;

        try {
            chunk = FileUtil.readChunk(f, startPos, chunkSize);
        } catch (IOException ex) {
            String msg = "problem fetching file " + progId + ", " + fileName;
            LOG.log(Level.WARNING, msg, ex);
            throw AuxFileException.create(msg, ex);
        }

        // Return the result.
        long fileSize = f.length();
        return new AuxFileChunk(chunkNumber, chunkSize, fileSize, actualTimestamp, chunk);
    }

    private void _verifyFileSizes(String fileName, AuxFileChunk chunk, File progDir) throws AuxFileException {
        // Make sure this file isn't too big.
        if (chunk.getFileSize() > MAX_SINGLE_FILE_SIZE) {
            throw new AuxFileException(fileName + " is bigger than the limit for a single file transfer");
        }

        File[] files = progDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
        if (files == null) return;

        long total = 0;
        for (File f : files) {
            total += f.length();
        }
        total += chunk.getFileSize();

        if (total > MAX_ALL_FILE_SIZES) {
            throw new AuxFileException("Adding " + fileName + " would require more than the permitted space for this program's files.");
        }
    }

    @Override
    public String storeChunk(SPProgramID progId, String fileName, AuxFileChunk chunk, String token)
            throws AuxFileException {
        FileManager man = FileManager.instance();

        // Get the directory associated with the program, creating it if needed.
        File progDir = man.getProgramDir(progId);
        if (!progDir.exists()) man.initProgramDir(progId);

        // Make sure this file isn't too big, and that all the files together
        // wouldn't be too big.
        _verifyFileSizes(fileName, chunk, progDir);

        // Get the directory assocaited with file transfers, creating it if
        // needed.
        File xferDir = man.getTransferDir(progId);
        if (!xferDir.exists()) xferDir.mkdir();

        // Get the name of the temp file created for this file download.
        if ((token == null) || "".equals(token)) {
            token = FileManager.getNextDownloadToken();
        }
        File f = man.getTransferFile(progId, fileName, token);

        // Write the bytes.
        try {
            FileUtil.writeChunk(f, chunk.getChunkData());
        } catch (IOException ex) {
            String msg = "problem storing file " + progId + ", " + fileName;
            LOG.log(Level.WARNING, msg, ex);
            throw AuxFileException.create(msg, ex);
        }

        // If we're done transferring this file, rename it to the destination
        // file name. Mark file as unchecked to reset NGO checked flag in the
        // case of a re-upload.
        boolean lastChunk = chunk.isLastChunk();
        if (lastChunk) {
            f.renameTo(man.getProgramFile(progId, fileName));
            setChecked(progId, Collections.singleton(fileName), false);
        }

        // Return the download token, so that the next chunk (if any) can be
        // associated with the correct file.
        return token;
    }

    public MetaData getMetaData(SPProgramID programId, String fileName) throws AuxFileException {
        try {
            return MetaData.forFile(programId, fileName);
        } catch (IOException ex) {
            final String msg = String.format("problem reading meta file: %s, %s", programId, fileName);
            LOG.log(Level.SEVERE, msg, ex);
            throw AuxFileException.create(msg, ex);
        }
    }

    @FunctionalInterface
    interface IOExceptionThrowingConsumer<A> {
        void accept(A a) throws IOException;
    }

    private void setValue(SPProgramID progId, Collection<String> fileNames, IOExceptionThrowingConsumer<MetaData> setter) throws AuxFileException {
        for (String fileName: fileNames) {
           try {
               setter.accept(MetaData.forFile(progId, fileName));
           } catch (IOException ex) {
               final String msg = String.format("problem writing meta file: %s, %s", progId, fileName);
               LOG.log(Level.SEVERE, msg, ex);
               throw AuxFileException.create(msg, ex);
           }
       	}
    }

    @Override
    public void setDescription(SPProgramID progId, Collection<String> fileNames, String newDescription) throws AuxFileException {
        setValue(progId, fileNames, m -> m.setDescription(newDescription));
    }

    @Override
    public void setChecked(SPProgramID progId, Collection<String> fileNames, boolean newChecked) throws AuxFileException {
        setValue(progId, fileNames, m -> m.setChecked(newChecked));
    }

    @Override
    public void setLastEmailed(SPProgramID progId, Collection<String> fileNames, Option<Instant> newEmailed) throws AuxFileException {
        setValue(progId, fileNames, m -> m.setLastEmailed(newEmailed));
    }
}
