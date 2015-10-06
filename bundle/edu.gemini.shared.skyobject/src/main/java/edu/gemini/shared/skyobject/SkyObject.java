package edu.gemini.shared.skyobject;

import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.skyobject.coords.SkyCoordinates;
import edu.gemini.shared.util.immutable.*;

import java.io.Serializable;
import java.util.*;

/**
 * An immutable object that represents a celestial body (for example, a guide
 * star or science target).  Each SkyObject has an arbitrary name, a set of
 * coordinates to identify its position, a collection of apparent magnitude
 * information at various bandpasses, and a set of arbitrary attributes.
 *
 * <p>It is anticipated that the attributes would be filled in by tools and
 * used for informational purposes.  For example, the various fields in
 * guide star catalog output that are not otherwise used in the OCS will be
 * recorded in attributes for display.
 *
 * <p>SkyObjects are constructed via an associated {@link Builder}.  The
 * Builder is created and initialized with the appropriate values before
 * calling its {@link Builder#build} method to create the SkyObject.
 */
public final class SkyObject implements Serializable {

    /**
     * A mutable constructor of SkyObjects. Builder calls are meant to be
     * chained together to simplify client code.  For example,
     *
     * <code>
     * SkyObject so = (new Builder(name, coords)).magnitudes(mags).build();
     * </code>
     *
     * Here the <code>magnitudes</code> method updates the magnitude values
     * and returns a reference to <code>this</code> Builder.
     */
    public static final class Builder {
        private String name;
        private SkyCoordinates coords;

        private ImList<Magnitude> magList;
        private Map<Object, Object> attrMap;           // kept immutable

        /**
         * Constructs the Builder with the required name and coordinates
         * parameters.
         *
         * @param name an identifier for the celestial body
         * @param coords a description of its location in the sky
         */
        public Builder(String name, SkyCoordinates coords) {
            if (name == null) throw new IllegalArgumentException("name is null");
            if (coords == null) throw new IllegalArgumentException("coords are null");

            this.name    = name;
            this.coords  = coords;
            this.magList = DefaultImList.create();
            this.attrMap = Collections.emptyMap();
        }

        /**
         * Sets the name of the SkyObject that will be created.
         *
         * @param name new name of the SkyObject
         *
         * @return <code>this</code> Builder
         */
        public Builder name(String name) {
            if (name == null) throw new IllegalArgumentException("name is null");
            this.name = name;
            return this;
        }

        /**
         * Sets the coordinates of the SkyObject that will be created.
         *
         * @param coords new position of the SkyObject
         *
         * @return <code>this</code> Builder
         */
        public Builder coordinates(SkyCoordinates coords) {
            if (coords == null) throw new IllegalArgumentException("coords are null");
            this.coords = coords;
            return this;
        }

        /**
         * Sets the attributes of the SkyObject that will be created.
         *
         * @param attrs new attributes
         *
         * @return <code>this</code> Builder
         */
        public Builder attributes(final Map<Object, Object> attrs) {
            // Make an immutable copy of the argument and store it.
            TreeMap<Object, Object> copy = new TreeMap<>(attrs);
            return internalAttributes(Collections.unmodifiableMap(copy));
        }

        /**
         * Directly sets the internal structure used to store the Builder's
         * attributes.  This method is used internally to avoid making an
         * unnecessary copy of the attribute map whenever possible.
         *
         * @param attrs new attribute map
         *
         * @return <code>this</code> Builder
         */
        private Builder internalAttributes(Map<Object, Object> attrs) {
            attrMap = attrs;
            return this;
        }

        /**
         * Sets the collection of magnitude values associated with this
         * SkyObject
         *
         * @param mags collection of magnitude values associated with this
         * object
         *
         * @return <code>this</code> Builder
         */
        public Builder magnitudes(Magnitude... mags) {
            this.magList = DefaultImList.create(mags);
            return this;
        }

        /**
         * Sets the collection of magnitude values associated with this
         * SkyObject.
         *
         * @param magList collection of magnitude values associated with
         * this object
         *
         * @return <code>this</code> Builder
         */
        public Builder magnitudes(ImList<Magnitude> magList) {
            if (magList == null) throw new IllegalArgumentException();
            this.magList = magList;
            return this;
        }


        /**
         * Creates the SkyObject that has been configured in this builder.
         *
         * @return new SkyObject with the current values set in this builder
         */
        public SkyObject build() {
            return new SkyObject(this);
        }
    }

    private final String name;
    private final SkyCoordinates coords;
    private final Map<Object, Object> attrMap;
    private final ImList<Magnitude> magList;

    private SkyObject(Builder b) {
        this.name    = b.name;
        this.coords  = b.coords;
        this.attrMap = b.attrMap;
        this.magList = b.magList;
    }

    @Override public String toString() {
        return "SkyObject(name=" + name + ", coords=" + coords + ", magList=" + magList.mkString("(", ",", ")" + ")");
    }

    /**
     * Gets the name associated with this SkyObject.
     *
     * @return name of the SkyObject
     */
    public String getName() {
        return name;
    }

    /**
     * Creates a new SkyObject, identical to this one, but with the given
     * new name.
     *
     * @param name name to apply to the new SkyObject returned by this method
     *
     * @return new SkyObject, identical to <code>this</code> SkyObject, but
     * with the given name
     */
    public SkyObject withName(String name) {
        return builder().name(name).build();
    }

    /**
     * Gets the coordinates associated with this SkyObject.  The coordinates
     * describe where the SkyObject may be found in the sky.
     *
     * @return SkyCoordinates describing the position of the SkyObject
     */
    public SkyCoordinates getCoordinates() {
        return coords;
    }

