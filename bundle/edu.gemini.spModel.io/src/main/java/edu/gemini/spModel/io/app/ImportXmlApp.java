// Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: ImportXML.java 47005 2012-07-26 22:35:47Z swalker $
//
package edu.gemini.spModel.io.app;

/*
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.*;
import edu.gemini.shared.util.FileUtil;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.io.SpImportFunctor;
import edu.gemini.spModel.io.SpXmlParser;
import edu.gemini.spModel.io.updater.SpUpdateFunctor;
import edu.gemini.spModel.util.ProgIdParser;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
*/


/**
 * Implements a command line application for importing science programs to the
 * observing database from XML files in the given files or directories.
 */
public class ImportXmlApp {
    /*
    // constants indicating how to handle duplicate ids
    public enum DuplicationMode {
        keep, update, add
    }


    // Initialize the database connection using a remote or
    // local database.
    private static IDBDatabaseService getDatabase(String localDB) {
        if (localDB == null) throw new IllegalArgumentException("localDB = null");
        IDBDatabaseService db = null;
        try {
            db = DBLocalDatabase.create(new File(localDB));
        } catch (IOException ex) {
            System.out.println("Failed to open a database.");
            ex.printStackTrace();
            System.exit(1);
        }
        return db;
    }

    private static List<File> getFilesToImport(List<File> filesAndDirs) {
        List<File> res = new ArrayList<File>();

        for (File input : filesAndDirs) {
            if (!input.isDirectory()) {
                res.add(input);
            } else {
                File[] lst = input.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".xml");
                    }
                });
                for (File cur : lst) res.add(cur);
            }
        }

        // Sort by program id
        Collections.sort(res, new Comparator<File>() {
            public int compare(File f1, File f2) {
                String name1 = f1.getName();
                String name2 = f2.getName();

                int index1 = name1.lastIndexOf('.');
                int index2 = name2.lastIndexOf('.');

                String progIdStr1 = name1.substring(0, index1);
                String progIdStr2 = name2.substring(0, index2);

                SPProgramID progId1 = null;
                try {
                    progId1 = SPProgramID.toProgramID(progIdStr1);
                } catch (Exception ex) {
                    // empty
                }

                SPProgramID progId2 = null;
                try {
                    progId2 = SPProgramID.toProgramID(progIdStr2);
                } catch (Exception ex) {
                    // empty
                }

                if (progId1 == null) {
                    return progId2 == null ? name1.compareTo(name2) : -1;
                } else if (progId2 == null) {
                    return 1;
                }
                return progId1.compareTo(progId2);
            }
        });
        return res;
    }

    private static class IdPool {
        // used in checking for duplicates
        private final Set<SPProgramID> _progIds = new HashSet<SPProgramID>();
        private final Set<SPNodeKey>  _progKeys = new HashSet<SPNodeKey>();

        IdPool(Collection<Collection<DBProgramKeyAndId>> allProgs) {
            for (Collection<DBProgramKeyAndId> slaveProgs : allProgs) {
                for (DBProgramKeyAndId kai : slaveProgs) {
                    _progKeys.add(kai.getKey());
                    SPProgramID progId = kai.getId();
                    if (progId != null) _progIds.add(progId);
                }
            }
        }

        // Return a new unique id based on the given program id
        SPProgramID getNewProgramId(SPProgramID progId) {
            // remove existing -copyNNN part, if present
            if (progId == null) return null;

            String progIdStr = progId.stringValue();
            int i = progIdStr.lastIndexOf("-copy");
            if (i != -1) progIdStr = progIdStr.substring(0, i);

            for (int count=1; count<99; count++) {
                SPProgramID newId;
                try {
                    newId = SPProgramID.toProgramID(progIdStr + "-copy" + count);
                } catch (SPBadIDException e) {
                    throw new RuntimeException("Could not form legal program id from existing program id: " + progIdStr);
                }
                if (!exists(null, newId)) return newId;
            }
            throw new RuntimeException("Could not generate unique program id for: " + progId);
        }

        synchronized boolean exists(SPNodeKey key, SPProgramID id) {
            if ((key != null) && _progKeys.contains(key)) return true;
            return (id != null) && _progIds.contains(id);
        }

        synchronized void markExists(ISPProgram prog)  {
            _progKeys.add(prog.getNodeKey());
            _progIds.add(prog.getProgramID());
        }

    }

    private static class ImportProblem {
        private final SPNodeKey _key;
        private final SPProgramID _progId;
        private final Throwable _exception;

        ImportProblem(SPNodeKey key, SPProgramID progId, Throwable ex) {
            _key       = key;
            _progId    = progId;
            _exception = ex;
        }

        SPNodeKey getKey() {
            return _key;
        }

        SPProgramID getProgramId() {
            return _progId;
        }

        Throwable getProblem() {
            return _exception;
        }
    }



    // The IDBDatabase to use
    private final IDBDatabaseService _database;
    private final DuplicationMode _dupMode;
    private final int _slaveCount;
    private final IdPool _idPool;


    public ImportXmlApp(IDBDatabaseService db, DuplicationMode dupMode)  {
        _database = db;
        _dupMode  = dupMode;

        Collection<Collection<DBProgramKeyAndId>> progInfo;
        progInfo = DBSlaveSegregatedListFunctor.getProgramList(db);
        _slaveCount = progInfo.size();

        _idPool = new IdPool(progInfo);
    }

    // Import the given XML files or directories containing XML files.
    // The argument is a list of File objects.
    public void importFiles(List<File> files) {
        List<File> inputFiles = getFilesToImport(files);

        // Create executor to handle importing
        ExecutorService exec = Executors.newSingleThreadExecutor();

        // Remember the start time.
        long startTime = System.currentTimeMillis();

        // Keep up with the count of programs going into each database.
        int progCount = 0;

        // Import all the files.
        for (File f : inputFiles) {
            ImportWorker worker = new ImportWorker(_database, _slaveCount, f, _dupMode, _idPool);

            SPNodeKey key = worker.getKey();
            exec.execute(worker);
            ++progCount;
        }

        // Shutdown (waits until all tasks that have been added are finished)
        exec.shutdown();
        try {
            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // empty
        }

        _printReport(startTime, progCount);
    }

    private void _printReport(long startTime, int progCount) {
        long totalTime = System.currentTimeMillis() - startTime;

        // Remove failed programs from the count for each database.
        List<ImportProblem> problems = ImportWorker.getProblems();
        for (ImportProblem problem : problems) {
            --progCount;
        }

        // Compute and print statistics
        int sum=progCount;

        System.out.println(String.format("\n******\nImported %d programs/plans in %d ms (%.2f min)", sum, totalTime, totalTime/60000.0));

        // Show the problem imports
        if (problems.size() > 0) {
            Collections.sort(problems, new Comparator<ImportProblem>() {
                public int compare(ImportProblem ip1, ImportProblem ip2) {
                    SPProgramID id1 = ip1.getProgramId();
                    SPProgramID id2 = ip2.getProgramId();
                    if (id1 == null) {
                        if (id2 != null) return -1;
                    } else if (id2 == null) {
                        return 1;
                    } else {
                        int res = id1.compareTo(id2);
                        if (res != 0) return res;
                    }

                    return ip1.getKey().compareTo(ip2.getKey());
                }
            });

            System.out.println(String.format("\n%d programs/plans not imported", problems.size()));
            for (ImportProblem p : problems) {
                String id = p.getKey().toString();
                if (p.getProgramId() != null) {
                    id = p.getProgramId().toString();
                }
                String msg = "unknown";
                if (p.getProblem() != null) {
                    msg = p.getProblem().getMessage();
                }
                System.out.println(String.format("\t%s -> %s", id, msg));
            }
        }

    }

    private static final class ImportWorker implements Runnable {
        private static final Collection<ImportProblem> _problems = new ArrayList<ImportProblem>();

        private final IDBDatabaseService _db;
        private final File _file;
        private final DuplicationMode _dupMode;
        private final IdPool _idPool;
        private final String _kind;
        private final SPNodeKey _progKey;
        private final SPProgramID _progId;
//        private final int _dbNum;

        private static synchronized void addProblem(ImportProblem problem) {
            _problems.add(problem);
        }

        static synchronized List<ImportProblem> getProblems() {
            return new ArrayList<ImportProblem>(_problems);
        }

        ImportWorker(IDBDatabaseService db, int dbCount, File file, DuplicationMode dupMode, IdPool idPool) {
            _db = db;
            _file = file;
            _dupMode = dupMode;
            _idPool = idPool;

            // Do a quick parse of the program header to see if it is a duplicate
            final ProgIdParser p = new ProgIdParser();
            final ProgIdParser.ParsedProgId res = p.parse(file);
            _kind    = res.kind;
            _progKey = res.progKey;
            _progId  = res.progId.getOrNull();
//            _dbNum   = DbSlice.getDatabaseNumber(_progKey, dbCount);
        }

        SPNodeKey getKey() {
            return _progKey;
        }

        public void run() {
            try {
                if ("nightlyPlan".equals(_kind)) {
                    _importNightlyPlan();
                } else {
                    _importProgram();
                }
            } catch (Exception ex) {
                System.out.println("Import failed for: " + _file + ": " + ex.toString());
                addProblem(new ImportProblem(_progKey, _progId, ex));
                ex.printStackTrace();
            }
        }

        // Import a nightly plan file
        private void _importNightlyPlan()
                throws IOException, SPNodeNotLocalException, DBIDClashException {

            if (_progId == null) {
                System.out.println("No program id found for nightly plan: " + _file);
                return;
            }

            String xml = FileUtil.readFile(_file);
            SpImportFunctor functor = _db.execute(new SpImportFunctor(xml), null);

            Exception problem = functor.getProblem();
            if (problem != null) {
                System.out.println("Error parsing " + _file.getName() + ": " + problem.getMessage());
            }

            ISPNightlyRecord nightlyPlan = (ISPNightlyRecord) functor.getRootNode();
            ISPNightlyRecord oldNightlyPlan = _db.lookupNightlyRecordByID(nightlyPlan.getProgramID());
            if (oldNightlyPlan == null) {
                System.out.println(String.format("Importing (db %2d) %s", _progId));
            } else {
                if (_dupMode != DuplicationMode.update) {
                    System.out.println(_progId + " is already in the database: Skipping.");
                    return;
                }
                _db.remove(oldNightlyPlan);
                System.out.println(_progId + " is already in the database: Replacing with the imported version.");
            }
        }

        // Import a science program
        private void _importProgram() throws Exception {

            String displayStr = _file.getName();
            if (_progId != null) displayStr = _progId.toString();

            ISPProgram prog;
            if (_idPool.exists(_progKey, _progId)) {
                // handle duplicate programs
                System.out.print(String.format("%s is already in the database: ", displayStr));
                switch (_dupMode) {
                    case update:
                        // update existing program with imported version
                        System.out.println("Updating with the imported version.");
                        prog = (ISPProgram) parseXmlFile(false);
                        if (prog == null) return;
                        _idPool.markExists(prog);
                        break;
                    case add:
                        System.out.println("Importing as a new program.");
                        // import as new program, change progId to a new, unused value
                        prog = (ISPProgram)parseXmlFile(true);
                        if (prog == null) return;

                        // TODO: WHAT?
//                        SPProgramID newId = _idPool.getNewProgramId(_progId);
//                        _db.updateProgramID(prog, newId);
                        _idPool.markExists(prog);
                        break;
                    default:
                        // keep existing program
                        System.out.println("Skipping.");
                }
            } else {
                // there was no duplicate program in the database
                System.out.println(String.format("Importing %s", displayStr));
                prog = (ISPProgram)parseXmlFile(false);
                if (prog == null) return;
                _idPool.markExists(prog);
            }
        }

        // Parses the given XML file and returns the ISPProgram or ISPNightlyRecord object for it.
        private ISPNode parseXmlFile(boolean replaceKeys) throws Exception {
            String xml = FileUtil.readFile(_file);

            if (replaceKeys) {
                // make new keys and progId for program copy
                SPProgramID newProgId = null;
                if (_progId != null) newProgId = _idPool.getNewProgramId(_progId);
                xml = SpXmlParser.changeKeys(xml, newProgId);
            }

            SpUpdateFunctor functor = _db.execute(new SpUpdateFunctor(xml), null);
            Exception problem = functor.getProblem();
            if (problem != null) throw problem;
            return functor.getUpdatedProgram();
        }
    }
    */


