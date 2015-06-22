package edu.gemini.pot.sp;

import edu.gemini.shared.util.immutable.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Pairs an optional {@link DataObjectConflict} with a list of
 * {@link Conflict.Note}.  Together these describe the collection of conflicts
 * that a given program node might suffer after an update.
 */
public final class Conflicts implements Serializable {
    public static final Conflicts EMPTY =
            new Conflicts(None.<DataObjectConflict>instance(), ImCollections.<Conflict.Note>emptyList());

    public static Conflicts apply(Option<DataObjectConflict> dataObjectConflict, ImList<Conflict.Note> notes) {
        return (dataObjectConflict.isEmpty() && notes.isEmpty()) ?
                EMPTY : new Conflicts(dataObjectConflict, notes);
    }

    public static Conflicts apply(Option<DataObjectConflict> dataObjectConflict) {
        return apply(dataObjectConflict, ImCollections.<Conflict.Note>emptyList());
    }

    public static Conflicts apply(ImList<Conflict.Note> notes) {
        return apply(None.<DataObjectConflict>instance(), notes);
    }

    public final Option<DataObjectConflict> dataObjectConflict;
    public final ImList<Conflict.Note> notes;

    private Conflicts(Option<DataObjectConflict> dataObjectConflict, ImList<Conflict.Note> notes) {
        this.dataObjectConflict = dataObjectConflict;
        this.notes              = notes;
    }

    public Conflicts withDataObjectConflict(DataObjectConflict doc) {
        return apply(new Some<>(doc), notes);
    }

    public Conflicts withConflictNote(final Conflict.Note note) {
        return notes.contains(note) ? this : apply(dataObjectConflict, notes.append(note));
    }

    public Conflicts resolveDataObjectConflict() {
        return apply(notes);
    }

    public Conflicts resolveConflictNote(final Conflict.Note note) {
        final ImList<Conflict.Note> lst = notes.filter(cn -> !cn.equals(note));
        return (lst.size() == notes.size()) ? this : apply(dataObjectConflict, lst);
    }

    public boolean isEmpty() {
        return dataObjectConflict.isEmpty() && (notes.size() == 0);
    }

    public boolean nonEmpty() {
        return !isEmpty();
    }

    public Conflicts merge(Conflicts that) {
        if (that.isEmpty()) return this;
        if (this.isEmpty()) return that;

        final Option<DataObjectConflict> mergedDoc = that.dataObjectConflict.orElse(dataObjectConflict);

        final Set<SPNodeKey> newKeys = new HashSet<>();
        that.notes.foreach(cn -> newKeys.add(cn.getNodeKey()));

        final ImList<Conflict.Note> oldNotes = notes.filter(cn -> !newKeys.contains(cn.getNodeKey()));

        final ImList<Conflict.Note> mergedNotes = oldNotes.append(that.notes);

        return Conflicts.apply(mergedDoc, mergedNotes);
    }

    @Override public String toString() {
        final String ds = dataObjectConflict.map(DataObjectConflict::toString).getOrElse("No data object conflict");
        final String ns = notes.mkString("Notes: {", ",", "}");
        return "Conflicts: " + ds + ", " + ns;
    }
}