    /**
     * Creates a new SkyObject, identical to this one, but with the given
     * new coordinates.
     *
     * @param coords new coordinates for the SkyObject to be created and
     * returned
     *
     * @return new SkyObject, identical to <code>this</code> SkyObject, but
     * with the given coordinates
     */
    public SkyObject withCoordinates(SkyCoordinates coords) {
        return builder().coordinates(coords).build();
    }

    /**
     * Gets the coordinates converted to {@link HmsDegCoordinates} for the
     * current time. This call is equivalent to
     * {@link #getHmsDegCoordinates(long) getHmsDegCoordinates(System.currentTimeMillis(}).
     *
     * @return coordinates of the SkyObject expressed in
     * {@link HmsDegCoordinates} at the current time
     */
    public HmsDegCoordinates getHmsDegCoordinates() {
        return coords.toHmsDeg(System.currentTimeMillis());
    }

    /**
     * Gets the coordinates of this SkyObject expressed as
     * {@link HmsDegCoordinates} for the indicated time.  Non-sideral coordinate
     * systems, for example, do not have a fixed RA and declination but rather
     * a valid position at a particular time.
     *
     * @param time the time at which the current position is sought
     *
     * @return coordinates of the SkyObject expressed in
     * {@link HmsDegCoordinates}, valid at the given <code>time</code>
     */
    public HmsDegCoordinates getHmsDegCoordinates(long time) {
        return coords.toHmsDeg(time);
    }

    /**
     * Gets the attribute associated with the given key, if any.
     *
     * @param key attribute key whose value is sought
     *
     * @return {@link None} if there is no attribute associated with the given
     * key; {@link Some}<Object> if there is one
     */
    public Option<Object> getAttribute(final Object key) {
        Object res = attrMap.get(key);
        return (res == null) ? None.instance() : new Some<>(res);
    }

    /**
     * Gets all the attributes associated with this SkyObject.
     *
     * @return non-null (though possibly empty) collection of all attributes
     * associated with this SkyObject
     */
    public Map<Object, Object> getAttributes() {
        return attrMap;
    }

    /**
     * Creates a new SkyObject, identical to this one, but with the addition
     * of or update to the indicated attribute.
     *
     * @param key key of the attribute to add or update
     * @param value new value of the attribute
     *
     * @return new SkyObject, identical to <code>this</code> SkyObject, but
     * with the given attribute
     */
    public SkyObject addAttribute(final Object key, final Object value) {
        final Map<Object, Object> copy = new TreeMap<>(attrMap);
        copy.put(key,value);
        return builder().attributes(copy).build();
    }

    /**
     * Creates a new SkyObject, identical to this one, but with the provided
     * collection of attributes.
     *
     * @param attrs non-null collection of attributes to associate with the
     * new SkyObject
     *
     * @return new SkyObject, identical to <code>this</code> SkyObject, but
     * with the given attributes
     */
    public SkyObject withAttributes(Map<Object, Object> attrs) {
        return builder().attributes(attrs).build();
    }

    /**
     * Gets all the magnitude values associated with this SkyObject, if any.
     *
     * @return non-null (though possibly empty) collection of all magnitude
     * values associated with this SkyObject
     */
    public ImList<Magnitude> getMagnitudes() {
        return magList;
    }

    /**
     * Gets the magnitude value for the indicated band, if any.
     *
     * @param band bandpass of interest whose associated magnitude should
     * be returned
     *
     * @return {@link None} if there is no {@link Magnitude} associated with
     * this wavelength band; {@link Some}<Magnitude> otherwise
     */
    public Option<Magnitude> getMagnitude(final Magnitude.Band band) {
        return magList.find(magnitude -> band.equals(magnitude.getBand()));
    }

    // A map operation from Magnitude to Magnitude.Band.
    private static final MapOp<Magnitude, Magnitude.Band> BAND_MAP_OP =
            Magnitude::getBand;

    /**
     * Gets all the magnitudes associated with this SkyObject, if any.
     *
     * @return non-null (though possibly empty) set of bands for which there
     * is an associated {@link Magnitude}
     */
    public Set<Magnitude.Band> getMagnitudeBands() {
        return new HashSet<>(magList.map(BAND_MAP_OP).toList());
    }

    /**
     * Creates a new SkyObject, identical to this one, but with the addition of,
     * or update to, the provided {@link Magnitude}.
     *
     * @param mag new or updated magnitude object
     *
     * @return new SkyObject, identical to <code>this</code> SkyObject, but
     * with the addition of, or update to, the provide {@link Magnitude}
     */
    public SkyObject addMagnitude(final Magnitude mag) {
        return withMagnitudes(magList.filter(magnitude -> mag.getBand() != magnitude.getBand()).cons(mag));
    }

    /**
     * Creates a new SkyObject, identical to this one, but with the provided
     * collection of {@link Magnitude} objects instead of the existing ones.
     * If multiple {@link Magnitude} objects with the same band are in the
     * collection, only the last one returned by the collection's iterator
     * will be kept.
     *
     * @param magnitudes new non-null collection of {@link Magnitude}
     *
     * @return new SkyObject, identical to <code>this</code> SkyObject, but
     * with the provided collection of {@link Magnitude}
     */
    public SkyObject withMagnitudes(ImList<Magnitude> magnitudes) {
        return builder().magnitudes(magnitudes).build();
    }

    /**
     * Creates a new {@link Builder} configured with the values of this
     * {@link SkyObject}.
     *
     * @return a new mutable {@link Builder} initialized with the values from
     * this SkyObject
     */
    public Builder builder() {
        return new Builder(name, coords).
                     internalAttributes(attrMap).
                     magnitudes(magList);
    }
}
