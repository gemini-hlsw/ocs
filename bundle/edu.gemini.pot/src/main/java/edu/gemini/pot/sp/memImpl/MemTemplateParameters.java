package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.ISPProgramVisitor;
import edu.gemini.pot.sp.ISPTemplateParameters;
import edu.gemini.pot.sp.SPNodeKey;

public final class MemTemplateParameters extends MemProgramNodeBase implements ISPTemplateParameters {

    private final MemProgram program;
    public MemTemplateParameters(MemProgram prog, SPNodeKey key)  {
        super(prog.getDocumentData(), key);
        program = prog;
    }

    public MemTemplateParameters(MemProgram prog, ISPTemplateParameters that, boolean preserveKeys)  {
        super(prog.getDocumentData(), that, preserveKeys);
        program = prog;
    }

    public void accept(ISPProgramVisitor visitor) {
        visitor.visitTemplateParameters(this);
    }

    @Override public MemProgram getProgram() { return program; }
}
