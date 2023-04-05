// Copyright 1999-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: java 18053 2009-02-20 20:16:23Z swalker $
//

package edu.gemini.pot.sp;

import java.io.Serializable;
import java.util.NoSuchElementException;

import static edu.gemini.pot.sp.SPComponentBroadType.*;

/**
 * Describes a type of observation or sequence component.  Used to differentiate between two or more components in scope.
 */
// N.B. none of these strings should be changed; they are used in PIO and exist in archival programs
public enum SPComponentType implements Serializable {

    AO_ALTAIR(AO, "Altair", "Altair Adaptive Optics"),
    AO_GEMS(AO, "GeMS", "GeMS Adaptive Optics"),

    CONFLICT_FOLDER(CONFLICT, "Folder", "Conflict Folder"),

    DATA_DATAONLY(DATA, "dataOnly", "Hidden Data"),

    ENG_ENGNIFS(ENGINEERING, "EngNIFS", "NIFS Engineering"),
    ENG_ENGTRECS(ENGINEERING, "EngTReCS", "TReCS Engineering"),

    GROUP_GROUP(GROUP, "group", "Group"),

    INFO_NOTE(INFO, "note", "Note"),
    INFO_PROGRAMNOTE(INFO, "programNote", "Planning Note"),
    INFO_SCHEDNOTE(INFO, "schedNote", "Scheduling Note"),

    INSTRUMENT_ACQCAM(INSTRUMENT, "AcqCam", "Acquisition Camera"),
    INSTRUMENT_BHROS(INSTRUMENT, "BHROS", "bHROS"),
    INSTRUMENT_FLAMINGOS2(INSTRUMENT, "Flamingos2", "Flamingos2"),
    INSTRUMENT_GHOST(INSTRUMENT, "GHOST", "GHOST"),
    INSTRUMENT_GMOS(INSTRUMENT, "GMOS", "GMOS-N"),
    INSTRUMENT_GMOSSOUTH(INSTRUMENT, "GMOSSouth", "GMOS-S"),
    INSTRUMENT_GNIRS(INSTRUMENT, "GNIRS", "GNIRS"),
    INSTRUMENT_GPI(INSTRUMENT, "GPI", "GPI"),
    INSTRUMENT_GSAOI(INSTRUMENT, "GSAOI", "GSAOI"),
    INSTRUMENT_IGRINS2(INSTRUMENT, "IGRINS2", "IGRINS2"),
    INSTRUMENT_MICHELLE(INSTRUMENT, "Michelle", "Michelle"),
    INSTRUMENT_NICI(INSTRUMENT, "NICI", "NICI"),
    INSTRUMENT_NIFS(INSTRUMENT, "NIFS", "NIFS"),
    INSTRUMENT_NIRI(INSTRUMENT, "NIRI", "NIRI"),
    INSTRUMENT_PHOENIX(INSTRUMENT, "Phoenix", "Phoenix"),
    INSTRUMENT_TEXES(INSTRUMENT, "Texes", "Texes"),
    INSTRUMENT_TRECS(INSTRUMENT, "TReCS", "TReCS"),
    INSTRUMENT_VISITOR(INSTRUMENT, "Visitor", "Visitor Instrument"),

    ITERATOR_ACQCAM(ITERATOR, "AcqCam", "AcqCam Sequence"),
    ITERATOR_BASE(ITERATOR, "base", "Sequence"),
    ITERATOR_BHROS(ITERATOR, "BHROS", "bHROS Sequence"),
    ITERATOR_CALUNIT(ITERATOR, "CalUnit", "Cal Unit Sequence"),
    ITERATOR_FLAMINGOS2(ITERATOR, "Flamingos2", "Flamingos2 Sequence"),
    ITERATOR_GHOST(ITERATOR, "GHOST", "GHOST Sequence"),
    ITERATOR_GMOS(ITERATOR, "GMOS", "GMOS-N Sequence"),
    ITERATOR_GMOSSOUTH(ITERATOR, "GMOSSouth", "GMOS-S Sequence"),
    ITERATOR_GNIRS(ITERATOR, "GNIRS", "GNIRS Sequence"),
    ITERATOR_GPI(ITERATOR, "GPI", "GPI Sequence"),
    ITERATOR_GPIOFFSET(ITERATOR, "gpioffset", "GPI IFS Offset"),
    ITERATOR_GPOL(ITERATOR, "gpol", "GPOL Sequence"),
    ITERATOR_GSAOI(ITERATOR, "GSAOI", "GSAOI Sequence"),
    ITERATOR_IGRINS2(ITERATOR, "IGRINS2", "IGRINS-2 Sequence"),
    ITERATOR_MICHELLE(ITERATOR, "Michelle", "Michelle Sequence"),
    ITERATOR_NICI(ITERATOR, "NICI", "NICI Sequence"),
    ITERATOR_NICIOFFSET(ITERATOR, "nicioffset", "NICI Offset"),
    ITERATOR_NIFS(ITERATOR, "NIFS", "NIFS Sequence"),
    ITERATOR_NIRI(ITERATOR, "NIRI", "NIRI Sequence"),
    ITERATOR_OFFSET(ITERATOR, "offset", "Offset"),
    ITERATOR_PHOENIX(ITERATOR, "Phoenix", "Phoenix Sequence"),
    ITERATOR_REPEAT(ITERATOR, "repeat", "Repeat"),
    ITERATOR_TRECS(ITERATOR, "TReCS", "TReCS Sequence"),

