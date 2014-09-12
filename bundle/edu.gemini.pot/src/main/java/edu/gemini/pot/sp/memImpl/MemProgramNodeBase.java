package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.ISPProgramNode;
import edu.gemini.pot.sp.SPNodeKey;

import java.io.Serializable;

/**
 *
 */
public abstract class MemProgramNodeBase extends MemAbstractBase implements ISPProgramNode, Serializable {
    protected MemProgramNodeBase(DocumentData documentData, SPNodeKey nodeKey) {
        super(documentData, nodeKey);
    }

    protected MemProgramNodeBase(DocumentData documentData, ISPProgramNode node, boolean preserveKeys) {
        super(documentData, node, preserveKeys);
    }

    public abstract MemProgram getProgram();
}
