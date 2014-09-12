//
// $
//

package edu.gemini.spdb.shell.misc;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.timeacct.TimeAcctAllocation;
import edu.gemini.spModel.timeacct.TimeAcctCategory;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A functor that will set the {@link TimeAcctAllocation} for a program.  This
 * class has a main which should be invoked with the following arguments:
 * <p>
 * <code>progid cat=hours cat=hours ...</code>
 * <br>
 * where cat is a {@link edu.gemini.spModel.timeacct.TimeAcctCategory} and
 * hours are the time to associate with the category.  For example:
 * <br>
 * <code>GS-2008A-Q-23 US=3.0 UK=2.1</code>
 * <br>
 */
public final class TimeAcctSetterFunctor extends DBAbstractFunctor {
    private static final Logger LOG = Logger.getLogger(TimeAcctSetterFunctor.class.getName());

    private final SPProgramID _progId;
    private final TimeAcctAllocation _alloc;

    private TimeAcctSetterFunctor(final SPProgramID progId, final TimeAcctAllocation alloc) {
        _progId   = progId;
        _alloc    = alloc;
    }

    public void execute(final IDBDatabaseService db, final ISPNode node, Set<Principal> principals) {
            final ISPProgram prog = db.lookupProgramByID(_progId);
            if (prog == null) {
                LOG.warning("Program not found: " + _progId);
                return;
            }
            final SPProgram dataObj = (SPProgram) prog.getDataObject();
            dataObj.setTimeAcctAllocation(_alloc);
            prog.setDataObject(dataObj);
    }

    private static void usage() {
        System.err.println("progid cat=hours cat=hours ....\nprogid cat cat ...");
        throw new RuntimeException("progid cat=hours cat=hours ....\nprogid cat cat ...");
    }

    private static SPProgramID parseProgId(final String arg) {
        try {
            return SPProgramID.toProgramID(arg);
        } catch (SPBadIDException e) {
            System.err.println("Bad program id: " + arg);
            throw new RuntimeException("Bad program id: " + arg);
        }
    }

    private static final Pattern TIME_ALLOC_PATTERN = Pattern.compile("([A-Z]+)=(\\d*\\.?\\d*)");

    private static TimeAcctAllocation getAllocation(final String[] args) {
        final Map<TimeAcctCategory, Double> timeMap = new HashMap<TimeAcctCategory, Double>();
        for (final String arg : args) {
            final Matcher mat = TIME_ALLOC_PATTERN.matcher(arg);
            if (!mat.matches()) {
                usage();
            }

            final String catStr = mat.group(1);
            final String timeStr = mat.group(2);

            TimeAcctCategory cat = null;
            try {
                cat = TimeAcctCategory.valueOf(catStr);
            } catch (Exception ex) {
                // ignore
            }
            if (cat == null) {
                System.err.println("Unknown category: " + catStr);
                throw new RuntimeException("Unknown category: " + catStr);
            }

            double time = 0.0;
            try {
                time = Double.parseDouble(timeStr);
            } catch (Exception ex) {
                System.err.println("Could not parse time: " + timeStr);
               throw new RuntimeException("Could not parse time: " + timeStr);
            }

            timeMap.put(cat, time);
        }
        return new TimeAcctAllocation(timeMap);
    }

//    public static void main(final String[] args) {
//        if (args.length < 2) usage();
//
//        final SPProgramID progId = parseProgId(args[0]);
//        final String[] taArgs = new String[args.length - 1];
//        System.arraycopy(args, 1, taArgs, 0, taArgs.length);
//        final TimeAcctAllocation alloc = getAllocation(taArgs);
//
//        final TimeAcctSetterFunctor func = new TimeAcctSetterFunctor(progId, alloc);
//
//        System.out.println("Updating " + progId.toString() + ": " + alloc);
//
//        final IDBDatabaseService dbRef = SPDB.get();
//
//        try {
//            dbRef.getQueryRunner().execute(func, null);
//        } catch (Exception ex) {
//            System.err.println("Problem: ");
//            ex.printStackTrace();
//            throw new RuntimeException(ex);
//        }
//
//        System.out.println("done");
//    }

}
