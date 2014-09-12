package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;
import static edu.gemini.pot.sp.SPComponentType.OBS_EXEC_LOG;

public final class MemObsExecLog extends MemProgramNodeBase implements ISPObsExecLog {
    private final MemProgram program;

    MemObsExecLog(MemProgram prog, SPNodeKey key) {
        super(prog.getDocumentData(), key);
        program = prog;
    }

    MemObsExecLog(MemProgram prog, ISPObsExecLog log, boolean preserveKeys) {
        super(prog.getDocumentData(), log, preserveKeys);
        program = prog;
    }

    @Override public final SPComponentType getType() { return OBS_EXEC_LOG; }

    @Override public final void accept(ISPProgramVisitor visitor) {
        visitor.visitObsExecLog(this);
    }

    @Override public MemProgram getProgram() { return program; }
}
