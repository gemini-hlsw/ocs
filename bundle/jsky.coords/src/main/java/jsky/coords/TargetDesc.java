/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TargetDesc.java 42349 2012-03-01 13:03:51Z swalker $
 */

package jsky.coords;


import edu.gemini.shared.util.immutable.Option;

import java.util.function.Function;

/**
 * A simple class describing a target object and some related information.
 *
 * @version $Revision: 42349 $
 * @author Allan Brighton
 */
public abstract class TargetDesc {
    public enum ElConstraintType {
    	NONE, HOUR_ANGLE, AIRMASS
    }

    private String _name;
    private Function<Option<Long>, Option<WorldCoords>> _coords;
    private String _priority;
    private String _category;
    private ElConstraintType _elType;
    private double _elMin;
    private double _elMax;

    protected TargetDesc(String name,
                         Function<Option<Long>, Option<WorldCoords>> coords,
                         String priority,
                         String category,
                         ElConstraintType elType,
                         Double elMin,
                         Double elMax) {
        _name     = name;
        _coords   = coords;
        _priority = priority;
        _category = category;
        _elType   = elType;
        _elMin    = elMin;
        _elMax    = elMax;
    }

    public String getName() {
        return _name;
    }

    public String toString() {
        return _name;
    }

    public Option<WorldCoords> getCoordinates(Option<Long> when) {
        return _coords.apply(when);
    }

    public String getPriority() {
        return _priority;
    }

    public String getCategory() {
        return _category;
    }

    /** Return an array of one or more Strings describing the target */
    public String[] getDescriptionFields() {
        return new String[]{_name};
    }

    public double getElMax() {
        return _elMax;
    }

    public double getElMin() {
        return _elMin;
    }

    public ElConstraintType getElType() {
        return _elType;
    }
}

