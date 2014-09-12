package edu.gemini.spdb.shell.misc.deactivate;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
//import joptsimple.OptionParser;
//import joptsimple.OptionSet;

import java.io.*;
import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Functor to handle backup and restore activations status for programs */
public class ActivationStatusBackupFunctor extends DBAbstractQueryFunctor {
    private static final Logger LOG = Logger.getLogger(ActivationStatusBackupFunctor.class.getName());

    private static final Pattern SEMESTER_PATTERN = Pattern.compile("\\d\\d\\d\\d[AB]");

    interface BackupRestoreWorker {
        void execute(ISPNode node);
        List<SPProgramIDProperty<Boolean>> getOutput();
    }

    /** Handles the backup */
    private static class Backup implements BackupRestoreWorker, Serializable {

        interface IdMatcher extends Serializable {
            boolean matches(SPProgramID id);
        }

        private static final IdMatcher FULL_MATCHER = new IdMatcher() {
            public boolean matches(final SPProgramID id) {
                return true;
            }
        };

        private static class SemesterMatcher implements IdMatcher, Serializable {
            private final Pattern _pattern;

            SemesterMatcher(final String semester) {
                final Matcher mat = SEMESTER_PATTERN.matcher(semester);
                if (!mat.matches()) {
                    LOG.severe("Not a valid semester: " + semester);
                    throw new IllegalArgumentException("not a semester: '" + semester + "'");
                }

                /** TODO: include LP? */
                _pattern = Pattern.compile("G[NS]-" + semester + "-Q-.*");
            }

            public boolean matches(final SPProgramID id) {
                final Matcher mat = _pattern.matcher(id.stringValue());
                return mat.matches();
            }
        }

        private final List<SPProgramIDProperty<Boolean>> _output = new ArrayList<SPProgramIDProperty<Boolean>>();
        private final IdMatcher matcher;

        Backup(final String semester, final boolean full) {
            matcher = full ? FULL_MATCHER : new SemesterMatcher(semester);
        }

        public List<SPProgramIDProperty<Boolean>> getOutput() {
            return _output;
        }

        public void execute(final ISPNode node) {
            final ISPProgram prog = (ISPProgram) node;
            final SPProgramID id = prog.getProgramID();
            if (id == null) return;

            final SPProgram dobj = (SPProgram) prog.getDataObject();
            final SPProgram.Active curStatus = dobj.getActive();

            if (matcher.matches(id)) {
                if (curStatus == SPProgram.Active.NO) {
                    _output.add(new SPProgramIDProperty<Boolean>(id, false));
                } else {
                    _output.add(new SPProgramIDProperty<Boolean>(id, true));
                }
            }
        }
    }

    /** Handles the Restore */
    class Restore implements BackupRestoreWorker, Serializable {
        private final List<SPProgramIDProperty<Boolean>> _output = new ArrayList<SPProgramIDProperty<Boolean>>();
        private Set<String> _toActivate = null, _toDeactivate = null;

        Restore(final Set<String> toActivate, final Set<String> toDeactivate) {
            _toActivate = toActivate;
            _toDeactivate = toDeactivate;
        }

        public List<SPProgramIDProperty<Boolean>> getOutput() {
            return _output;
        }

        public void execute(final ISPNode node) {
            final ISPProgram prog = (ISPProgram) node;
            final SPProgramID id = prog.getProgramID();

            if (id == null) return;

            final SPProgram dobj = (SPProgram) prog.getDataObject();
            final SPProgram.Active curStatus = dobj.getActive();

            if (_toActivate.contains(id.stringValue())) {
                if (curStatus == SPProgram.Active.NO) {
                    dobj.setActive(SPProgram.Active.YES);
                    prog.setDataObject(dobj);
                    _output.add(new SPProgramIDProperty<Boolean>(id, true));
                }
            } else if (_toDeactivate.contains(id.stringValue())) {
                if (curStatus == SPProgram.Active.YES) {
                    dobj.setActive(SPProgram.Active.NO);
                    prog.setDataObject(dobj);
                    _output.add(new SPProgramIDProperty<Boolean>(id, false));
                }
            }
        }
    }

