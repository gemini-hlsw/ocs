package edu.gemini.spModel.target;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.MagnitudeSystem;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * {@link ParamSet} I/O for {@link Magnitude} values.
 */
public enum MagnitudePio {
    /** Singleton instance. */
    instance;

    private static final Logger LOGGER = Logger.getLogger(MagnitudePio.class.getName());

    /**
     * Name of the {@link ParamSet} containing a list of {@link Magnitude}
     * values.
     */
    public static final String MAG_LIST = "magnitudeList";

    /**
     * Name of the {@link ParamSet} containing a single {@link Magnitude}.
     */
    public static final String MAG      = "magnitude";

    /**
     * Name of the {@link edu.gemini.spModel.pio.Param} that identifies
     * the {@link Magnitude.Band magnitude band}.
     */
    public static final String MAG_BAND = "band";

    /**
     * Name of the {@link edu.gemini.spModel.pio.Param} that identifies
     * the {@link edu.gemini.shared.skyobject.Magnitude#getBrightness() magnitude brightness}.
     */
    public static final String MAG_VAL  = "val";

    /**
     * Name of the {@link edu.gemini.spModel.pio.Param} that identifies
     * the {@link edu.gemini.shared.skyobject.Magnitude#getError() error}
     * in the magnitude value.
     */
    public static final String MAG_ERROR= "error";

    /**
     * Name of the {@link edu.gemini.spModel.pio.Param} that identifies
     * the {@link edu.gemini.shared.skyobject.Magnitude#getSystem() system}
     * in the magnitude value.
     */
    public static final String MAG_SYSTEM= "system";

    /**
     * Creates a ParamSet from the given {@link ImList} of {@link Magnitude}
     * objects that can later be parsed by {@link #toList}.
     *
     * @param factory factory for constructing the ParamSet and parameters
     * @param magList magnitude values to read
     *
     * @return ParamSet describing the given list of magnitude
     */
    public ParamSet toParamSet(final PioFactory factory, ImList<Magnitude> magList) {
        final ParamSet pset = factory.createParamSet(MAG_LIST);

        magList.foreach(new ApplyOp<Magnitude>() {
            @Override
            public void apply(Magnitude mag) {
                pset.addParamSet(toParamSet(factory, mag));
            }
        });

        return pset;
    }


    /**
     * Creates a ParamSet from the given {@link edu.gemini.shared.skyobject.Magnitude} that can later be
     * parsed by {@link #toMagnitude}.
     *
     * @param factory factory for constructing the ParamSet and parameters
     * @param mag magnitude value to read
     *
     * @return ParamSet describing the given magnitude value
     */
    public ParamSet toParamSet(PioFactory factory, Magnitude mag) {
        ParamSet magPset = factory.createParamSet(MAG);
        Magnitude.Band band = mag.getBand();
        double magVal = mag.getBrightness();
        Option<Double> error = mag.getError();
        MagnitudeSystem system = mag.getSystem();

        Pio.addParam(factory, magPset, MAG_BAND, band.name());
        Pio.addDoubleParam(factory, magPset, MAG_VAL, magVal);
        if (!error.isEmpty()) {
            Pio.addDoubleParam(factory, magPset, MAG_ERROR, error.getValue());
        }
        Pio.addParam(factory, magPset, MAG_SYSTEM, system.name());

        return magPset;
    }


    /**
     * Parses the given {@link ParamSet} into an immutable list of
     * {@link Magnitude} values, if possible.
     *
     * @param pset parameter set containing the description of the list of
     * magnitudes
     *
     * @return {@link ImList}<{@link Magnitude}> containing the magnitude
     * values
     *
     * @throws ParseException if there is a problem extracting the magnitude
     * list
     */
    public ImList<Magnitude> toList(ParamSet pset) throws ParseException {
        ImList<Magnitude> magList = ImCollections.emptyList();
        List<ParamSet> magPsetList = pset.getParamSets(MAG);
        if (magPsetList != null) {
            for (ParamSet magPset : magPsetList) {
                Magnitude mag = toMagnitude(magPset);
                if (mag != null) {
                    magList = magList.cons(mag);
                }
            }
        }
        return magList;
    }


    /**
     * Parses the given {@link ParamSet} into a {@link Magnitude} value, if
     * possible.
     *
     * @param pset parameter set containing the description of the magnitude
     *
     * @return {@link Magnitude} value corresponding to the information in the
     * {@link ParamSet}
     *
     * @throws ParseException if there is a problem extracting the magnitude
     */
    public Magnitude toMagnitude(ParamSet pset) throws ParseException {
        String bandName = Pio.getValue(pset, MAG_BAND);
        if (bandName == null) {
            throw new ParseException("Missing magnitude band.", 0);
        }

        // REL-549: "AB" and "Jy" were erroneously added to the list of Magnitude.Band options.
        // In MagnitudePio, when reading a ParamSet containing a magnitude band of either of these two,
        // it should be skipped with a warning log message
        if (bandName.equals("AB") || bandName.equals("Jy")) {
            LOGGER.warning("Ignoring invalid magnitude band: " + bandName);
            return null;
        }

        Magnitude.Band band;
        try {
            band = Magnitude.Band.valueOf(bandName);
        } catch (Exception ex) {
            String msg = String.format("Invalid magnitude band '%s'", bandName);
            throw new ParseException(msg, 0);
        }

        String valStr = Pio.getValue(pset, MAG_VAL);
        if (valStr == null) {
            String msg = String.format("Missing magnitude value for band '%s'", bandName);
            throw new ParseException(msg, 0);
        }

        double val;
        try {
            val = Double.parseDouble(valStr);
        } catch (NumberFormatException ex) {
            String msg = String.format("Could not extract manitude value from '%s' for band '%s'.", valStr, bandName);
            throw new ParseException(msg, 0);
        }

        Option<Double> error = None.instance();
        String errorStr = Pio.getValue(pset, MAG_ERROR);
        if (errorStr != null) {
            try {
                error = new Some<Double>(Double.parseDouble(errorStr));
            } catch (NumberFormatException ex) {
                String msg = String.format("Could not parse error information from '%s' for band '%s'.", errorStr, bandName);
                throw new ParseException(msg, 0);
            }
        }

        // Get the system and assume Vega if not specified.
        final String defaultSys = MagnitudeSystem.VEGA$.MODULE$.name();
        final String systemName = Optional.ofNullable(Pio.getValue(pset, MAG_SYSTEM)).orElse(defaultSys);
        final MagnitudeSystem system;
        try {
            system = MagnitudeSystem.allAsJava().stream().
                        filter(m -> m.name().equals(systemName)).
                        findFirst().
                        get();
        } catch (Exception ex) {
            String msg = String.format("Invalid magnitude system '%s'", systemName);
            throw new ParseException(msg, 0);
        }

        return new Magnitude(band, val, error, system);
    }

}