    OBSERVATION_BASIC(OBSERVATION, "basic", "Observation"),

    OBSERVER_BASELINEDAY(OBSERVER, "BaselineDay", "Day Baseline GCAL"),
    OBSERVER_BASELINENIGHT(OBSERVER, "BaselineNight", "Night Baseline GCAL"),
    OBSERVER_BIAS(OBSERVER, "bias", "Manual Bias"),
    OBSERVER_DARK(OBSERVER, "dark", "Manual Dark"),
    OBSERVER_GHOST_DARK(OBSERVER, "ghostDark", "Manual Ghost Dark"),
    OBSERVER_GEMFLAT(OBSERVER, "GemFlat", "Manual Flat/Arc"),
    OBSERVER_GHOST_GEMFLAT(OBSERVER, "ghostGemFlat", "Manual Ghost Flat/Arc"),
    OBSERVER_OBSERVE(OBSERVER, "observe", "Observe"),
    OBSERVER_SMARTARC(OBSERVER, "SmartArc", "Arc"),
    OBSERVER_SMARTFLAT(OBSERVER, "SmartFlat", "Flat"),

    OBS_EXEC_LOG(OBSLOG, "exec", "Observation Exec Log"),
    OBS_QA_LOG(OBSLOG, "qa", "Observing Log"),

    PLAN_BASICPLAN(PLAN, "basicPlan", "Default Plan/Observing Log"),

    PROGRAM_BASIC(PROGRAM, "basic", "Science Program"),

    SCHEDULING_CONDITIONS(SCHEDULING, "conditions", "Observing Conditions"),

    TELESCOPE_TARGETENV(TELESCOPE, "targetEnv", "Targets"),

    TEMPLATE_FOLDER(TEMPLATE, "Folder", "Template Folder"),
    TEMPLATE_GROUP(TEMPLATE, "Group", "Template Group"),
    TEMPLATE_PARAMETERS(TEMPLATE, "Parameters", "Template Parameters"),
    UNKNOWN(SPComponentBroadType.UNKNOWN, "unknown", "unknown"),

    // TODO: remove these
    // HACK: these are types used only by QPT that seem to have been introduced by misunderstanding.
    QPT_CANOPUS(INSTRUMENT, "Canopus", "Canopus"),
    QPT_PWFS(INSTRUMENT, "PWFS", "PWFS"),

            ;

    private static final String ANY_DESCRIPTION = "*";

    /** The general category that this component falls under. */
    public final SPComponentBroadType broadType;

    /** The specific instance of the general category to which this type corresponds. */
    public final String narrowType;

    /** The human readable String description of this type. */
    public final String readableStr;

    public static SPComponentType getInstance(String broad, String narrow, String readableStr) {
        return getInstance(SPComponentBroadType.getInstance(broad), narrow, readableStr);
    }

    public static SPComponentType getInstance(SPComponentBroadType broad, String narrow, String readableStr) {
        for (SPComponentType t: values())
                if (t.broadType.equals(broad) &&
                    t.narrowType.equals(narrow) &&
                    (t.readableStr.equals(readableStr) || ANY_DESCRIPTION.equals(readableStr)))
                    return t;
        throw new NoSuchElementException(toString(broad, narrow, readableStr));
    }

    public static SPComponentType getInstance(String broad, String narrow) {
        return getInstance(SPComponentBroadType.getInstance(broad), narrow);
    }

    public static SPComponentType getInstance(SPComponentBroadType broad, String narrow) {
        return getInstance(broad, narrow, ANY_DESCRIPTION);
    }

    private SPComponentType(SPComponentBroadType broadType, String narrowType, String readableStr) {
        if (broadType == null || narrowType == null || readableStr == null)
            throw new IllegalArgumentException("All arguments must be non-null.");
        this.broadType = broadType;
        this.narrowType = narrowType;
        this.readableStr = readableStr;
    }


//    @Override public String toString() {
//        return name() + ":" + toString(broadType, narrowType, readableStr);
//    }

    private static String toString(SPComponentBroadType broadType, String narrowType, String readableStr) {
        return String.format("%s(%s,%s,%s)",SPComponentType.class.getSimpleName(), broadType, narrowType, readableStr);
    }

    public static void main(String[] args) {
        System.out.println(OBSERVATION_BASIC);
    }

}
