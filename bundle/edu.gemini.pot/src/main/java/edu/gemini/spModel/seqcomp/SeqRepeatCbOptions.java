package edu.gemini.spModel.seqcomp;

import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProvider;

import java.util.Map;

public class SeqRepeatCbOptions {
    public static final String COLLAPSE_REPEAT_OPTION = "collapseRepeat";
    public static final String ADD_OBS_COUNT_OPTION = "addObsCount";
    public static final String CALIBRATION_PROVIDER_OPTION = "calibrationProvider";

    private static boolean getBooleanOption(Map<String, Object> options, String key) {
        if (options == null) return false;
        Boolean val = (Boolean) options.get(key);
        return (val != null) && val;
    }

    public static boolean getCollapseRepeat(Map<String, Object> options) {
        return getBooleanOption(options, COLLAPSE_REPEAT_OPTION);
    }

    public static void setCollapseRepeat(Map<String, Object> options, boolean collapse) {
        options.put(COLLAPSE_REPEAT_OPTION, collapse ? Boolean.TRUE : Boolean.FALSE);
    }

    public static boolean getAddObsCount(Map<String, Object> options) {
        return getBooleanOption(options, ADD_OBS_COUNT_OPTION);
    }

    public static void setAddObsCount(Map<String, Object> options, boolean addObsCount) {
        options.put(ADD_OBS_COUNT_OPTION, addObsCount ? Boolean.TRUE : Boolean.FALSE);
    }

    public static CalibrationProvider getCalibrationProvider(Map<String, Object> options) {
        return (CalibrationProvider) options.get(CALIBRATION_PROVIDER_OPTION);
    }

    public static void setCalibrationProvider(Map<String, Object> options, CalibrationProvider calibrationProvider) {
        options.put(CALIBRATION_PROVIDER_OPTION, calibrationProvider);
    }
}
