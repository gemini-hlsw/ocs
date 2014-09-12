package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;

import static edu.gemini.pot.sp.SPComponentType.OBS_QA_LOG;

public final class MemObsQaLog extends MemProgramNodeBase implements ISPObsQaLog {
    private final MemProgram program;

    MemObsQaLog(MemProgram prog, SPNodeKey key) {
        super(prog.getDocumentData(), key);
        program = prog;
    }

    MemObsQaLog(MemProgram prog, ISPObsQaLog log, boolean preserveKeys) {
        super(prog.getDocumentData(), log, preserveKeys);
        program = prog;
    }

    @Override public final SPComponentType getType() { return OBS_QA_LOG; }

    @Override public final void accept(ISPProgramVisitor visitor) {
        visitor.visitObsQaLog(this);
    }

    @Override public MemProgram getProgram() { return program; }
}