    /** handles the actual backup/restore */
    private final BackupRestoreWorker worker;

    /**
     * Constructor for backing up activation statuses
     *
     * @param semester the semester to backup. Can be null if 'full' is true.
     * @param full     backup all semesters.
     */
    private ActivationStatusBackupFunctor(final String semester, final boolean full) {
        worker = new Backup(semester, full);
    }

    /**
     * Constructor to restore the activation statuses
     *
     * @param toActivate   programs to be activated
     * @param toDeactivate programs to be deactivated
     */
    private ActivationStatusBackupFunctor(final Set<String> toActivate, final Set<String> toDeactivate) {
        worker = new Restore(toActivate, toDeactivate);
    }

    public void execute(final IDBDatabaseService db, final ISPNode node, Set<Principal> principals) {
        worker.execute(node);
    }

    List<SPProgramIDProperty<Boolean>> getOutput() {
        return worker.getOutput();
    }

    private static void printOutput(final List<SPProgramIDProperty<Boolean>> output, final OutputStream out) {
        final PrintWriter _out = new PrintWriter(out);
        for (final SPProgramIDProperty idProp : output) {
            _out.println(idProp.id.stringValue() + ((Boolean) idProp.property ? "\tactive" : "\tinactive"));
        }
        _out.close();
    }

    public static void main(final String[] args) throws Exception {

        // TODO

//        final OptionParser parser = new OptionParser();
//        parser.accepts("semester", "The semester to backup (ex. 2010B), no semester means full backup").withRequiredArg().ofType(String.class);
//        parser.accepts("file", "The file to write the backup to, or read from.").withRequiredArg().ofType(String.class);
//        parser.accepts("restore", "Restores the program to the state given in a file.");
//        final OptionSet options = parser.parse(args);
//        if (options.has("restore")) {
//            if (options.has("semester") || !options.has("file")) {
//                System.out.println("If you want to restore the database you need to specify a restore file and don't specify any semester.");
//                parser.printHelpOn(System.out);
//                return;
//            }
//            final BufferedReader in = new BufferedReader(new FileReader((String) options.valueOf("file")));
//            final Set<String> toActivate = new TreeSet<String>();
//            final Set<String> toDeactivate = new TreeSet<String>();
//            while (in.ready()) {
//                final String line = in.readLine();
//                final String[] splitLine = line.split("\t");
//                if (splitLine[1].compareTo("active") == 0) {
//                    toActivate.add(splitLine[0]);
//                } else if (splitLine[1].compareTo("inactive") == 0) {
//                    toDeactivate.add(splitLine[0]);
//                } else {
//                    System.out.println("Restore file has the wrong format");
//                }
//            }
//            in.close();
//            ActivationStatusBackupFunctor func;
//            func = new ActivationStatusBackupFunctor(toActivate, toDeactivate);
//            final IDBDatabaseService dbref = SPDB.get();
//            func = dbref.getQueryRunner().queryPrograms(func);
//            Collections.sort(func.getOutput(), SPProgramIDProperty.ID_COMPARATOR);
//            printOutput(func.getOutput(), System.out);
//        } else {
//            String semester = null;
//            boolean full = false;
//            if (options.has("semester")) {
//                semester = (String) options.valueOf("semester");
//            } else {
//                full = true;
//            }
//            ActivationStatusBackupFunctor func;
//            func = new ActivationStatusBackupFunctor(semester, full);
//            final IDBDatabaseService dbref = SPDB.get();
//            func = dbref.getQueryRunner().queryPrograms(func);
//            Collections.sort(func.getOutput(), SPProgramIDProperty.ID_COMPARATOR);
//            if (options.has("file")) {//output to file
//                printOutput(func.getOutput(), new FileOutputStream((String) options.valueOf("file")));
//            } else {//output to stdout
//                printOutput(func.getOutput(), System.out);
//            }
//        }
    }
}

