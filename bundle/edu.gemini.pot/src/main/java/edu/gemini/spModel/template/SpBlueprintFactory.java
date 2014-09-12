package edu.gemini.spModel.template;

import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintImaging;
import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintLongslit;
import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintMos;
import edu.gemini.spModel.gemini.gmos.blueprint.*;
import edu.gemini.spModel.gemini.gnirs.blueprint.SpGnirsBlueprintImaging;
import edu.gemini.spModel.gemini.gnirs.blueprint.SpGnirsBlueprintSpectroscopy;
import edu.gemini.spModel.gemini.gpi.blueprint.SpGpiBlueprint;
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint;
import edu.gemini.spModel.gemini.gsaoi.blueprint.SpGsaoiBlueprint;
import edu.gemini.spModel.gemini.michelle.blueprint.SpMichelleBlueprintImaging;
import edu.gemini.spModel.gemini.michelle.blueprint.SpMichelleBlueprintSpectroscopy;
import edu.gemini.spModel.gemini.nici.blueprint.SpNiciBlueprintCoronagraphic;
import edu.gemini.spModel.gemini.nici.blueprint.SpNiciBlueprintStandard;
import edu.gemini.spModel.gemini.nifs.blueprint.SpNifsBlueprint;
import edu.gemini.spModel.gemini.nifs.blueprint.SpNifsBlueprintAo;
import edu.gemini.spModel.gemini.niri.blueprint.SpNiriBlueprint;
import edu.gemini.spModel.gemini.texes.blueprint.SpTexesBlueprint;
import edu.gemini.spModel.gemini.trecs.blueprint.SpTrecsBlueprintImaging;
import edu.gemini.spModel.gemini.trecs.blueprint.SpTrecsBlueprintSpectroscopy;
import edu.gemini.spModel.gemini.visitor.blueprint.SpVisitorBlueprint;
import edu.gemini.spModel.pio.ParamSet;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// SW: I lifted this from SpBlueprint itself because it caused cyclic class
// loading.  As part of SpBlueprint, the first time loading any SpBlueprint
// subclass required loading all the others.  In a multi-threaded context this
// can deadlock when another thread is loading an SpBlueprint class.

/**
 * Provides support for creating the proper SpBlueprint from a ParamSet.
 */
public final class SpBlueprintFactory {
    private static final Logger LOGGER = Logger.getLogger(SpBlueprint.class.getName());

        // A map from each blueprint's PARAM_SET_NAME to its ParamSet ctor. This is spiritually flawed.
    private static final Map<String, Constructor<? extends SpBlueprint>> ctors =
            new TreeMap<String, Constructor<? extends SpBlueprint>>();

    static {

        // All concrete suclasses of SpBlueprint must be included here. There is no way to check this statically.
        @SuppressWarnings("unchecked") final List<Class<? extends SpBlueprint>> types = Arrays.asList(
                SpFlamingos2BlueprintImaging.class,
                SpFlamingos2BlueprintLongslit.class,
                SpFlamingos2BlueprintMos.class,
                SpGmosNBlueprintIfu.class,
                SpGmosNBlueprintImaging.class,
                SpGmosNBlueprintLongslit.class,
                SpGmosNBlueprintLongslitNs.class,
                SpGmosNBlueprintMos.class,
                SpGmosSBlueprintIfu.class,
                SpGmosSBlueprintIfuNs.class,
                SpGmosSBlueprintImaging.class,
                SpGmosSBlueprintLongslit.class,
                SpGmosSBlueprintLongslitNs.class,
                SpGmosSBlueprintMos.class,
                SpGnirsBlueprintImaging.class,
                SpGnirsBlueprintSpectroscopy.class,
                SpGpiBlueprint.class,
                SpGracesBlueprint.class,
                SpGsaoiBlueprint.class,
                SpMichelleBlueprintImaging.class,
                SpMichelleBlueprintSpectroscopy.class,
                SpNiciBlueprintCoronagraphic.class,
                SpNiciBlueprintStandard.class,
                SpNifsBlueprint.class,
                SpNifsBlueprintAo.class,
                SpNiriBlueprint.class,
                SpTrecsBlueprintImaging.class,
                SpTrecsBlueprintSpectroscopy.class,
                SpTexesBlueprint.class,
                SpVisitorBlueprint.class
        );

        // Map each class's PARAM_SET_NAME to its ParamSet ctor
        for (Class<? extends SpBlueprint> c: types) {
            try {
                final String psn = (String) c.getField("PARAM_SET_NAME").get(null);
                final Constructor<? extends SpBlueprint> ctor = c.getConstructor(ParamSet.class);
                ctors.put(psn, ctor);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Trouble mapping ctor for " + c.getName(), e);
            }
        }

    }

    /** Returns true if the given ParamSet represents an SpBlueprint. */
    public static boolean isSpBlueprintParamSet(ParamSet ps) {
        return ctors.containsKey(ps.getName());
    }

    /** Deswizzles a ParamSet into a SpBlueprint, or throws a RuntimeException on failure. */
    public static SpBlueprint fromParamSet(ParamSet ps) {
        if (!isSpBlueprintParamSet(ps))
            throw new NoSuchElementException("No constructor found for ParamSet name " + ps.getName());
        try {
            return ctors.get(ps.getName()).newInstance(ps);
        } catch (Exception e) {
            throw new RuntimeException("Trouble parsing ParamSet " +  ps.getName(), e);
        }
    }

}
