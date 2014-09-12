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
public class Problem implements Serializable, Comparable<Problem> {

    public static enum Type {

        NONE("None"),
        WARNING("Warning"),
        ERROR("Error");

        private String _displayValue;

        private Type(String displayValue) {
            _displayValue = displayValue;
        }

        public String getDisplayValue() {
            return _displayValue;
        }
    }

    private Type _type;

    private String _id; // A unique id for this problem

    private String _description;

    private ISPProgramNode _node; //The node that produced the problem

//    public Problem(Type type, String description, ISPProgramNode n) {
//        // XXX set ID!
//        _type = type;
//        _description = description;
//        _node = n;
//    }
    
    public Problem(Type type, String id, String description, ISPProgramNode n) {
        _type = type;
        _id = id;
        _description = description;
        _node = n;
    }

    public Type getType() {
        return _type;
    }

    public String getDescription() {
        return _description;
    }

    public String getId() {
        return _id;
    }

    public String toString() {
        return _type.getDisplayValue() + ":" + _description + " at " + _node;
    }

    public ISPProgramNode getAffectedNode() {
        return _node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Problem)) return false;

        Problem problem = (Problem) o;

        if (_description != null ? !_description.equals(problem._description) : problem._description != null)
            return false;
        if (_id != null ? !_id.equals(problem._id) : problem._id != null) return false;
        if (_node != null ? !_node.equals(problem._node) : problem._node != null) return false;
        if (_type != problem._type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _description != null ? _description.hashCode() : 0;
        result = 31 * result + (_type != null ? _type.hashCode() : 0);
        result = 31 * result + (_node != null ? _node.hashCode() : 0);
        result = 31 * result + (_id != null ? _id.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Problem o) {
        int result = _id.compareTo(o._id);
        if (result == 0) {
            result = _description.compareTo(o._description);
            if (result == 0) {
                result = _type.compareTo(o._type);
                if (result == 0) {
                    result = _node.toString().compareTo(o._node.toString());
                }
            }
        }
        return result;
    }
}
