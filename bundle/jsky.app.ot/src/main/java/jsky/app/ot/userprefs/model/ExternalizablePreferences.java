package jsky.app.ot.userprefs.model;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

/**
 * Interface describing an OT user preferences collection that can be
 * externalized to PIO {@link ParamSet}s.
 */
public interface ExternalizablePreferences {

    /**
     * A factory for creating an instance from a {@link ParamSet} describing
     * its content.
     */
    interface Factory<T extends ExternalizablePreferences> {

        /**
         * Creates the UserPreferences instance from a {@link ParamSet} if
         * possible.
         *
         * @param container param set containing the parameters that make up
         * this preferences item
         */
        T create(Option<ParamSet> container);
    }

    /**
     * Obtains a {@link ParamSet} representation for the user preferences
     * suitable for storing by the {@link PreferencesDatabase}.
     * @param {@link PioFactory factory} used to create the {@link ParamSet}
     */
    ParamSet toParamSet(PioFactory factory);
}
