package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

public final class MemConflictFolder extends MemAbstractContainer implements ISPConflictFolder {
    private static final Logger LOG = Logger.getLogger(MemConflictFolder.class.getName());

    private static final String CONFLICT_CHILDREN_PROP = "ConflictChildren";

    private static final class NodeCopier implements ISPProgramVisitor {
        private final ISPProgram prog;
        private final ISPFactory fact;
        private final boolean preserveKeys;

        private ISPNode copy;

        NodeCopier(ISPProgram prog, ISPFactory fact, boolean preserveKeys) {
            this.prog = prog;
            this.fact = fact;
            this.preserveKeys = preserveKeys;
        }

        public ISPNode getCopy() { return copy; }

        @Override public void visitConflictFolder(ISPConflictFolder node) {
            try {
                copy = fact.createConflictFolderCopy(prog, node, preserveKeys);
            } catch (SPUnknownIDException e) {
                throw new RuntimeException(e);
            }
        }

        @Override public void visitObsQaLog(ISPObsQaLog node) {
            try {
                copy = fact.createObsQaLogCopy(prog, node, preserveKeys);
            } catch (SPException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override public void visitObsExecLog(ISPObsExecLog node) {
            try {
                copy = fact.createObsExecLogCopy(prog, node, preserveKeys);
            } catch (SPException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override public void visitObsComponent(ISPObsComponent node) {
            try {
                copy = fact.createObsComponentCopy(prog, node, preserveKeys);
            } catch (SPUnknownIDException e) {
                throw new RuntimeException(e);
            }
        }

        @Override public void visitObservation(ISPObservation node) {
            try {
                copy = fact.createObservationCopy(prog, node, preserveKeys);
            } catch (SPException e) {
                throw new RuntimeException(e);
            }
        }

        @Override public void visitGroup(ISPGroup node) {
            try {
                copy = fact.createGroupCopy(prog, node, preserveKeys);
            } catch (SPUnknownIDException e) {
                throw new RuntimeException(e);
            }
        }

        @Override public void visitProgram(ISPProgram node) {
            throw new RuntimeException("Cannot put a program in a folder");
        }

        @Override public void visitSeqComponent(ISPSeqComponent node) {
            try {
                copy = fact.createSeqComponentCopy(prog, node, preserveKeys);
            } catch (SPUnknownIDException e) {
                throw new RuntimeException(e);
            }
        }

        @Override public void visitTemplateFolder(ISPTemplateFolder node) {
            try {
                copy = fact.createTemplateFolderCopy(prog, node, preserveKeys);
            } catch (SPUnknownIDException e) {
                throw new RuntimeException(e);
            }
        }

        @Override public void visitTemplateGroup(ISPTemplateGroup node) {
            try {
                copy = fact.createTemplateGroupCopy(prog, node, preserveKeys);
            } catch (SPUnknownIDException e) {
                throw new RuntimeException(e);
            }
        }

        @Override public void visitTemplateParameters(ISPTemplateParameters node) {
            try {
                copy = fact.createTemplateParametersCopy(prog, node, preserveKeys);
            } catch (SPUnknownIDException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final MemProgram program;
    private final List<ISPNode> children = new ArrayList<>();

    public MemConflictFolder(MemProgram prog, SPNodeKey key) {
        super(prog.getDocumentData(), key);
        this.program = prog;
    }

    public MemConflictFolder(MemProgram prog, ISPFactory fact, ISPConflictFolder folder, boolean preserveKeys) {
        super(prog.getDocumentData(), folder, preserveKeys);
        this.program = prog;

        final List<ISPNode> childrenCopy = folder.getChildren();
        final ListIterator<ISPNode> lit  = childrenCopy.listIterator();
        while (lit.hasNext()) {
            final NodeCopier nc = new NodeCopier(prog, fact, preserveKeys);
            ((ISPProgramNode) lit.next()).accept(nc);
            lit.set(nc.getCopy());
        }

        getProgramWriteLock();
        try {
            updateChildren(children, childrenCopy);
        } catch (SPException ex) {
            throw new RuntimeException(ex);
        } finally {
            returnProgramWriteLock();
        }
    }

    @Override protected void setTypedChildren(TypedChildren tc) throws SPException {
        // not used
    }

    @Override public List<ISPNode> getChildren() {
        getProgramReadLock();
        try {
            return new ArrayList<>(children);
        } finally {
            returnProgramReadLock();
        }
    }

    @Override public void setChildren(List<ISPNode> newChildren) throws SPException {
        checkChildTypes(children, ISPNode.class);

        final List<ISPNode> newCopy = new ArrayList<>(newChildren);
        getProgramWriteLock();
        try {
            final List<ISPNode> oldCopy = new ArrayList<>(children);
            updateChildren(children, newCopy);
            firePropertyChange(CONFLICT_CHILDREN_PROP, oldCopy, newCopy);
            fireStructureChange(CONFLICT_CHILDREN_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    @Override public void removeChild(ISPNode child) {
        MemAbstractBase node = (MemAbstractBase) child;
        getProgramWriteLock();
        try {
            int index = children.indexOf(node);
            if (index == -1) {
                LOG.warning("Tried to remove a child that didn't exist in the conflict folder: " + child);
                return;
            }

            List<ISPNode> oldCopy = getChildren();
            node.detachFrom(this);
            children.remove(index);
            List<ISPNode> newCopy = new ArrayList<>(children);
            firePropertyChange(CONFLICT_CHILDREN_PROP, oldCopy, newCopy);
            fireStructureChange(CONFLICT_CHILDREN_PROP, this, oldCopy, newCopy);
        } finally {
            returnProgramWriteLock();
        }
    }

    @Override public void accept(ISPProgramVisitor visitor) {
        visitor.visitConflictFolder(this);
    }

    @Override public MemProgram getProgram() { return program; }
}