    /**
     * Main: parse the options, initialize the database, and import the files.
     */
    public static void main(String args[]) {
        throw new Error("disabled for now");

        /*
        boolean ok = true;
        List<File> files = new ArrayList<File>();
//        boolean remoteUse = false;
        String localDB = null;
        DuplicationMode dupMode = DuplicationMode.keep;

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                String opt = args[i];
                if (opt.equals("-remote")) {
                    throw new Error("remote use not supported (for now)");
//                    DefaultJiniLifecycle.INSTANCE.startup("spModel-dl.jar");
//                    remoteUse = true;
                } else if (opt.equals("-local")) {
                    localDB = args[++i];
                } else if (opt.equals("-update") || opt.equals("-replace")) {
                    dupMode = DuplicationMode.update;
                } else if (opt.equals("-keep")) {
                    dupMode = DuplicationMode.keep;
                } else if (opt.equals("-add")) {
                    dupMode = DuplicationMode.add;
                } else {
                    System.out.println("Unknown option: " + opt);
                    ok = false;
                    break;
                }
            } else {
                File file = new File(args[i]);
                if (file.exists()) {
                    files.add(file);
                } else {
                    System.out.println("File: " + file + " does not exist");
                }
            }
        }

        if (!ok) {
            System.out.println("Usage: importXML [options...] [fileOrDirectory  ...]\n\n"
                    + "Options:\n\n"
                    + "Database options:\n"
                    + " -remote          specifies that OT should use a remote database.\n"
                    + " -local dir       specifies the location of a local database\n"
                    + "                  (default: <user.home>/.jsky/spdb)\n\n"
                    + " -lookupHost host may be passed to specify the host where the remote \n"
                    + "                  database is running.\n\n"
                    + " These options control what happens when the imported program is already in the database:\n"
                    + "  -update         update with new imported program\n"
                    + "  -keep           keep the existing program\n"
                    + "  -add            add as new program\n\n"
                    + "Any other arguments should be XML files containing science programs or\n"
                    + "directories for bulk import (default: current dir).");

            System.exit(1);
        }

        if (files.size() == 0) {
            files.add(new File(System.getProperty("user.dir")));
        }

        try {
            IDBDatabaseService db = getDatabase(localDB);
            ImportXmlApp importXML = new ImportXmlApp(db, dupMode);
            importXML.importFiles(files);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
        */
    }
}

