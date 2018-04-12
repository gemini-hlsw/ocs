package edu.gemini.pot.sp;

import static edu.gemini.pot.sp.SPComponentType.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Some;

import java.util.Arrays;

/**
 * An enumeration of all instruments, which is a subset of all component types.
 */
public enum Instrument {
    AcquisitionCamera(INSTRUMENT_ACQCAM),
    Bhros(INSTRUMENT_BHROS),
    Flamingos2(INSTRUMENT_FLAMINGOS2),
    Ghost(INSTRUMENT_GHOST),
    GmosNorth(INSTRUMENT_GMOS),
    GmosSouth(INSTRUMENT_GMOSSOUTH),
    Gnirs(INSTRUMENT_GNIRS),
    Gpi(INSTRUMENT_GPI),
    Gsaoi(INSTRUMENT_GSAOI),
    Michelle(INSTRUMENT_MICHELLE),
    Nici(INSTRUMENT_NICI),
    Nifs(INSTRUMENT_NIFS),
    Niri(INSTRUMENT_NIRI),
    Phoenix(INSTRUMENT_PHOENIX),
    Texes(INSTRUMENT_TEXES),
    Trecs(INSTRUMENT_TRECS),
    Visitor(INSTRUMENT_VISITOR)
    ;

    public final SPComponentType componentType;

    Instrument(SPComponentType type) {
        this.componentType = type;
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
