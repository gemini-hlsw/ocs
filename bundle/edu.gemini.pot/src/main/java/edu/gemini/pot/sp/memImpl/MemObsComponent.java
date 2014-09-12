package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;

/**
 * This class implements an in-memory, non-persistent ISPObsComponent object.
 */
public final class MemObsComponent extends MemProgramNodeBase implements ISPObsComponent {

    private final MemProgram program;
    private final SPComponentType _type;

    MemObsComponent(MemProgram prog, SPComponentType type, SPNodeKey key) {
        super(prog.getDocumentData(), key);
        program = prog;
        _type = type;
    }

    MemObsComponent(MemProgram prog, ISPObsComponent obsComp, boolean preserveKeys) {
        super(prog.getDocumentData(), obsComp, preserveKeys);
        program = prog;
        _type = obsComp.getType();
    }

    public SPComponentType getType() {
        return _type;
    }

    protected void attachTo(MemAbstractContainer node) throws SPNodeNotLocalException, SPTreeStateException {
        if (!haveProgramWriteLock()) throw new IllegalStateException("Do not have program write lock.");
        super.attachTo(node);
    }

    protected void detachFrom(MemAbstractBase node) {
        if (!haveProgramWriteLock()) throw new IllegalStateException("Do not have program write lock.");
        super.detachFrom(node);
    }

    public void accept(ISPProgramVisitor visitor) {
        visitor.visitObsComponent(this);
    }

    @Override public MemProgram getProgram() { return program; }
}

