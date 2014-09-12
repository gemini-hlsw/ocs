package edu.gemini.lchquery.servlet;

import java.util.HashSet;
import java.util.Set;

/**
 * Definitions for the lch query service.
 */
public final class LchQueryService {
    private LchQueryService() { }

    /**
     * valid parameters
     */
    public static final String PARAMETER_PROGRAM_SEMESTER = "programSemester";
    public static final String PARAMETER_PROGRAM_TITLE = "programTitle";
    public static final String PARAMETER_PROGRAM_REFERENCE = "programReference";
    public static final String PARAMETER_PROGRAM_ACTIVE = "programActive";
    public static final String PARAMETER_PROGRAM_COMPLETED = "programCompleted";
    public static final String PARAMETER_PROGRAM_NOTIFY_PI = "programNotifyPi";
    public static final String PARAMETER_PROGRAM_ROLLOVER = "programRollover";


    public static final String PARAMETER_OBSERVATION_TOO_STATUS = "observationTooStatus";
    public static final String PARAMETER_OBSERVATION_NAME = "observationName";
    public static final String PARAMETER_OBSERVATION_STATUS = "observationStatus";
    public static final String PARAMETER_OBSERVATION_INSTRUMENT = "observationInstrument";
    public static final String PARAMETER_OBSERVATION_AO = "observationAo";
    public static final String PARAMETER_OBSERVATION_CLASS = "observationClass";

    private static final Set<String> PARAM_SET = new HashSet<String>();
    static {
        PARAM_SET.add(PARAMETER_PROGRAM_SEMESTER);
        PARAM_SET.add(PARAMETER_PROGRAM_TITLE);
        PARAM_SET.add(PARAMETER_PROGRAM_REFERENCE);
        PARAM_SET.add(PARAMETER_PROGRAM_ACTIVE);
        PARAM_SET.add(PARAMETER_PROGRAM_COMPLETED);
        PARAM_SET.add(PARAMETER_PROGRAM_NOTIFY_PI);
        PARAM_SET.add(PARAMETER_PROGRAM_ROLLOVER);

        PARAM_SET.add(PARAMETER_OBSERVATION_TOO_STATUS);
        PARAM_SET.add(PARAMETER_OBSERVATION_NAME);
        PARAM_SET.add(PARAMETER_OBSERVATION_STATUS);
        PARAM_SET.add(PARAMETER_OBSERVATION_INSTRUMENT);
        PARAM_SET.add(PARAMETER_OBSERVATION_AO);
        PARAM_SET.add(PARAMETER_OBSERVATION_CLASS);
    }

    public static final boolean isValidParameter(String s) {
        return PARAM_SET.contains(s);
    }
}
