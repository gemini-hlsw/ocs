/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NamedCoordinates.java 23553 2010-01-22 19:24:12Z swalker $
 */

package jsky.catalog.gui;

import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import jsky.coords.Coordinates;


/**
 * Simple class containing an object name, its coordinates, and the brightness,
 * if known. The brightness is an optional string (for display) describing the
 * object's brightness, for example: "mag: 13.2", or "12.3B, 12.1V".
 * The brightness may also be null or empty, if not known.
 *
 * @version $Revision: 23553 $
 * @author Allan Brighton
 */
public final class NamedCoordinates {

    private final String name;
    private final Coordinates coords;
    private final Option<String> brightness;
    private final Option<SkyObject> skyObject;

    public NamedCoordinates(String name, Coordinates coords) {
        //noinspection unchecked
        this(name, coords, None.INSTANCE, None.INSTANCE);
    }

    public NamedCoordinates(String name, Coordinates coords, String brightness) {
        //noinspection unchecked
        this(name, coords, (brightness == null) ? None.INSTANCE : new Some<String>(brightness), None.INSTANCE);
    }

    public NamedCoordinates(String name, Coordinates coords, SkyObject skyObject) {
        //noinspection unchecked
        this(name, coords, None.INSTANCE, (skyObject == null) ? None.INSTANCE : new Some<SkyObject>(skyObject));
    }

    public NamedCoordinates(String name, Coordinates coords, Option<String> brightness, Option<SkyObject> skyObj) {
        if ((name == null) || (coords == null) || (brightness == null) || (skyObj == null)) {
            throw new IllegalArgumentException();
        }
        this.name       = name;
        this.coords     = coords;
        this.brightness = brightness;
        this.skyObject  = skyObj;
    }

    public String getName() { return name; }
    public String toString() { return name; }
    public Coordinates getCoordinates() { return coords; }
    public Option<String> getBrightness() { return brightness; }
    public Option<SkyObject> getSkyObject() { return skyObject; }
}

