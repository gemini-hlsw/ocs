package edu.gemini.lchquery.servlet;

import java.util.Collections;
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
    public static final String PARAMETER_PROGRAM_INVESTIGATOR_NAMES = "programInvestigatorNames";
    public static final String PARAMETER_PROGRAM_PI_EMAIL = "programPiEmail";
    public static final String PARAMETER_PROGRAM_COI_EMAILS = "programCoiEmails";
    public static final String PARAMETER_PROGRAM_ABSTRACT = "programAbstract";
    public static final String PARAMETER_PROGRAM_BAND = "programBand";
    public static final String PARAMETER_PROGRAM_PARTNERS = "programPartners";
    public static final String PARAMETER_PROGRAM_REFERENCE = "programReference";
    public static final String PARAMETER_PROGRAM_ACTIVE = "programActive";
    public static final String PARAMETER_PROGRAM_COMPLETED = "programCompleted";
    public static final String PARAMETER_PROGRAM_NOTIFY_PI = "programNotifyPi";
    public static final String PARAMETER_PROGRAM_ROLLOVER = "programRollover";
    public static final String PARAMETER_PROGRAM_TOO_STATUS = "programTooStatus"; // new
    public static final String PARAMETER_PROGRAM_ALLOC_TIME = "programAllocatedTime"; // new
    public static final String PARAMETER_PROGRAM_REMAIN_TIME = "programRemainTime"; // new

    public static final String PARAMETER_OBSERVATION_TOO_STATUS = "observationTooStatus";
    public static final String PARAMETER_OBSERVATION_NAME = "observationName";
    public static final String PARAMETER_OBSERVATION_STATUS = "observationStatus";
    public static final String PARAMETER_OBSERVATION_INSTRUMENT = "observationInstrument";
    public static final String PARAMETER_OBSERVATION_AO = "observationAo";
    public static final String PARAMETER_OBSERVATION_CLASS = "observationClass";

    private static final Set<String> PARAM_SET = new HashSet<>();
    static {
        PARAM_SET.add(PARAMETER_PROGRAM_SEMESTER);
        PARAM_SET.add(PARAMETER_PROGRAM_TITLE);
        PARAM_SET.add(PARAMETER_PROGRAM_INVESTIGATOR_NAMES);
        PARAM_SET.add(PARAMETER_PROGRAM_PI_EMAIL);
        PARAM_SET.add(PARAMETER_PROGRAM_COI_EMAILS);
        PARAM_SET.add(PARAMETER_PROGRAM_ABSTRACT);
        PARAM_SET.add(PARAMETER_PROGRAM_BAND);
        PARAM_SET.add(PARAMETER_PROGRAM_PARTNERS);
        PARAM_SET.add(PARAMETER_PROGRAM_REFERENCE);
        PARAM_SET.add(PARAMETER_PROGRAM_ACTIVE);
        PARAM_SET.add(PARAMETER_PROGRAM_COMPLETED);
        PARAM_SET.add(PARAMETER_PROGRAM_NOTIFY_PI);
        PARAM_SET.add(PARAMETER_PROGRAM_ROLLOVER);
        PARAM_SET.add(PARAMETER_PROGRAM_TOO_STATUS);
        PARAM_SET.add(PARAMETER_PROGRAM_ALLOC_TIME);
        PARAM_SET.add(PARAMETER_PROGRAM_REMAIN_TIME);

        PARAM_SET.add(PARAMETER_OBSERVATION_TOO_STATUS);
        PARAM_SET.add(PARAMETER_OBSERVATION_NAME);
        PARAM_SET.add(PARAMETER_OBSERVATION_STATUS);
        PARAM_SET.add(PARAMETER_OBSERVATION_INSTRUMENT);
        PARAM_SET.add(PARAMETER_OBSERVATION_AO);
        PARAM_SET.add(PARAMETER_OBSERVATION_CLASS);
    }

    public static final Set<String> PARAMS = Collections.unmodifiableSet(PARAM_SET)
    public static final boolean isValidParameter(String s) {
        return PARAM_SET.contains(s);
    }
}
