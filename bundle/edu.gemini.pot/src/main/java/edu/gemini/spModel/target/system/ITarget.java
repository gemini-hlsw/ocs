package edu.gemini.spModel.target.system;

import java.io.Serializable;

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
       SIDEREAL("J2000", "Sidereal Target"),
       NAMED("Solar system object"),
       JPL_MINOR_BODY("JPL minor body", "JPL minor body (Comet)"),
       MPC_MINOR_PLANET("MPC minor planet", "MPC minor planet (Asteroid)");

       public final String tccName;
       public final String friendlyName;

       Tag(String tccName, String friendlyName) {
           this.tccName = tccName;
           this.friendlyName = friendlyName;
       }

       Tag(String tccName) {
           this(tccName, tccName);
       }

       @Override
       public String toString() {
           return friendlyName;
       }

    }
}
