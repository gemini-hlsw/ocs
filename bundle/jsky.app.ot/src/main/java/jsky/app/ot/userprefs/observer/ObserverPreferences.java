package jsky.app.ot.userprefs.observer;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import jsky.app.ot.userprefs.model.ExternalizablePreferences;
import jsky.app.ot.userprefs.model.PreferencesChangeListener;
import jsky.app.ot.userprefs.model.PreferencesSupport;
import jsky.util.Preferences;

/**
 * Preferences related to executing observations.
 */
public final class ObserverPreferences implements ExternalizablePreferences {
    public static final class Builder {
        private Site observingSite = null;
        private boolean audibleTooAlerts = false;

        Builder() { /* empty */ }

        Builder observingSite(Site val) {
            observingSite = val;
            return this;
        }

        Builder audibleTooAlerts(boolean val) {
            audibleTooAlerts = val;
            return this;
        }

        public ObserverPreferences build() {
            return new ObserverPreferences(this);
        }
    }

    public static final class Factory implements ExternalizablePreferences.Factory<ObserverPreferences> {
        // Handle upgrading from past versions where preference bits were
        // stored in Jsky Preferences.
        private static final String TOO_PREF = "jsky.app.ot.OTOptions.audibleTooAlertsEnabled";

        private ObserverPreferences createDefault() {
            Builder b = new Builder();
            b.audibleTooAlerts(Preferences.get(TOO_PREF, true));
            return b.build();
        }

        private Site parseSite(String s) {
            return (s == null) ? null : Site.tryParse(s);
        }

        public ObserverPreferences create(Option<ParamSet> psetOpt) {
            if (None.instance().equals(psetOpt)) return createDefault();

            Builder b = new Builder();
            ParamSet pset = psetOpt.getValue();
            b.observingSite(parseSite(Pio.getValue(pset, OBSERVING_SITE_PARAM)));
            b.audibleTooAlerts(Pio.getBooleanValue(pset, AUDIBLE_TOO_PARAM, false));
            return b.build();
        }
    }

    private static final Factory FACTORY = new Factory();
    private static final String PSET_NAME = "observer";

    private static final String OBSERVING_SITE_PARAM = "observingSite";
    private static final String AUDIBLE_TOO_PARAM = "audibleToo";

    private final Site observingSite;
    private final boolean audibleTooAlerts;

    private ObserverPreferences(Builder builder) {
        this.observingSite = builder.observingSite;
        this.audibleTooAlerts = builder.audibleTooAlerts;
    }

    /**
     * Observing site is where we listen to ToO events, where we queue
     * observations, etc.  This won't be set for non-staff members.
     *
     * @return the observing site (if any); <code>null</code> otherwise
     */
    public Site observingSite() {
        return observingSite;
    }

    /**
     * @return <code>true</code> if rapid TOO alerts for science observations
     *         should generate an audible message; <code>false</code> otherwise
     */
    public boolean isAudibleTooAlerts() {
        return audibleTooAlerts;
    }

    private Builder getBuilder() {
        Builder b = new Builder();
        b.observingSite(this.observingSite);
        b.audibleTooAlerts(this.audibleTooAlerts);
        return b;
    }

    ObserverPreferences withObservingSite(Site val) {
        return getBuilder().observingSite(val).build();
    }

    ObserverPreferences withAudibleTooAlerts(boolean val) {
        return getBuilder().audibleTooAlerts(val).build();
    }

    public boolean store() {
        return store(this);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet res = factory.createParamSet(PSET_NAME);
        Pio.addParam(factory, res, OBSERVING_SITE_PARAM, (observingSite == null) ? "" : observingSite.name());
        Pio.addBooleanParam(factory, res, AUDIBLE_TOO_PARAM, audibleTooAlerts);
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObserverPreferences that = (ObserverPreferences) o;
        if (observingSite != that.observingSite) return false;
        return audibleTooAlerts == that.audibleTooAlerts;
    }

    @Override
    public int hashCode() {
        int result = (observingSite == null) ? 0 : observingSite.hashCode();
        result = 31 * result + (audibleTooAlerts ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("[observingSite=%s, audibleTooAlerts=%b]", observingSite, audibleTooAlerts);
    }

    private static final PreferencesSupport<ObserverPreferences> sup =
            new PreferencesSupport<>(PSET_NAME, FACTORY);

    public static ObserverPreferences fetch() {
        return sup.fetch();
    }

    public static boolean store(ObserverPreferences val) {
        return sup.store(val);
    }

    public static void addChangeListener(PreferencesChangeListener<ObserverPreferences> listener) {
        sup.addChangeListener(listener);
    }

}
