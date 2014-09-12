package edu.gemini.spdb.reports.collection.table;

import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.core.ProgramType$;
import edu.gemini.spModel.core.SPProgramID;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class TypeCheck {
    private TypeCheck() {}

    private static final Set<ProgramType> SCIENCE_TYPES = new HashSet<ProgramType>(
   		Arrays.asList(new ProgramType[] {
                   ProgramType.Classical$.MODULE$,
                   ProgramType.DirectorsTime$.MODULE$,
                   ProgramType.DemoScience$.MODULE$,
                   ProgramType.LargeProgram$.MODULE$,
                   ProgramType.FastTurnaround$.MODULE$,
                   ProgramType.Queue$.MODULE$,
                   ProgramType.SystemVerification$.MODULE$,
           }));

    public static boolean isScienceType(SPProgramID pid) {
        if (pid == null) return false;

        final ProgramType pt = ProgramType$.MODULE$.readOrNull(pid);
        return (pt != null) && SCIENCE_TYPES.contains(pt);
    }

    public static boolean is(SPProgramID pid, ProgramType pt) {
        return ProgramType$.MODULE$.readOrNull(pid) == pt;
    }
}
