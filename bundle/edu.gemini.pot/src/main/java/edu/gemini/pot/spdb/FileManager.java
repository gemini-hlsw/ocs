package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.sp.memImpl.MemSerializer;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.spModel.core.SPProgramID;

import java.io.*;
import java.util.*;


/**
 * Handles file I/O for the database.  Currently, the locking is not very
 * sophisticated.  Only one thread at a time is permitted to execute any
 * of the public methods.  That is sufficient for now because after startup
 * only the <code>{@link StorageManager}</code> uses the
 * <code>FileManager</code>.
 */
final class FileManager implements IDBPersister {

    /** The file suffix that is appended to programs in the database. */
    public static final String PROGRAM_SUFFIX = ".sp";

    /** The file suffix which is appended to nightly plans in the database. */
    public static final String PLAN_SUFFIX    = ".pl";

    /**
     * A file filter that separates out potential Science Program files
     * or Nightly Plan files in the database directory based upon whether they
     * end in the <code>PROGRAM_SUFFIX</code> or <code>NIGHTLY_PLAN_SUFFIX</code>
     * and are readable and writable.
     */
    private static final class ISPFileFilter implements FileFilter {
        private final String _suffix;

        public ISPFileFilter(String suffix) {
            if (suffix == null) throw new NullPointerException("ISPFileFilter requires a suffix");
            _suffix = suffix;
        }

        public boolean accept(File f) {
            if (!f.getName().endsWith(_suffix)) return false;
            return f.canRead() && f.canWrite();
        }

    }

    private static final ISPFileFilter _progFilter = new ISPFileFilter(PROGRAM_SUFFIX);
    private static final ISPFileFilter _planFilter = new ISPFileFilter(PLAN_SUFFIX);

    private final File _dbDir;
    private final MemSerializer _ser;
    private final Map<SPNodeKey, File> _fileMap = new HashMap<SPNodeKey, File>();

    /**
     * Constructs the <code>FileManager</code> with the database directory
     * to use.  The <code>dbDir</code> argument must either be non-existent
     * but creatable, or else existent and both readable and writable.
     *
     * @throws IOException if <code>dbDir</code> is not valid
     */
    FileManager(final File dbDir) throws IOException {
        _setupDbDirectory(dbDir);
        _dbDir = dbDir;
        _ser   = new MemSerializer();
    }

    /**
     * Sets the database directory, creating it if necessary and ensuring that
     * it is useable.
     */
    private static void _setupDbDirectory(File dbDir) throws IOException {
        // Make sure the directory exists, creating if necessary.
        if (!(dbDir.exists() || dbDir.mkdirs())) {
            final String msg = "Could not create the database directory: " + dbDir.getPath();
            throw new IOException(msg);
        }

        // Make sure the file is a readable/writable directory.
        if (!(dbDir.isDirectory() && dbDir.canRead() && dbDir.canWrite())) {
            final String msg = dbDir.toString() + ": not a readable/writable directory";
            throw new IOException(msg);
        }
    }

    /**
     * Gets the canonical path associated with the given <code>File</code>,
     * or the relative path if that fails.
     */
    private static String _getPath(File f) {
        try {
            return f.getCanonicalPath();
        } catch (Exception ex) {
            return f.getPath();
        }
    }

    /**
     * Looks up the given program's id.  Though the interface is
     * <code>Remote</code>, we are dealing with local objects so the
     * <code>prog.getProgramKey()</code> should never fail.  This method just
     * wraps the call in a try block for the convenience of the caller.
     */
    private static String _getDocumentID(ISPNode prog) {
        try {
            final SPProgramID docId = prog.getProgramID();
            if (docId == null) return null;
            return docId.stringValue();
        } catch (Exception ex) {
            System.err.println("Problem getting programID for `" + prog +
                               "': " + ex);
            throw new GeminiRuntimeException("Could not determine the filename.");
        }
    }

    /**
     * Gets a <code>File</code> associated with the document.
     */
    private File _getDocumentFile(ISPNode node, String suffix) {
        final String id = _getDocumentID(node);
        final String filename = (id == null) ? node.getNodeKey().toString() : id;
        return _getDocumentFile(filename, suffix);
    }

    /**
     * Gets the full path to the file with the given prefix name.
     */
    private File _getDocumentFile(String filePrefix, String fileSuffix) {
        return new File(_dbDir, filePrefix + fileSuffix);
    }

    public List<ISPProgram> reloadPrograms() throws IOException {
        return reload("program", _progFilter);
    }

