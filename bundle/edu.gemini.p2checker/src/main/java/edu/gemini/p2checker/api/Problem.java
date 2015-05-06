//
// $Id$
//

package edu.gemini.p2checker.api;

import edu.gemini.pot.sp.ISPProgramNode;

import java.io.Serializable;

/**
 * A Problem contains the description of a condition that needs to
 * be fixed in a given <code>ISPProgramNode</code>
 */
public class Problem implements Serializable {

    public enum Type {
        NONE("None"),
        WARNING("Warning"),
        ERROR("Error");

        public final String displayValue;

        Type(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }
    }

    public final Type type;

    public final String id; // A unique id for this problem

    public final String description;

    private final ISPProgramNode node; //The node that produced the problem

    public Problem(Type type, String id, String description, ISPProgramNode n) {
        this.type        = type;
        this.id          = id;
        this.description = description;
        this.node        = n;
    }

    public final Type getType() {
        return type;
    }

    public final String getDescription() {
        return description;
    }

    public final String getId() {
        return id;
    }

    public final String toString() {
        return type.getDisplayValue() + ":" + description + " at " + node;
    }

    public final ISPProgramNode getAffectedNode() {
        return node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Problem that = (Problem) o;

        if (type != that.type) return false;
        if (!id.equals(that.id)) return false;
        if (!description.equals(that.description)) return false;
        return node.equals(that.node);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + node.hashCode();
        return result;
    }
}
