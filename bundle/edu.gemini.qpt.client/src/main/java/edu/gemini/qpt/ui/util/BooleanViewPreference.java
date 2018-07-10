package edu.gemini.qpt.ui.util;

public enum BooleanViewPreference implements PreferenceManager.Key<Boolean> {

    // Show All (normally off)
    VIEW_ALL(false),

    // Warnings (normally on)
    VIEW_OVER_QUALIFIED_OBSERVATIONS(true),
    VIEW_BLOCKED_OBSERVATIONS(true),

    // Errors (normally off)
    VIEW_UNDER_QUALIFIED_OBSERVATIONS(false),
    VIEW_UNAVAILABLE(false),
    VIEW_MASK_IN_CABINET(false),
    VIEW_UNSCHEDULABLE(false),
    VIEW_NOT_DARK_ENOUGH(false),
    VIEW_LOW_IN_SKY(false),

//    VIEW_BAND_0("Non-Queue Programs", false),
    VIEW_INACTIVE_PROGRAMS(false),
    VIEW_SCIENCE_OBS(true),
    VIEW_NIGHTTIME_CALIBRATIONS(false),
    VIEW_DAYTIME_CALIBRATIONS(false),

    VIEW_BAND_1(true),
    VIEW_BAND_2(true),
    VIEW_BAND_3(true),
    VIEW_BAND_4(false),

    VIEW_SP_LP(true),
    VIEW_SP_FT(true),
    VIEW_SP_Q(true),
    VIEW_SP_C(false),
    VIEW_SP_SV(false),
    VIEW_SP_DD(true),
    VIEW_SP_DS(false),
    VIEW_SP_ENG(false),
    VIEW_SP_CAL(false),

    SHOW_IN_VISUALIZER(false),

    VIEW_LGS_ONLY(false),
    ;

    final Boolean defaultValue;

    BooleanViewPreference(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getDefaultValue() {
        return defaultValue;
    }

    public Boolean get() {
        return PreferenceManager.get(this);
    }

}
