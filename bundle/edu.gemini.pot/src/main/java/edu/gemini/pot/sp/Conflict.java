package edu.gemini.pot.sp;

import java.io.Serializable;

/**
 * Conflict types and associated notes.
 */
public final class Conflict {
    private Conflict() {}

    /**
     * Note added to {@link Conflicts} to warn the user of a conflict.
     */
    public static abstract class Note implements Serializable {
        public final SPNodeKey nodeKey;

        Note(SPNodeKey nodeKey) {
            if (nodeKey == null) throw new IllegalArgumentException("nodeKey == null");
            this.nodeKey      = nodeKey;
        }

        public SPNodeKey getNodeKey() { return nodeKey; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Note that = (Note) o;
            return nodeKey.equals(that.nodeKey);

        }

        @Override public int hashCode() {
            return nodeKey.hashCode();
        }

        public abstract void accept(NoteVisitor v);

        @Override public String toString() {
            return getClass().getSimpleName() + ": " + getNodeKey();
        }
    }

    /**
     * The Visitor pattern is used to guarantee that when we add a new note
     * type we update all the usages that distinguish between the various
     * types.
     */
    public interface NoteVisitor {
        void visitMoved(Moved note);
        void visitResurrectedLocalDelete(ResurrectedLocalDelete note);
        void visitReplacedRemoteDelete(ReplacedRemoteDelete note);
        void visitCreatePermissionFail(CreatePermissionFail note);
        void visitUpdatePermissionFail(UpdatePermissionFail note);
        void visitDeletePermissionFail(DeletePermissionFail note);
        void visitConstraintViolation(ConstraintViolation note);
        void visitConflictFolder(ConflictFolder note);
    }

    /**
     * Moved to distinct locations in incoming and existing program.  The
     * location in the incoming update is respected and a Moved conflict
     * note is added to the parent where the node was in the existing program.
     */
    public static final class Moved extends Note {
        public final SPNodeKey to;

        public Moved(SPNodeKey node, SPNodeKey to) {
            super(node);

            if (to == null) throw new IllegalArgumentException("to == null");
            this.to = to;
        }

        public SPNodeKey getDestinationKey() { return to; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            Moved that = (Moved) o;
            return to.equals(that.to);
        }

        @Override public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + to.hashCode();
            return result;
        }

        public void accept(NoteVisitor v) { v.visitMoved(this); }

        @Override public final String toString() {
            return super.toString() + " to " + getDestinationKey();
        }
    }

    /**
     * Node was deleted locally but had been modified in the remote database.
     * In this case, we "resurrect" the node in the local existing program using
     * the version coming from the database as the guide and add this conflict
     * note.
     */
    public static final class ResurrectedLocalDelete extends Note {
        public ResurrectedLocalDelete(SPNodeKey node) { super(node); }

        public void accept(NoteVisitor v) { v.visitResurrectedLocalDelete(this);}
    }

    /**
     * Node was deleted remotely but had been modified in the local database.
     * In this case, we make a copy of the node in the local existing program
     * with new keys add this conflict note. Once a deletion is checked in to a
     * database, the node cannot be brought back to life but a new copy based
     * on the modified local version can be kept.
     */
    public static final class ReplacedRemoteDelete extends Note {
        public ReplacedRemoteDelete(SPNodeKey node) {
            super(node);
        }

        public void accept(NoteVisitor v) { v.visitReplacedRemoteDelete(this);}
    }

    public static final class CreatePermissionFail extends Note {
        public CreatePermissionFail(SPNodeKey node) {
            super(node);
        }

        public void accept(NoteVisitor v) { v.visitCreatePermissionFail(this); }
    }

    public static final class UpdatePermissionFail extends Note {
        public UpdatePermissionFail(SPNodeKey node) {
            super(node);
        }

        public void accept(NoteVisitor v) { v.visitUpdatePermissionFail(this); }
    }

    public static final class DeletePermissionFail extends Note {
        public DeletePermissionFail(SPNodeKey node) {
            super(node);
        }

        public void accept(NoteVisitor v) { v.visitDeletePermissionFail(this); }
    }

    public static final class ConstraintViolation extends Note {
        public ConstraintViolation(SPNodeKey node) {
            super(node);
        }

        public void accept(NoteVisitor v) { v.visitConstraintViolation(this); }
    }

    public static final class ConflictFolder extends Note {
        public ConflictFolder(SPNodeKey node) {
            super(node);
        }

        public void accept(NoteVisitor v) { v.visitConflictFolder(this); }
    }
}
