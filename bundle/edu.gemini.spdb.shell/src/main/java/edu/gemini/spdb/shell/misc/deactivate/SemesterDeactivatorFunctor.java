//
// $Id: SemesterDeactivatorFunctor.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spdb.shell.misc.deactivate;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
//import joptsimple.OptionParser;
//import joptsimple.OptionSet;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Functor to deactivate programs at the end of a semester */
public class SemesterDeactivatorFunctor extends DBAbstractQueryFunctor {

    private static final Logger LOG = Logger.getLogger(SemesterDeactivatorFunctor.class.getName());
    private static final Pattern SEMESTER_PATTERN = Pattern.compile("\\d\\d\\d\\d[AB]");

    private final List<SPProgramIDProperty<String>> _messages = new ArrayList<SPProgramIDProperty<String>>();
    private final Pattern _pattern;
    private final List<Pattern> _skip = new ArrayList<Pattern>();

    /**
     * Constructor
     *
     * @param semester the semester to deactivate
     * @param toSkip   Array of programs to skip
     */
    private SemesterDeactivatorFunctor(final String semester, final String[] toSkip) {
        final Matcher mat = SEMESTER_PATTERN.matcher(semester);
        if (!mat.matches()) {
            LOG.severe("Not a valid semester: " + semester);
            throw new IllegalArgumentException("not a semester: '" + semester + "'");
        }
        /** TODO: include LP? */
        for (final String s : toSkip) {
            _skip.add(Pattern.compile("G[NS]-" + semester + "-Q-" + s));
        }
        _pattern = Pattern.compile("G[NS]-" + semester + "-Q-.*");
    }

    public void execute(final IDBDatabaseService db, final ISPNode node, Set<Principal> principals) {
        final ISPProgram prog = (ISPProgram) node;

        // Throw away programs from other semesters.
        final SPProgramID id = prog.getProgramID();
        if (id == null) return;

        final Matcher mat = _pattern.matcher(id.stringValue());

        if (!mat.matches()) {
            return;
        }

        for (final Pattern p : _skip) {
            if (p.matcher(id.stringValue()).matches()) {
                _messages.add(new SPProgramIDProperty<String>(id, "skipping (was on the skip list)"));
                return;
            }
        }

        // Get the data object to check whether this is a rollover program.
        final SPProgram dobj = (SPProgram) prog.getDataObject();
        if (dobj.getRolloverStatus()) {
            _messages.add(new SPProgramIDProperty<String>(id, "skipped (is a rollover program)"));
            return;
        }

        // Okay, semester matches and this is not a rollover program.
        // So inactivate it.
        final SPProgram.Active curStatus = dobj.getActive();
        if (curStatus == SPProgram.Active.NO) {
            _messages.add(new SPProgramIDProperty<String>(id, "skipped (already inactive)"));
            return;
        }
        dobj.setActive(SPProgram.Active.NO);
        prog.setDataObject(dobj);

        _messages.add(new SPProgramIDProperty<String>(id, "deactived"));

    }

    public static void main(final String[] args) throws Exception {

        // TODO:

//        final OptionParser parser = new OptionParser();
//        parser.accepts("semester", "The semester to deactivate (ex. 2010B)").withRequiredArg().ofType(String.class);
//        parser.accepts("skip", "Programs to skip, separated by ':' (ex. if semester is 2010B, then \"--skip 23:2\" will skip G?-2010B-Q-23 and G?-2010B-Q-2").withRequiredArg().ofType(String.class).withValuesSeparatedBy(':');
//
//        final OptionSet options = parser.parse(args);
//        if (!options.has("semester")) {
//            parser.printHelpOn(System.out);
//            return;
//        }
//        final String semester = (String) options.valueOf("semester");
//
//        //noinspection unchecked
//        final List<String> skipList = (List<String>) options.valuesOf("skip");
//
//        final String[] skips = skipList.toArray(new String[0]);
//        SemesterDeactivatorFunctor func;
//        func = new SemesterDeactivatorFunctor(semester, skips);
//        final IDBDatabaseService dbref = SPDB.get();
//        func = dbref.getQueryRunner().queryPrograms(func);
//        Collections.sort(func._messages, SPProgramIDProperty.ID_COMPARATOR);
//        for (final SPProgramIDProperty idProp : func._messages) {
//            LOG.info(idProp.id.stringValue() + "\t: " + idProp.property);
//        }


        /*

The first one is to backup the current state of the database in case of problems. Usage to backup:
    backupActivationStatus [-lu look_up_host] [-group group] [--semester 2010B] [--file filename]
Usage to restore from a backup:
    backupActivationStatus --restore --file filename
For example, the specific command I used today was:
    backupActivationStatus -lu gnodb -group production --semester 2010A --file 2010Abackup.gn
Which backs up all 2010A program statuses from gnodb
The other script is to deactivate programs. Usage:
    deactivatePrograms --semester 2010B [--skip 23:2]
Actual command used today:
    deactivatePrograms -lu gnodb -group production --semester 2010A --skip 5:10:17:18:24:56:91:99
Which deactivates all 2010A programs in gnodb except for (Q5, Q10, Q17, Q18, Q24, Q56, Q91, and Q99). Rollover programs are implicitly skipped.
I'll put this information online as soon as I figure where to put it.
         */
    }
}
