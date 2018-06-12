//
// $Id: FileManager.java 855 2007-05-22 02:52:46Z rnorris $
//

package edu.gemini.auxfile.server.file;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.auxfile.api.AuxFileException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 */
public final class FileManager {
    private static final Logger LOG = Logger.getLogger(FileManager.class.getName());

    private static FileManager _instance;

    public synchronized static void init(File rootDirectory) {
        _instance = new FileManager(rootDirectory);
    }

    public synchronized static FileManager instance() {
        if (_instance == null) {
            throw new IllegalStateException("FileManager is not initialized");
        }
        return _instance;
    }

    private static int _tokenIndex = 0;
    private static DateFormat _format = new SimpleDateFormat("yyyyMMddHHmmss");

    public static synchronized String getNextDownloadToken() {
        String timeStr = _format.format(new Date());
        return timeStr + "-" + String.valueOf(_tokenIndex++);
    }

    private static final String XFER_DIR    = "xfer";

    private static final String META_DIR    = "meta";
    private static final String META_SUFFIX = ".meta";

    // were aux files for deleted programs are kept
    private static final String TRASH_DIR   = "trash";

    private File _root;

    private FileManager(File root) {
        _root = root;
    }

    public File getRoot() {
        return _root;
    }

    public File getTrashDir() {
        File f = new File(_root, TRASH_DIR);

        if (!f.exists()) {
            if (!f.mkdir()) {
                String msg = "Could not create trash dir: " + f;
                LOG.log(Level.SEVERE, msg);
                throw new RuntimeException(msg);
            }
        }

        return f;
    }

    public Collection<File> getAllProgramDirs() {
        File[] res = _root.listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (!file.isDirectory()) return false;
                if (TRASH_DIR.equals(file.getName())) return false;

                String progIdStr = file.getName();
                try {
                    SPProgramID.toProgramID(progIdStr);
                } catch (SPBadIDException ex) {
                    LOG.log(Level.INFO, "Auxfile root contains an odd directory: " + progIdStr, ex);
                    return false;
                }
                return true;
            }
        });
        return Arrays.asList(res);
    }

    public Collection<SPProgramID> getAllProgramIds() {
        final List<SPProgramID> res = new ArrayList<>();

        _root.listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (!file.isDirectory()) return false;
                String progIdStr = file.getName();
                try {
                    res.add(SPProgramID.toProgramID(progIdStr));
                } catch (SPBadIDException ex) {
                    LOG.log(Level.INFO, "Auxfile root contains an odd directory: " + progIdStr, ex);
                }
                return false;
            }
        });
        return res;
    }

    public File getProgramDir(SPProgramID progId) {
        return new File(_root, progId.toString());
    }

    public File getMetaDir(SPProgramID progId) {
        return new File(getProgramDir(progId), META_DIR);
    }

    public File getTransferDir(SPProgramID progId) {
        return new File(getProgramDir(progId), XFER_DIR);
    }


    public File getProgramFile(SPProgramID progId, String fileName) {
        return new File(getProgramDir(progId), fileName);
    }

    public File getMetaFile(SPProgramID progId, String fileName) {
        return new File(getMetaDir(progId), fileName + META_SUFFIX);
    }

    public File getTransferFile(SPProgramID progId, String fileName, String token) {
        if (token == null) throw new NullPointerException("token is null");
        File dir = getTransferDir(progId);
        return new File(dir, fileName + "-" + token);
    }

    public List<File> getAllTransferFiles(SPProgramID progId, final String fileName) {
        List<File> res = new ArrayList<>();
        File dir = getTransferDir(progId);

        dir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String name) {
                return name.startsWith(fileName);
            }
        });

        return res;
    }

    public void initProgramDir(SPProgramID progId) throws AuxFileException {
        mkdir(getProgramDir(progId));
        mkdir(getMetaDir(progId));
        mkdir(getTransferDir(progId));
    }

    private static void mkdir(File dir) throws AuxFileException {
        if (dir.exists()) return;
        if (!dir.mkdir()) throw new AuxFileException("could not mkdir: " + dir);
    }
}
