package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;

/**
 * A service used to determine the instrument used by an observation.  Examines
 * the static components until it finds an instrument component, then extracts
 * the instrument from the `SPComponentType`.
 */
public final class InstrumentService {

    private InstrumentService() {
    }

    /**
     * Determines the instrument for the given observation by examining the
     * observation's components to find the first one that is an instrument, if
     * any.
     *
     * @param obs the observation whose instrument is sought
     *
     * @return the instrument in use in the observation, if any
     */
    public static Option<Instrument> lookupInstrument(ISPObservation obs) {

        // First check the cache.
        Option<Instrument> inst = SPObsCache.getInstrument(obs);
        if (inst.isEmpty()) {

            // Compute the value.
            inst = SPObsCache.getInstrument(obs).orElse(() ->
                       ImOption.fromOptional(
                           obs.getObsComponents()
                              .stream()
                              .filter(c -> c.getType().broadType == SPComponentBroadType.INSTRUMENT)
                              .flatMap(c -> Instrument.fromComponentType(c.getType()).toStream())
                              .findFirst()
                       )
            );

            // Cache the results.
            if (inst.isDefined()) SPObsCache.setInstrument(obs, inst);
        }

        return inst;
    }

}
