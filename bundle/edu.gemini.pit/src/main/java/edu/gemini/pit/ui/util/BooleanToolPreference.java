package edu.gemini.pit.ui.util;

// Copied from QPT, sorry
public enum BooleanToolPreference implements PreferenceManager.Key<Boolean> {

    SIMBAD(true),
    NED(false),
    HORIZONS(true)
    ;

    final Boolean defaultValue;

    BooleanToolPreference(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getDefaultValue() {
        return defaultValue;
    }

    public Boolean get() {
        return PreferenceManager.get(this);
    }

    public void set(Boolean value) {
        PreferenceManager.set(this, value);
    }

}
