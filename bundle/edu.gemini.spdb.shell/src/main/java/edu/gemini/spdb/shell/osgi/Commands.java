package edu.gemini.spdb.shell.osgi;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBQueryFunctor;
import edu.gemini.pot.spdb.IDBQueryRunner;
import edu.gemini.shared.util.immutable.ImEither;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Left;
import edu.gemini.shared.util.immutable.Right;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.io.SpImportService;
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProviderHolder;
import edu.gemini.spModel.gemini.inst.InstRegistry;
import edu.gemini.spdb.shell.misc.EphemerisPurgeCommand;
import static edu.gemini.spdb.shell.misc.EphemerisPurgeCommand.*;
import edu.gemini.spdb.shell.misc.ExportXmlCommand;
import edu.gemini.spdb.shell.misc.ExportOcs3Command;
import edu.gemini.spdb.shell.misc.ImportXmlCommand;
import edu.gemini.spdb.shell.misc.LsProgs;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

class Commands {

    private final ServiceTracker tracker;
    private final Set<Principal> user;

    Commands(final ServiceTracker tracker, Set<Principal> user) {
        this.tracker = tracker;
        this.user = user;
    }

    private IDBDatabaseService db() {
        final IDBDatabaseService db = (IDBDatabaseService) tracker.getService();
        if (db == null)
            throw new IllegalStateException("No database is available.");
        return db;
    }

    private interface Query {
        IDBQueryFunctor doQuery(IDBQueryRunner run, IDBQueryFunctor fun);
    }

    private static final Query PROG_QUERY = IDBQueryRunner::queryPrograms;

    private static final Query PLAN_QUERY = IDBQueryRunner::queryNightlyPlans;

    // return all the program ids formatted in columns
    public String lsprogs() {
        return lsRoots(PROG_QUERY);
    }

    public String lsplans() {
        return lsRoots(PLAN_QUERY);
    }

    private String lsRoots(final Query q) {
        final int cols = 5;
        final StringBuilder sb = new StringBuilder();
        int col = -1;
        for (final Object id : ((LsProgs) q.doQuery(db().getQueryRunner(user), new LsProgs())).ids()) {
            if (++col == cols) {
                sb.append('\n');
                col = 0;
            }
            sb.append(String.format("%-18s", id));
        }
        return sb.toString();
    }


    // import xml files
    public String importXml(final File path) throws Throwable {
        return importXml(path, "keep");
    }

    public String importXml(final File path, final String option) throws Throwable {
        try {
            final SpImportService.ImportDirective op;
            if ("keep".equals(option)) {
                op = SpImportService.Skip$.MODULE$;
            } else if ("replace".equals(option)) {
                op = SpImportService.Replace$.MODULE$;
            } else if ("copy".equals(option)) {
                op = SpImportService.Copy$.MODULE$;
            } else {
                return ("Option must be one of { copy, keep, replace }");
            }

            new ImportXmlCommand(db(), path, op).importXML();

            return "Done.";

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }

    }

    private static ImEither<String, List<SPProgramID>> parsePids(String[] pidStrings) {
        final List<SPProgramID> pids = new ArrayList<>(pidStrings.length);
        for (String id : pidStrings) {
            try {
                pids.add(SPProgramID.toProgramID(id));
            } catch (SPBadIDException e) {
                return new Left<>("Could not parse '" + id + "' as a program ID.");
            }
        }
        return new Right<>(pids);
    }