    public List<ISPNightlyRecord> reloadPlans() throws IOException {
        return reload("plan", _planFilter);
    }

    private <T extends ISPRootNode> List<T> reload(final String name, final FileFilter filter) throws IOException {
        System.out.println(String.format("Loading the %s database ...", name));

        final long time1      = System.currentTimeMillis();
        final File[] fileA    = _dbDir.listFiles(filter);
        final List<T> retList = new ArrayList<T>(fileA.length);

        // Try to load each file.
        for (final File progFile : fileA) {

            // Load the program file.
            final T prog;
            try {
                prog = (T) _ser.load(progFile);
                if (prog == null) continue;
            } catch (Exception ex) {
                String path = _getPath(progFile);
                if (ex.getCause() instanceof InvalidClassException) {
                    System.err.println("Warning: incompatible file: '" + path + "'. Please delete and reimport from XML");
                } else {
                    System.err.println("Problem reading program file `" + path + "': " + ex);
                }
                continue;
            }

            // If there are two program files with the same program (i.e.,
            // with the same key), just skip the next one read.
            final SPNodeKey key = prog.getNodeKey();
            if (_fileMap.get(key) != null) {
                System.out.println("Already loaded: " + key);
                continue; // already loaded
            }
            _fileMap.put(key, progFile);

            retList.add(prog);
        }

        final long time2 = System.currentTimeMillis();

        final String msg = String.format("Finished loading: %d ms, %d %ss", time2-time1, fileA.length, name);
        System.out.println(msg);
        return retList;

    }

    public void store(ISPRootNode mab) throws IOException {
        if (mab instanceof ISPNightlyRecord) {
            _storeProgram(mab, PLAN_SUFFIX);
        } else {
            _storeProgram(mab, PROGRAM_SUFFIX);
        }
    }


    private File _storeProgram(ISPRootNode node, String suffix) throws IOException {
        final SPNodeKey key = node.getNodeKey();
        final File newFile = _getDocumentFile(node, suffix);

        _storeProgram(node, newFile);

        synchronized (this) {
            final File oldFile = _fileMap.get(key);
            if ((oldFile != null) && !newFile.equals(oldFile) && oldFile.exists()) {
                // Cleanup the old file
                oldFile.delete();
            }
            _fileMap.put(key, newFile);
        }

        return newFile;
    }

    /**
     * Stores the given <code>prog</code>ram in the given <code>progFile</code>.
     */
    private void _storeProgram(ISPRootNode node, File file) throws IOException {
        // Create a temp file to write the object.
        final File tmpFile = _createTempFile(file);

        final SPNodeKey key = node.getProgramKey();
        SPNodeKeyLocks.instance.readLock(key);
        try {
            // Write the object to the temp file.
            _ser.store(node, tmpFile);

            // Rename the temp file to the destination file.
            file.delete(); // under win2k, rename fails if file exists
            if (!tmpFile.renameTo(file)) throw new IOException("Couldn't store the program.");
        } finally {
            SPNodeKeyLocks.instance.readUnlock(key);
        }
    }

    @Override public synchronized long size(SPNodeKey key) {
        final File f = _fileMap.get(key);
        return (f == null) ? -1 : f.length();
    }

    /**
     * Removes the given program, erasing the file associated with it.
     */
    public synchronized void remove(SPNodeKey key) {
        final File progFile = _fileMap.remove(key);
        if (progFile != null) progFile.delete();
    }

    /**
     * Gets a temporary file into which an object will be initially written,
     * before it is moved to the destination file after a successful write.
     */
    private static File _createTempFile(File destFile) throws IOException {
        // Get the parent directory that the file will be written into.
        final File dir = destFile.getParentFile();

        // Create a temp file to write the object.  Try to put it in the
        // same directory as the file to be ultimately written.
        final File tmpFile;
        if (dir != null) {
            tmpFile = File.createTempFile("_spdb", null, dir);
        } else {
            // Use the default system temp dir.
            tmpFile = File.createTempFile("_spdb", null);
        }

        return tmpFile;
    }

    @Override
    public long getTotalStorage() {
        return getTotalStorage(_planFilter) + getTotalStorage(_progFilter);
    }

    private long getTotalStorage(ISPFileFilter filter) {
        final File[] fileA    = _dbDir.listFiles(filter);
        long total = 0L;
        for (final File progFile : fileA)
            total = total + progFile.length();
        return total;
    }

 }


