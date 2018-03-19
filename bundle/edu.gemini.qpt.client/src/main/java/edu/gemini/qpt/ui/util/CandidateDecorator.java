package edu.gemini.qpt.ui.util;

import static edu.gemini.qpt.core.Variant.Flag.BACKGROUND_CNS;
import static edu.gemini.qpt.core.Variant.Flag.BLOCKED;
import static edu.gemini.qpt.core.Variant.Flag.CC_UQUAL;
import static edu.gemini.qpt.core.Variant.Flag.CONFIG_UNAVAILABLE;
import static edu.gemini.qpt.core.Variant.Flag.LGS_UNAVAILABLE;
import static edu.gemini.qpt.core.Variant.Flag.ELEVATION_CNS;
import static edu.gemini.qpt.core.Variant.Flag.INACTIVE;
import static edu.gemini.qpt.core.Variant.Flag.INSTRUMENT_UNAVAILABLE;
import static edu.gemini.qpt.core.Variant.Flag.IN_PROGRESS;
import static edu.gemini.qpt.core.Variant.Flag.IQ_UQUAL;
import static edu.gemini.qpt.core.Variant.Flag.MULTI_CNS;
import static edu.gemini.qpt.core.Variant.Flag.OVER_ALLOCATED;
import static edu.gemini.qpt.core.Variant.Flag.OVER_QUALIFIED;
import static edu.gemini.qpt.core.Variant.Flag.SCHEDULED;
import static edu.gemini.qpt.core.Variant.Flag.TIME_CONSTRAINED;
import static edu.gemini.qpt.core.Variant.Flag.TIMING_CNS;
import static edu.gemini.qpt.core.Variant.Flag.WV_UQUAL;
import static edu.gemini.qpt.ui.util.SharedIcons.*;
import edu.gemini.spModel.core.*;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;

import javax.swing.Icon;

import edu.gemini.qpt.core.Variant.Flag;
import edu.gemini.qpt.shared.sp.Obs;

public class CandidateDecorator {

    private static final EnumSet<Flag> DARK_GRAY = EnumSet.of(
            OVER_QUALIFIED
    );

    private static final EnumSet<Flag> GRAY = EnumSet.of(
            INSTRUMENT_UNAVAILABLE,
            CONFIG_UNAVAILABLE,
            LGS_UNAVAILABLE,
            BLOCKED,
            INACTIVE,
            IQ_UQUAL,
            WV_UQUAL,
            CC_UQUAL,
            OVER_ALLOCATED,
            ELEVATION_CNS,
            BACKGROUND_CNS,
            TIMING_CNS,
            MULTI_CNS
    );

    private static final EnumSet<Flag> CROSSED_OUT = EnumSet.of(
            IQ_UQUAL,
            WV_UQUAL,
            CC_UQUAL,
            INSTRUMENT_UNAVAILABLE,
            CONFIG_UNAVAILABLE,
            LGS_UNAVAILABLE,
            ELEVATION_CNS,
            BACKGROUND_CNS,
            TIMING_CNS,
            MULTI_CNS
    );

    public static Icon getIcon(Set<Flag> flags, Obs obs) {

        boolean isGray = isGray(flags);
        boolean isDim = isDarkGray(flags);

        // Find the base icon.
        Icon icon;
        if (obs.getObsClass() == null) {
            icon = ICON_BOGUS;
        } else {
            switch (obs.getObsClass()) {
                case ACQ:
                case ACQ_CAL:
                    icon = ICON_ACQ;
                    break;
                case DAY_CAL:
                case PARTNER_CAL:
                case PROG_CAL:
                    icon = isGray ? ICON_CALIB_DIS : isDim ? ICON_CALIB_DIM : ICON_CALIB;
                    break;
                case SCIENCE:
                    icon = isGray ? ICON_SCIENCE_DIS : isDim ? ICON_SCIENCE_DIM : ICON_SCIENCE;
                    break;
                default:
                    throw new Error("Impossible.");
            }
        }

        if (obs.getProg().isType(ProgramTypeEnum.ENG)) {
            icon = isGray ? ICON_DAYCAL_DIS : isDim ? ICON_DAYENG_DIM : ICON_DAYENG;
        } else if (obs.getProg().isType(ProgramTypeEnum.CAL)) {
            icon = isGray ? ICON_DAYCAL_DIS : isDim ? ICON_DAYCAL_DIM : ICON_DAYCAL;
        }

        // If in progress
        if (flags.contains(IN_PROGRESS)) {
            icon = new CompositeIcon(icon, OVL_IN_PROGRESS);
        }

        // Add slash if we need to.
        if (isCrossedOut(flags)) {
            icon = new CompositeIcon(icon, OVL_CROSSED_OUT);
        }

        // If timing window (after slash so it's on top)
        if (flags.contains(TIME_CONSTRAINED)) {
            icon = new CompositeIcon(icon, OVL_TIMING);
        }

        if (flags.contains(SCHEDULED)) {
            icon = new CompositeIcon(icon, OVL_SCHEDULED);
        }

        //If LGS observation
        if (obs.getLGS()) {
            icon = new CompositeIcon(icon, OVL_LGS);
        }

        return icon;
    }

    public static Color getColor(Set<Flag> flags) {
        return isGray(flags) ? Color.GRAY :
                isDarkGray(flags) ? Color.DARK_GRAY : Color.BLACK;
    }

    private static boolean isGray(Set<Flag> flags) {
        for (Flag f : GRAY) {
            if (flags.contains(f)) return true;
        }
        return isCrossedOut(flags);
    }

    private static boolean isDarkGray(Set<Flag> flags) {
        if (isGray(flags)) return false;
        for (Flag f : DARK_GRAY) {
            if (flags.contains(f)) return true;
        }
        return false;
    }

    private static boolean isCrossedOut(Set<Flag> flags) {
        for (Flag f : CROSSED_OUT) {
            if (flags.contains(f)) return true;
        }
        return false;
    }

}