    public String exportXml(final File path, final String... progIdStrings) {
        if (!path.isDirectory()) return ("Not a directory: " + path);

        return parsePids(progIdStrings).biFold(err -> err, pids -> {
            try {
                new ExportXmlCommand(db(), path, user).exportXML(pids);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
            return "Done.";
        });
    }

    public String exportOcs3(final File path, final String... progIdStrings) {
        if (!path.isDirectory()) return String.format("Not a directory: %s", path);

        return parsePids(progIdStrings).biFold(err -> err, pids -> {
            try {
                new ExportOcs3Command(db(), path, user).exportOcs3(pids);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
            return "Done.";
        });
    }

    /**
     * Exports smart gcal data files in a format that is amenable to parsing
     * without special cases.
     *
     * @param dir directory into which the files will be written or updated
     *
     * @return nothing of importance
     *
     * @throws IOException
     */
    public String exportSmartGcal(final File dir) throws IOException {
        if (!dir.isDirectory()) return String.format("Not a directory: %s", dir);

        final Path dirPath = dir.toPath();
        final Charset utf8 = Charset.forName("UTF-8");

        final CalibrationProvider cp = CalibrationProviderHolder.getProvider();
        for (SPComponentType i : InstRegistry.instance.types()) {
            for (Calibration.Type ct : Calibration.Type.values()) {
                final Stream<ImList<String>> config = cp.export(ct, i.readableStr);
                final String               fileName = String.format("%s_%s.csv", i.readableStr, ct.name());

                System.out.print(String.format("Writing %-30s ... ", fileName));

                final Path p = dirPath.resolve(fileName);
                try (final BufferedWriter w = Files.newBufferedWriter(p, utf8, CREATE, TRUNCATE_EXISTING, WRITE)) {
                    config.map(l -> l.mkString("", ",", "\n")).forEach(s -> {
                        try {
                            w.write(s);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }

                // We don't know whether the Stream is empty in time to avoid
                // creating the file. (There is no Stream.isEmpty().)  If we
                // created an empty file, delete it.
                if (Files.size(p) == 0) {
                    System.out.println("skipped.");
                    Files.delete(p);
                } else {
                    System.out.println("done.");
                }
            }
        }

        return "Done";
    }

    // export xml files
    public String exportXml(final File path) {
        return exportXml(path, new String[0]);
    }

    public String du() {
        return String.format("Total SPDB storage %,d bytes.", db().getDBAdmin().getTotalStorage());
    }

    private final String PURGE_CONFIRMATION = "" + System.currentTimeMillis();

    public String purge() {
        return String.format("This will remove all programs in the database.\nThis operation cannot be undone.\nTo confirm, enter:\n   purge %s", PURGE_CONFIRMATION);
    }

    public String purge(String confirm) {
        if (PURGE_CONFIRMATION.equals(confirm)) {
            final IDBDatabaseService db = db();
            db.getQueryRunner(user).queryPrograms(new DBAbstractQueryFunctor() {
                public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
                    if (node instanceof ISPProgram) {
                        final ISPProgram p = (ISPProgram) node;
                        System.out.format("Deleting %s %s\n", p.getNodeKey(), p.getProgramID());
                        db.remove(p);
                    }
                }
            });
            return null;
        } else {
           return String.format("Incorrect confirmation (expected %s)", PURGE_CONFIRMATION);
        }
    }

    private ImEither<String, ISPProgram> prog(String programId) {
        final IDBDatabaseService db = db();
        final SPProgramID pid;
        try {
            pid = SPProgramID.toProgramID(programId);
        } catch (SPBadIDException ex) {
            return new Left<>(String.format("%s: illegal program id", programId));
        }

        final ISPProgram p = db.lookupProgramByID(pid);
        return (p == null) ? new Left<>(String.format("%s: not in db", programId)) : new Right<>(p);
    }

    public String rmprog(String programId) {
        final ImEither<String, ISPProgram> e = prog(programId);
        e.forEach(p -> db().remove(p));
        return e.biFold(s -> s, p -> "");
    }

    public String rmprog(String[] programIds) {
        final StringBuilder buf = new StringBuilder();
        for (String pid : programIds) {
            String res = rmprog(pid);
            if (!"".equals(res)) buf.append(res).append("\n");
        }
        return buf.toString();
    }

    public String rmprog(List<String> programIds) {
        return rmprog(programIds.toArray(new String[programIds.size()]));
    }

    public String purgeEphemeris(String programId) {
        return purgeEphemeris(programId, ObservedOnly$.MODULE$.displayValue());
    }

    public String purgeEphemeris(String programId, String purgeOption) {
        final ImEither<String, ISPProgram> e = prog(programId);
        final scala.Option<PurgeOption>   po = PurgeOption$.MODULE$.fromDisplayValue(purgeOption);

        return ImEither.merge(e.flatMap(p -> po.isDefined() ?
            new Right<>(EphemerisPurgeCommand.apply(p, po.get())) :
            new Left<>("Usage: purgeEphemeris programId " + PurgeOption$.MODULE$.usageString())
        ));
    }
}
