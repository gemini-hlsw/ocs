//
// $Id: SpidMatcher.java 4788 2004-06-19 14:05:44Z shane $
//
package jsky.app.ot.viewer;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.ProgramId;
import edu.gemini.spModel.core.ProgramType;

import java.io.Serializable;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Semester;
import scala.Option;

/**
 * SpidMatcher is a simple interface for matching program ids.  It should
 * return true if according to its rules a given program id matches.
 */
public interface SpidMatcher extends Serializable {

    /**
     * An implementation based upon Java regular expression patterns.
     * Construct with a pattern and the matches method will return true if
     * the program id matches the pattern.
     */
    final class Pattern implements SpidMatcher {
        private final java.util.regex.Pattern _pattern;

        public Pattern(String patternStr) {
            _pattern = java.util.regex.Pattern.compile(patternStr);
        }

        public boolean matches(Option<ProgramId> pid) {
            return pid.isDefined() && _pattern.matcher(pid.get().toString()).matches();
        }
    }

    /**
     * Matches on program type.
     */
    final class TypeMatcher implements SpidMatcher {
        private final ProgramType _type;
        public TypeMatcher(ProgramType t) {
            _type = t;
        }
        public boolean matches(Option<ProgramId> pid) {
            return pid.isDefined() && pid.get().ptype().isDefined() &&
                    pid.get().ptype().get().equals(_type);
        }
    }

    /**
     * Matches on semester.
     */
    final class SemesterMatcher implements SpidMatcher {
        private final edu.gemini.shared.util.immutable.Option<Semester> semester;

        SemesterMatcher(edu.gemini.shared.util.immutable.Option<Semester> semester) {
            this.semester = semester;
        }

        public boolean matches(scala.Option<ProgramId> pid) {
            // Sorry, this is awkward mixing Scala and Java Option and
            // worse still, using it from Java.
            return semester.isEmpty() ||  // match anything
                    (!pid.isEmpty() && !pid.get().semester().isEmpty() && pid.get().semester().get().equals(semester.getValue()));
        }
    }

    /**
     * Matches if the given program is known (local) to the given database.
     */
    final class LocalMatcher implements SpidMatcher {
        private final IDBDatabaseService db;

        LocalMatcher(IDBDatabaseService db) { this.db = db; }
        private SPProgramID toSpProgramId(scala.Option<ProgramId> pid) {
            try {
                return pid.isEmpty() ? null : SPProgramID.toProgramID(pid.get().toString());
            } catch (Exception ex) {
                return null;
            }
        }

        @Override public boolean matches(scala.Option<ProgramId> pid) {
            final SPProgramID spid = toSpProgramId(pid);
            return (spid == null) || (db.lookupProgramByID(spid) != null);
        }
    }

    /**
     * An implementation that returns the OR of two SpidMatchers.  If either
     * matches, then the matches method returns true.  If the first matches,
     * then the second matcher is not consulted.
     */
    final class Or implements SpidMatcher {
        private final SpidMatcher _first;
        private final SpidMatcher _second;

        public Or(SpidMatcher first, SpidMatcher second, SpidMatcher... rest) {
            _first = first;
            SpidMatcher tmp = second;
            for (SpidMatcher m: rest) tmp = new Or(tmp, m);
            _second = tmp;
        }

        public boolean matches(Option<ProgramId> pid) {
            return _first.matches(pid) || _second.matches(pid);
        }
    }

    /**
     * An implementation that returns the AND of two SpidMatchers.  If both
     * match, then the matches method returns true.  If the first doesn't
     * match, then the second matcher is not consulted.
     */
    final class And implements SpidMatcher {
        private final SpidMatcher _first;
        private final SpidMatcher _second;

        public And(SpidMatcher first, SpidMatcher second, SpidMatcher... rest) {
            _first = first;
            SpidMatcher tmp = second;
            for (SpidMatcher m: rest) tmp = new And(tmp, m);
            _second = tmp;
        }

        public boolean matches(Option<ProgramId> pid) {
            return _first.matches(pid) && _second.matches(pid);
        }
    }


    /**
     * An implementation that reveres the logic of a given SpidMather.
     */
    final class Not implements SpidMatcher {
        private final SpidMatcher _not;
        public Not(SpidMatcher not) { _not = not; }

        public boolean matches(Option<ProgramId> pid) {
            return !_not.matches(pid);
        }
    }

    /**
     * A matcher that always returns true regardless of its argument.
     */
    SpidMatcher TRUE = new SpidMatcher() {
        public boolean matches(Option<ProgramId> pid) {
            return true;
        }
    };

    /**
     * A matcher that always returns false regardless of its argument.
     */
    SpidMatcher FALSE = new SpidMatcher() {
        public boolean matches(Option<ProgramId> pid) {
            return false;
        }
    };

    /**
     * Returns <code>true</code> if the given <code>progid</code> matches
     * according to the rules of this implementation.
     *
     * @param pid program id to match
     *
     * @return <code>true</code> if the id matches, <code>false</code>
     * otherwise
     */
    boolean matches(Option<ProgramId> pid);
}
