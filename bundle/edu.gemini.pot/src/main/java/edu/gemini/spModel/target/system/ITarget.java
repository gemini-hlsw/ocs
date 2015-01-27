// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ITarget.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.PredicateOp;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.spModel.target.system.CoordinateTypes.Epoch;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * This class describes methods that must be implemented by
 * all coordinate systems.  A coordinate system consists of a
 * set of parameters that describe the position of a celestial object.
 *
 * @author      Kim Gillies
 */
public abstract class ITarget implements Cloneable, Serializable {

   public enum Tag {

       // N.B. these strings are meaningful to the TCC, catalog, and are used in PIO XML
       SIDEREAL("J2000"),
       NAMED("Solar system object"),
       JPL_MINOR_BODY("JPL minor body"),
       MPC_MINOR_PLANET("MPC minor planet");

       public final String tccName;

       private Tag(String tccName) {
           this.tccName = tccName;
       }

       @Override
       public String toString() {
           return tccName;
       }

    }

    public static ITarget forTag(Tag tag) {
        switch (tag) {
            case JPL_MINOR_BODY:   return new ConicTarget(ITarget.Tag.JPL_MINOR_BODY);
            case MPC_MINOR_PLANET: return new ConicTarget(ITarget.Tag.MPC_MINOR_PLANET);
            case NAMED:            return new NamedTarget();
            case SIDEREAL:         return new HmsDegTarget();
        }
        throw new Error("unpossible");
    }


    /**
     * Returns an optional name for the target.
     */
    public abstract String getName();

    /**
     * Sets an optional name for the target.
     */
    public abstract void setName(String name);

    /**
     * Gets a short description of the position.  For instance, the
     * {@link HmsDegTarget} might return its RA and Dec in a
     * formated String such as "RA=12:34:56 Dec=00:11:22".  To actually
     * use a coordinate system's position, the client will have to use its
     * particular interface rather than this method.
     */
    public abstract String getPosition();

    /** Set the RA. */
    public abstract void setRa(ICoordinate ra);

    /** Get the RA. */
    public abstract ICoordinate getRa();

    /** Set the Dec. */
    public abstract void setDec(ICoordinate dec);

    /** Get the Dec. */
    public abstract ICoordinate getDec();

    /** Set the RA in HMS format. */
    public abstract void setRaHMS(String hms);

    /** Get the RA in HMS format. */
    public abstract String getRaHMS();

    /** Set the Dec in DMS format. */
    public abstract void setDecDMS(String dec);

    /** Get the Dec in DMS format. */
    public abstract String getDecDMS();

    /**
     * Return the Epoch of this target position.
     * @throws IllegalArgumentException if the coordinate system does not
     * support the Epoch concept.
     */
    public abstract Epoch getEpoch();

    /**
     * Set the Epoch of this target position.
     * @throws IllegalArgumentException if the coordinate system does not
     * support the Epoch concept.
     */
    public abstract void setEpoch(Epoch e);

    /**
     * Provides testing of equality of two targets.
     */
    public abstract boolean equals(Object obj);


    // RCN: pushed across from SPTarget

    private ImList<Magnitude> magnitudes = ImCollections.emptyList();

    /**
     * Gets all the {@link Magnitude} information associated with this target,
     * if any.
     *
     * @return (possibly empty) immutable list of {@link Magnitude} values
     * associated with this target
     */
    public ImList<Magnitude> getMagnitudes() {
        return magnitudes;
    }

    /**
     * Filters {@link Magnitude} values with the same passband.
     *
     * @param magList original magnitude list possibly containing values with
     * duplicate passbands
     *
     * @return immutable list of {@link Magnitude} where each value in the
     * list is guaranteed to have a distinct passband
     */
    private static ImList<Magnitude> filterDuplicates(final ImList<Magnitude> magList) {
        return magList.filter(new PredicateOp<Magnitude>() {
            private final Set<Magnitude.Band> bands = new HashSet<>();
            @Override public Boolean apply(final Magnitude magnitude) {
                final Magnitude.Band band = magnitude.getBand();
                if (bands.contains(band)) return false;
                bands.add(band);
                return true;
            }
        });
    }

    /**
     * Assigns the list of magnitudes to associate with this target.  If there
     * are multiple magnitudes associated with the same bandpass, only one will
     * be kept.
     *
     * @param magnitudes new collection of magnitude information to store with
     * the target
     */
    public void setMagnitudes(final ImList<Magnitude> magnitudes) {
        this.magnitudes = filterDuplicates(magnitudes);
    }

    /**
     * Gets the {@link Magnitude} value associated with the given magnitude
     * passband.
     *
     * @param band passband of the {@link Magnitude} value to retrieve
     *
     * @return {@link Magnitude} value associated with the given passband,
     * wrapped in a {@link edu.gemini.shared.util.immutable.Some} object; {@link edu.gemini.shared.util.immutable.None} if none
     */
    public Option<Magnitude> getMagnitude(final Magnitude.Band band) {
        return magnitudes.find(new PredicateOp<Magnitude>() {
            @Override public Boolean apply(final Magnitude magnitude) {
                return band.equals(magnitude.getBand());
            }
        });
    }

    /**
     * Gets the set of magnitude bands that have been recorded in this target.
     *
     * @returns a Set of {@link Magnitude.Band magnitude bands} for which
     * we have information in this target
     */
    public Set<Magnitude.Band> getMagnitudeBands() {
        final ImList<Magnitude.Band> bandList = magnitudes.map(new MapOp<Magnitude, Magnitude.Band>() {
            @Override public Magnitude.Band apply(final Magnitude magnitude) {
                return magnitude.getBand();
            }
        });
        return new HashSet<>(bandList.toList());
    }

    /**
     * Adds the given magnitude to the collection of magnitudes associated with
     * this target, replacing any other magnitude of the same band if any.
     *
     * @param mag magnitude information to add to the collection of magnitudes
     */
    public void putMagnitude(final Magnitude mag) {
        magnitudes = magnitudes.filter(new PredicateOp<Magnitude>() {
            @Override public Boolean apply(final Magnitude cur) {
                return cur.getBand() != mag.getBand();
            }
        }).cons(mag);
    }


    // pushed down from CoordinateSystem

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new Error(cnse);
        }
    }

    public abstract Tag getTag();

    /** Gets a Skycalc {@link edu.gemini.skycalc.Coordinates} representation. */
    public synchronized Coordinates getSkycalcCoordinates() {
        return new Coordinates(getRa().getAs(CoordinateParam.Units.DEGREES), getDec().getAs(CoordinateParam.Units.DEGREES));
    }

}
