package edu.gemini.qpt.ui.view.candidate;

import java.util.Set;
import java.util.logging.Logger;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Variant.Flag;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.util.StructuredProgramID;
import edu.gemini.qpt.ui.util.BooleanViewPreference;

import static edu.gemini.qpt.ui.util.BooleanViewPreference.*;

import edu.gemini.spModel.core.*;
import edu.gemini.shared.util.immutable.Option;

public enum ClientExclusion {

    NULL("No current variant.", null),

    HIDDEN_BAND_1("Program is in Band 1.", VIEW_BAND_1),
    HIDDEN_BAND_2("Program is in Band 2.", VIEW_BAND_2),
    HIDDEN_BAND_3("Program is in Band 3.", VIEW_BAND_3),
    HIDDEN_BAND_4("Program is in Band 4.", VIEW_BAND_4),
    HIDDEN_LP("Program is of type "   + ProgramType$.MODULE$.LP().name(),  VIEW_SP_LP),
    HIDDEN_C("Program is of type "    + ProgramType$.MODULE$.C().name(),   VIEW_SP_C),
    HIDDEN_FT("Program is of type "   + ProgramType$.MODULE$.FT().name(),  VIEW_SP_FT),
    HIDDEN_Q("Program is of type "    + ProgramType$.MODULE$.Q().name(),   VIEW_SP_Q),
    HIDDEN_SV("Program is of type "   + ProgramType$.MODULE$.SV().name(),  VIEW_SP_SV),
    HIDDEN_DD("Program is of type "   + ProgramType$.MODULE$.DD().name(),  VIEW_SP_DD),
    HIDDEN_DS("Program is of type "   + ProgramType$.MODULE$.DS().name(),  VIEW_SP_DS),
    HIDDEN_ENG("Program is of type "  + ProgramType$.MODULE$.ENG().name(), VIEW_SP_ENG),
    HIDDEN_PCAL("Program is of type " + ProgramType$.MODULE$.CAL().name(), VIEW_SP_CAL),
    HIDDEN_INACTIVE_PROGRAMS("Program is inactive.", VIEW_INACTIVE_PROGRAMS),

    HIDDEN_NIGHT_CALS("Obs is a nighttime calibration.", VIEW_NIGHTTIME_CALIBRATIONS),
    HIDDEN_DAY_CALS("Obs is a daytime calibration.", VIEW_DAYTIME_CALIBRATIONS),
    HIDDEN_SCI("Obs is science.", VIEW_SCIENCE_OBS),

    HIDDEN_OVER_QUALIFIED_OBSERVATIONS("Obs is over-qualified.", VIEW_OVER_QUALIFIED_OBSERVATIONS),
    HIDDEN_BLOCKED_OBSERVATIONS("Obs is blocked.", VIEW_BLOCKED_OBSERVATIONS),

    HIDDEN_UNDER_QUALIFIED_OBSERVATIONS("Obs is under-qualified.", VIEW_UNDER_QUALIFIED_OBSERVATIONS),
    HIDDEN_UNAVAILABLE("Inst/Config is unavailable.", VIEW_UNAVAILABLE),
    HIDDEN_NON_LGS("Only showing LGS observations.", VIEW_LGS_ONLY),
    HIDDEN_UNSCHEDULABLE("Obs is unschedulable.", VIEW_UNSCHEDULABLE),
    HIDDEN_NOT_DARK_ENOUGH("Sky is not dark enough.", VIEW_NOT_DARK_ENOUGH),
    HIDDEN_LOW_IN_SKY("Target is low in sky.", VIEW_LOW_IN_SKY),;

    private static final Logger LOGGER = Logger.getLogger(ClientExclusion.class.getName());

