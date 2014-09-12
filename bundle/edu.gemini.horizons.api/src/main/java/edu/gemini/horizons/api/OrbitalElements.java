package edu.gemini.horizons.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

//$Id: OrbitalElements.java 630 2006-11-28 19:32:20Z anunez $
/**
 * A representation of the orbital elements that defines the orbit uniquely.
 * The representation of these elements was chosen based on the one used
 * in the JPL Horizons server.
 * <p/>
 * A description of every element is found in the
 * {@link edu.gemini.horizons.api.OrbitalElements.Name} Enumerated type.
 *
 */
public final class OrbitalElements implements Serializable {

    //private static final Logger LOG = Logger.getLogger(OrbitalElements.class.getName());

    /**
     * List of the orbital elements supported by the system. If a new
     * element wants to be added, it's up to augment the list of enumerated
     * types supported here. They match the Horizons' nomenclature
     * for orbital elements. Elements are added to the <code>OrbitalElement</code>
     * object using either the
     * {@link edu.gemini.horizons.api.OrbitalElements#addElement(edu.gemini.horizons.api.OrbitalElements.Name, Double)}
     * or
     * {@link edu.gemini.horizons.api.OrbitalElements#addElement(String, Double)}.
     * The later will transform the string name into a <code>Name</name> if that's possible.
     */
    public static enum Name {
        /**
         * Orbital Element Epoch
         */
        EPOCH,
        /**
         * Eccentricity
         */
        EC,
        /**
         * Perihelion Distance
         */
        QR,
        /**
         * Time of Perihelion Pssage
         */
        TP,
        /**
         * Logitude of Ascening Node
         */
        OM,
        /**
         * Argument of Perihelion
         */
        W,
        /**
         * Inclination
         */
        IN,
        /**
         * Semi-major Axis
         */
        A,
        /**
         * Mean Anomaly
         */
        MA
    }

    /**
     * Internal storage of the orbital elements. Maps
     * {@link edu.gemini.horizons.api.OrbitalElements.Name} enumerated type
     * to double values
     */
    private final HashMap<Name, Double> _elements;

    /**
     * Default constructor
     */
    public OrbitalElements() {
        _elements = new HashMap<Name, Double>();
    }

    /**
     * Set a double value for the specified {@link edu.gemini.horizons.api.OrbitalElements.Name}
     * type, that represent one of the supported orbital elements
     * @param name {@link edu.gemini.horizons.api.OrbitalElements.Name} enumerated type whose
     * value is going to be specified.
     * @param d Double value to be assigned to the specified <code>name</code> orbital element
     */
    public void addElement(Name name, Double d) {
        _elements.put(name, d);
    }

    /**
     * Attempts to add an specific element to the list of orbital elements.
     * In order to succeed, the argument <code>name</code> must match one of
     * the <code>Name.values()</code> elements.
     * @param name String representation of the element <code>Name</code> to add.
     * @param d Double value to be associated with the specific element
     * @see edu.gemini.horizons.api.OrbitalElements.Name
     */
    public void addElement(String name, Double d) {
        if (name == null || "".equals(name.trim())) return;
        try {
            Name n = Name.valueOf(name);
            addElement(n, d);
        } catch(IllegalArgumentException e) {
            //invalid name argument. We don't complain too much, since this
            //is likely to happen, cause we only support a subset of all
            //the elements an Horizons query returns
            //LOG.log(Level.INFO, "Unknown orbital name : " + name);
        }
    }

    /**
     * Gets a set of keys currently available in this orbital element collection.
     * Could return an empty set if no orbital elements have been assigned yet to
     * this object.
     * @return  a {@link java.util.Set} of {@link edu.gemini.horizons.api.OrbitalElements.Name}
     * elements.
     */
    public Set<Name> getKeys() {
        return _elements.keySet();
    }

    /**
     * Returns the double value for the orbital elements specified by the
     * {@link edu.gemini.horizons.api.OrbitalElements.Name} argument. If no value is
     * associated with the argument, a <code>null</code> vale is returned
     * @param name the {@link edu.gemini.horizons.api.OrbitalElements.Name} object whose
     * associated {@link Double} value is to be returned
     * @return the value to which the requested orbital element maps to, or null if
     * there is no value for the specified orbital element
     */
    public Double getValue(Name name) {
        return _elements.get(name);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        Set<Name> keys = getKeys();
        for (Name name: keys) {
            Double d = getValue(name);
            buf.append(name).append(": ").append(d).append("\n");
        }
        return buf.toString();
    }


    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof OrbitalElements)) return false;
        OrbitalElements that = (OrbitalElements) obj;
        return this._elements.equals(that._elements);
    }

    public int hashCode() {
        return _elements.hashCode();
    }
}
