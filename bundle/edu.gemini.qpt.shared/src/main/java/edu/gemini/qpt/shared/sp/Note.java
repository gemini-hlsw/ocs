package edu.gemini.qpt.shared.sp;

import edu.gemini.shared.util.CompressedString;

import java.io.Serializable;

/**
 * Mini-model representation for all spModel note types.
 * <p>
 * @author rnorris
 */
public final class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Scope {
        Program, Group, Observation, Sequence
    }
    
    private final Scope scope;
    private final String title;
    private final CompressedString text;
    
    public Note(Scope scope, String title, String text) {
        this.scope = scope;
        this.title = title;
        this.text = new CompressedString(text);
    }

    public Scope getScope() {
        return scope;
    }

    public String getText() {
        return text.get();
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
    
}