    public static ClientExclusion forObs(Variant variant, Obs obs) {

        // If there is no variant, reject all.
        // RCN: does this ever happen?
        if (variant == null) return NULL;

        // If "Show All" is selected, accept it.
        if (VIEW_ALL.get()) return null;

        Set<Flag> flags = variant.getFlags(obs);
        if (!VIEW_INACTIVE_PROGRAMS.get() && flags.contains(Flag.INACTIVE))
            return HIDDEN_INACTIVE_PROGRAMS;

        // Band filtering
        // Daily Engineering and Calibration observations are not filtered by band
        if (!obs.getProg().isEngOrCal()) {
            switch (obs.getProg().getBand()) {
                case 1:
                    if (!VIEW_BAND_1.get()) return HIDDEN_BAND_1;
                    break;
                case 2:
                    if (!VIEW_BAND_2.get()) return HIDDEN_BAND_2;
                    break;
                case 3:
                    if (!VIEW_BAND_3.get()) return HIDDEN_BAND_3;
                    break;
                case 4:
                    if (!VIEW_BAND_4.get()) return HIDDEN_BAND_4;
                    break;
                default:
                    LOGGER.warning("Program " + obs.getProg() + " has unexpected science band: " + obs.getProg().getBand());
            }
        }
        // SP Type filtering


        final Option<ProgramType> t = obs.getProg().getTypeAsJava();
        if (t.isDefined()) {
            switch (t.getValue().typeEnum()) {
                case C:
                    if (!VIEW_SP_C.get()) return HIDDEN_C;
                    break;
                case CAL:
                    if (!VIEW_SP_CAL.get()) return HIDDEN_PCAL;
                    break;
                case DD:
                    if (!VIEW_SP_DD.get()) return HIDDEN_DD;
                    break;
                case DS:
                    if (!VIEW_SP_DS.get()) return HIDDEN_DS;
                    break;
                case ENG:
                    if (!VIEW_SP_ENG.get()) return HIDDEN_ENG;
                    break;
                case FT:
                    if (!VIEW_SP_FT.get()) return HIDDEN_FT;
                    break;
                case LP:
                    if (!VIEW_SP_LP.get()) return HIDDEN_LP;
                    break;
                case Q:
                    if (!VIEW_SP_Q.get()) return HIDDEN_Q;
                    break;
                case SV:
                    if (!VIEW_SP_SV.get()) return HIDDEN_SV;
                    break;
            }
        }

        // Obs type filtering
        // (ObsQueryFunctor/MiniModel is configured to let only the obs classes through listed below.)
        switch (obs.getObsClass()) {
            case PROG_CAL:
                if (!VIEW_NIGHTTIME_CALIBRATIONS.get())
                    return HIDDEN_NIGHT_CALS;
                break;
            case PARTNER_CAL:
                if (!VIEW_NIGHTTIME_CALIBRATIONS.get())
                    return HIDDEN_NIGHT_CALS;
                break;
            case DAY_CAL:
                if (!VIEW_DAYTIME_CALIBRATIONS.get()) return HIDDEN_DAY_CALS;
                break;
            case SCIENCE:
                if (!VIEW_SCIENCE_OBS.get()) return HIDDEN_SCI;
                break;
        }

        // Flag-based filters
        if (!VIEW_LOW_IN_SKY.get() && flags.contains(Flag.ELEVATION_CNS))
            return HIDDEN_LOW_IN_SKY;
        if (!VIEW_BLOCKED_OBSERVATIONS.get() && flags.contains(Flag.BLOCKED))
            return HIDDEN_BLOCKED_OBSERVATIONS;
        if (!VIEW_OVER_QUALIFIED_OBSERVATIONS.get() && flags.contains(Flag.OVER_QUALIFIED))
            return HIDDEN_OVER_QUALIFIED_OBSERVATIONS;
        if (!VIEW_UNAVAILABLE.get() && (flags.contains(Flag.INSTRUMENT_UNAVAILABLE) || flags.contains(Flag.CONFIG_UNAVAILABLE) || flags.contains(Flag.LGS_UNAVAILABLE)))
            return HIDDEN_UNAVAILABLE;
        if (VIEW_LGS_ONLY.get() && !obs.getLGS() && variant.getLgsConstraint())
            return HIDDEN_NON_LGS;
        if (!VIEW_UNDER_QUALIFIED_OBSERVATIONS.get() && flags.contains(Flag.IQ_UQUAL))
            return HIDDEN_UNDER_QUALIFIED_OBSERVATIONS;
        if (!VIEW_UNDER_QUALIFIED_OBSERVATIONS.get() && flags.contains(Flag.CC_UQUAL))
            return HIDDEN_UNDER_QUALIFIED_OBSERVATIONS;
        if (!VIEW_UNDER_QUALIFIED_OBSERVATIONS.get() && flags.contains(Flag.WV_UQUAL))
            return HIDDEN_UNDER_QUALIFIED_OBSERVATIONS;
        if (!VIEW_UNSCHEDULABLE.get() && flags.contains(Flag.MULTI_CNS))
            return HIDDEN_UNSCHEDULABLE;
        if (!VIEW_NOT_DARK_ENOUGH.get() && flags.contains(Flag.BACKGROUND_CNS))
            return HIDDEN_NOT_DARK_ENOUGH;

        return null;

    }

    private final String string;
    private final BooleanViewPreference pref;

    ClientExclusion(final String string, final BooleanViewPreference pref) {
        this.string = string;
        this.pref = pref;
    }

    public BooleanViewPreference getPref() {
        return pref;
    }

    @Override
    public String toString() {
        return string;
    }

}
