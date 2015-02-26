package edu.gemini.spdb.reports.collection.table;

import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.core.ProgramType$;
import edu.gemini.spModel.core.SPProgramID;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class TypeCheck {
    private TypeCheck() {}

    // set of program types that are relevant for internal reports
    private static final Set<ProgramType> SCIENCE_TYPES = new HashSet<>(
   		Arrays.asList(new ProgramType[] {
                   ProgramType.Classical$.MODULE$,
                   ProgramType.DirectorsTime$.MODULE$,
                   ProgramType.DemoScience$.MODULE$,
                   ProgramType.LargeProgram$.MODULE$,
                   ProgramType.FastTurnaround$.MODULE$,
                   ProgramType.Queue$.MODULE$,
                   ProgramType.SystemVerification$.MODULE$,
           }));

    public static boolean isScienceType(final SPProgramID pid) { return isAnyOf(pid, SCIENCE_TYPES); }

    public static boolean isAnyOf(final SPProgramID pid, final Set<ProgramType> types) {
        if (pid == null) return false;

        final ProgramType pt = ProgramType$.MODULE$.readOrNull(pid);
        return (pt != null) && types.contains(pt);
    }

    public static boolean is(final SPProgramID pid, final ProgramType pt) {
        return ProgramType$.MODULE$.readOrNull(pid) == pt;
    }
}
