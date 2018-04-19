package edu.gemini.pot.sp;

import static edu.gemini.pot.sp.SPComponentType.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.Site;
import static edu.gemini.spModel.core.Site.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An enumeration of all instruments, which is a subset of all component types.
 */
public enum Instrument {
    AcquisitionCamera(INSTRUMENT_ACQCAM, GN, GS),
    Bhros(INSTRUMENT_BHROS),
    Flamingos2(INSTRUMENT_FLAMINGOS2, GS),
    Ghost(INSTRUMENT_GHOST, GS),
    GmosNorth(INSTRUMENT_GMOS, GN),
    GmosSouth(INSTRUMENT_GMOSSOUTH, GS),
    Gnirs(INSTRUMENT_GNIRS, GS),
    Gpi(INSTRUMENT_GPI, GS),
    Gsaoi(INSTRUMENT_GSAOI, GS),
    Michelle(INSTRUMENT_MICHELLE, GN),
    Nici(INSTRUMENT_NICI, GS),
    Nifs(INSTRUMENT_NIFS, GN),
    Niri(INSTRUMENT_NIRI, GN),
    Phoenix(INSTRUMENT_PHOENIX, GS),
    Texes(INSTRUMENT_TEXES),
    Trecs(INSTRUMENT_TRECS),
    Visitor(INSTRUMENT_VISITOR, GN, GS)
    ;

    public final SPComponentType componentType;
    public final Set<Site> sites;

    Instrument(SPComponentType type, Site... sites) {
        this.componentType = type;
        this.sites         = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(sites)));
    }

    public boolean existsAt(Site site) {
        return sites.contains(site);
    }

    /**
     * Wraps the instrument in an Option, to make it easy to pass to the
     * ISPFactory.createObservation methods.
     */
    public Option<Instrument> some() {
        return new Some<>(this);
    }

    /**
     * Convenience method for obtaining a none instance.
     */
    public static final Option<Instrument> none = None.instance();

    /**
     * Looks up the Instrument associated with an SPComponentType, if any.
     */
    public static Option<Instrument> fromComponentType(SPComponentType type) {
        return ImOption.fromOptional(
                   Arrays.stream(values())
                      .filter(i -> i.componentType == type)
                      .findFirst()
               );
    }
}
