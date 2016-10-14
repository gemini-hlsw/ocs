package jsky.app.ot.gemini.obscat;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import jsky.app.ot.userprefs.model.ExternalizablePreferences;
import jsky.app.ot.userprefs.model.PreferencesSupport;

/**
 * Preferences that pertain to the OT Browser.
 */
public final class BrowserPreferences implements ExternalizablePreferences {

    public static class Builder {
        private boolean showObsoleteOptions = true;

        Builder() { /* empty */ }

        Builder showObsoleteOptions(boolean val) {
            showObsoleteOptions = val;
            return this;
        }

        public BrowserPreferences build() {
            return new BrowserPreferences(this);
        }
    }

    private static class Factory implements ExternalizablePreferences.Factory<BrowserPreferences> {

        private BrowserPreferences createDefault() {
            return (new Builder()).build();
        }

        @Override
        public BrowserPreferences create(Option<ParamSet> psetOpt) {
            if (None.instance().equals(psetOpt)) return createDefault();

            final Builder b = new Builder();
            final ParamSet pset = psetOpt.getValue();
            b.showObsoleteOptions(Pio.getBooleanValue(pset, OBSOLETE_OPTION_PARAM, true));
            return b.build();
        }
    }

    private static final String BROWSER_PREFS         = "browser";
    private static final String OBSOLETE_OPTION_PARAM = "showObsolete";

    private final boolean showObsoleteOptions;

    private BrowserPreferences(Builder builder) {
        this.showObsoleteOptions = builder.showObsoleteOptions;
    }

    boolean showObsoleteOptions() { return showObsoleteOptions; }

    private Builder getBuilder() {
        final Builder b = new Builder();
        b.showObsoleteOptions(this.showObsoleteOptions);
        return b;
    }

    BrowserPreferences withShowObsoleteOptions(boolean val) {
        return getBuilder().showObsoleteOptions(val).build();
    }

    public void store() {
        store(this);
    }

    @Override
    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet res = factory.createParamSet(BROWSER_PREFS);
        Pio.addBooleanParam(factory, res, OBSOLETE_OPTION_PARAM, showObsoleteOptions);
        return res;
    }

    private static final PreferencesSupport<BrowserPreferences> sup =
            new PreferencesSupport<>(BROWSER_PREFS, new Factory());

    public static BrowserPreferences fetch() {
        return sup.fetch();
    }

    private static void store(BrowserPreferences val) {
        sup.store(val);
    }

}
