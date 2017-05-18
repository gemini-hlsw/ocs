package edu.gemini.spdb.reports.collection.util;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.shared.util.immutable.Tuple2;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gmos.*;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.ObsTimesService;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.ProgramNote;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.template.TemplateParameters;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.gemini.pot.sp.SPComponentBroadType.AO;
import static edu.gemini.pot.sp.SPComponentBroadType.INSTRUMENT;

@SuppressWarnings("unchecked")
public class ReportUtils {

    static final Pattern SEMESTER_PATTERN = Pattern.compile("^G[NS]-(\\d{4}+[AB])-.*", Pattern.CASE_INSENSITIVE);

	/**
	 * Return the text of the passed ISPProgram's planning note, or null
	 * if no such planning note exists.
	 */
	public static String getPlanningNoteTitle(ISPProgram progShell)  {
		for (Object o: progShell.getChildren()) {
			if (o instanceof ISPObsComponent && ((ISPObsComponent) o).getType() == ProgramNote.SP_TYPE) {
				ProgramNote note = (ProgramNote) ((ISPObsComponent) o).getDataObject();
                // SCT-286 (#3): now they only want to see the title, not the whole
                // note text
                return note.getEditableTitle();
            }
        }
        return null;
    }

    /**
     * Returns the semester from the given SPProgramID, or null if none could be determined.
     */
    public static String getSemester(SPProgramID id) {
        Matcher m = SEMESTER_PATTERN.matcher(id.toString());
        return m.matches() ? m.group(1) : null;
    }


	public static long getChargedTime(ISPObservation obsShell)  {
//		final ObsClass obsClass = ObsClassService.lookupObsClass(obsShell); // never null
//		final ObsRecord obsRec = SPTreeUtil.getObsRecord(obsShell);
//		if (obsRec == null) return 0;
//		final ChargeClass defaultChargeClass = obsClass.getDefaultChargeClass();
//		return obsRec.getTimeCharges(defaultChargeClass).getTime(ChargeClass.PROGRAM);
        return ObsTimesService.getCorrectedObsTimes(obsShell).getTimeCharges().getTime(ChargeClass.PROGRAM);
    }

	public static boolean isRollover(ISPProgram programShell)  {
		try {
            return ((SPProgram) programShell.getDataObject()).getRolloverStatus();
//			return P1DocumentUtil.getGeminiPart(P1DocumentUtil.lookupProposal(programShell)).getITacExtension().getRolloverFlag();
        } catch (NullPointerException npe) {
            // If the required structure isn't there, rollover is false. This
            // condition would be unusual but is legal. Easier to catch NPE than
            // check all the null conditions.
        }
        return false;
    }

    public static long getBand3Minimum(ISPProgram programShell)  {
        TimeValue tv = ((SPProgram) programShell.getDataObject()).getMinimumTime();
        if (tv == null) return 0;
        return (long) (tv.convertTimeAmountTo(TimeValue.Units.seconds) * 1000);
    }

    public static String getExecutionStatus(final ISPProgram progShell, final SPProgram prog, boolean useActiveFlag)  {

        // Build up the execution status.
        List<String> statusFlags = new ArrayList<String>();

        // Complete and Active
        if (prog.isCompleted()) {
            statusFlags.add("complete");
        /*
        } else {

            REL-803.  Removing "active" flag as requested but leaving the code
            for when we are asked to replace it.

            if (useActiveFlag) {

                // Science guys want to use this for scheduling, so we don't want
                // the user to see it.
                if (prog.isActive()) statusFlags.add("active");

            } else {

                // Active flag means not complete, plus at least one obs that is
                // ready, ongoing, or on hold.
                for (ISPObservation obsShell : progShell.getAllObservations()) {
                    final ObservationStatus os = ObservationStatus.computeFor(obsShell);
                    if (ObservationStatus.READY.equals(os) ||
                            ObservationStatus.ONGOING.equals(os) ||
                            ObservationStatus.ON_HOLD.equals(os)) {

                        statusFlags.add("active");
                        break;

                    }
                }
            }
        */

        }

        // Rollover
        if (isRollover(progShell)) statusFlags.add("rollover");

        // SCT-286 (#7): get the TOO status of the program.
        TooType type = Too.get(progShell);
        if (type != null) {
            switch (type) {
                case rapid:
                    statusFlags.add("RToO");
                    break;
                case standard:
                    statusFlags.add("SToO");
                    break;
            }
        }

        // Done with status flags. Build a string.
        StringBuilder statusBuf = new StringBuilder();
        for (String s : statusFlags) {
            if (statusBuf.length() > 0)
                statusBuf.append(" / ");
            statusBuf.append(s);
        }
        return statusBuf.toString();

    }

	public static String getScienceInstruments(ISPProgram progShell)  {
        Set<String> instruments = new TreeSet<String>();

        for (ISPObservation obsShell : getObservationsIncludingTemplates(progShell)) {
            // SCT-286 (#4): We only care about science observations.
            ObsClass obsClass = ObsClassService.lookupObsClass(obsShell);
            if (ObsClass.SCIENCE != obsClass) continue;

            // For each obs, we can have an inst and an AO and a LGS
            String inst = null;
            String ao = null;

            // Need to look through all the components.
            for (ISPObsComponent comp : obsShell.getObsComponents()) {
                SPComponentType type = comp.getType();

                // There will be zero or one.
                if (AO == type.broadType) {
                    // RS-28
                    if (InstAltair.SP_TYPE == type) {
                        ao = "Altair";

                        // SCT-286 (#5): if a LGS is required, add that to the
                        // string
                        InstAltair alt = (InstAltair) comp.getDataObject();
                        if (alt.getGuideStarType() == AltairParams.GuideStarType.LGS) {
                            ao += "LGS";
                        }
                    } else {
                        ao = type.readableStr;
                    }
                    if (inst != null) break;
                    continue;
                }

                // There will be zero or one.
                if (INSTRUMENT == type.broadType) {
                    // SW: changing this to pick the shorter of readable and
                    // narrow type. "Acquisition Camera" is just way too long.
                    // Sorry about this hack ...
                    String val = type.readableStr;
                    String narrow = type.narrowType;
                    if (narrow.length() < val.length()) val = narrow;
                    inst = val;
                    if (ao != null) break;
                }

            }

            // Inst string is like "NIRI+Altair" or "GMOS"
            if (inst != null) {
                if (ao == null) {
                    instruments.add(inst);
                } else {
                    instruments.add(inst + "+" + ao);
                }
            }

        }

        // Done with status flags. Build a string.
        StringBuilder instrumentsBuf = new StringBuilder();
        for (String s : instruments) {
            if (instrumentsBuf.length() > 0)
                instrumentsBuf.append(" / ");
            instrumentsBuf.append(s);
        }
        return instrumentsBuf.toString();
    }

    // TODO: The following is crap to make the FPU show up in the report the
    // way they want it.  This is being released mid-semester so I'm reluctant
    // to change the model at all.  Ideally, this should just be returned by a
    // method on the FPU types in a future release. Say "String reportFpu()"
    private static final Map<GmosCommonType.FPUnit, String> FPU_MAP =
            new HashMap<GmosCommonType.FPUnit, String>();

    static {
        FPU_MAP.put(GmosNorthType.FPUnitNorth.IFU_1, "IFU-2");
        FPU_MAP.put(GmosNorthType.FPUnitNorth.IFU_2, "IFU-B");
        FPU_MAP.put(GmosNorthType.FPUnitNorth.IFU_3, "IFU-R");
        FPU_MAP.put(GmosNorthType.FPUnitNorth.CUSTOM_MASK, "MOS");

        FPU_MAP.put(GmosSouthType.FPUnitSouth.IFU_1, "IFU-2");
        FPU_MAP.put(GmosSouthType.FPUnitSouth.IFU_2, "IFU-B");
        FPU_MAP.put(GmosSouthType.FPUnitSouth.IFU_3, "IFU-R");
        FPU_MAP.put(GmosSouthType.FPUnitSouth.IFU_N, "IFU-N-2");
        FPU_MAP.put(GmosSouthType.FPUnitSouth.IFU_N_B, "IFU-N-B");
        FPU_MAP.put(GmosSouthType.FPUnitSouth.IFU_N_R, "IFU-N-R");
        FPU_MAP.put(GmosSouthType.FPUnitSouth.CUSTOM_MASK, "MOS");
    }

    static List<ISPObservation> getObservationsIncludingTemplates(ISPProgram progShell) {
        List<ISPObservation> observations = new ArrayList<ISPObservation>();
        observations.addAll(progShell.getAllObservations());
        ISPTemplateFolder folder = progShell.getTemplateFolder();
        if (folder != null) {
            for (ISPTemplateGroup group : folder.getTemplateGroups()) {
                observations.addAll(group.getAllObservations());
            }
        }
        return observations;
    }

    static List<Tuple2<SPSiteQuality, TimeValue>> getTemplateConditions(ISPProgram progShell) {
        final List<Tuple2<SPSiteQuality, TimeValue>> results = new ArrayList<Tuple2<SPSiteQuality, TimeValue>>();

        final ISPTemplateFolder folder = progShell.getTemplateFolder();
        TemplateParameters.foreach(folder, new ApplyOp<TemplateParameters>() {
            @Override public void apply(TemplateParameters tp) {
                results.add(new Pair(tp.getSiteQuality(), tp.getTime()));
            }
        });
        return results;
    }


    public static String getInstResources(ISPProgram progShell) {
        Set<String> dispersers = new TreeSet<String>();
        Set<String> fpus = new TreeSet<String>();

        // Look at each observation in the program in case one or more are
        // GMOS observations.
        for (ISPObservation obsShell : getObservationsIncludingTemplates(progShell)) {

            // Need to look through all the components to find the instrument.
            for (ISPObsComponent comp : obsShell.getObsComponents()) {
                SPComponentType type = comp.getType();

                if (INSTRUMENT == type.broadType) {
                    if (type == InstGmosSouth.SP_TYPE || type == InstGmosNorth.SP_TYPE) {
                        InstGmosCommon gmos = (InstGmosCommon) comp.getDataObject();
                        GmosCommonType.Disperser disp = (GmosCommonType.Disperser) gmos.getDisperser();
                        if (!disp.isMirror()) {
                            dispersers.add(disp.logValue());
                        }
                        GmosCommonType.FPUnit fpu = (GmosCommonType.FPUnit) gmos.getFPUnit();
                        String val = FPU_MAP.get(fpu);
                        if (val != null) fpus.add(val);
                    }
                    break;
                }
            }
        }

        // Done with finding GMOS resources.  Build up a string to return.
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (String s : dispersers) {
            if (!first) buf.append("/");
            buf.append(s);
            first = false;
        }
        if (fpus.size() > 0) {
            first = true;
            buf.append("  ");
        }
        for (String s : fpus) {
            if (!first) buf.append("/");
            buf.append(s);
            first = false;
        }
        return buf.toString();
    }

    public static String getRaRange(ISPProgram progShell)  {
        // Get the set of distinct, sorted hours used by this program
        Integer[] hours = getRaHoursIncludingTemplates(progShell);

        // If there are no hours or only one, just return it.
        switch (hours.length) {
            case 0:
                return "";
            case 1:
                return String.valueOf(hours[0]);
        }

        // Find the biggest gap between hours.  Initialize with the gap between
        // the last and first element.
        int maxGap = (hours[0] + 24) - hours[hours.length - 1];
        int gapIndex = 0;

        int prev = hours[0];
        for (int i = 1; i < hours.length; ++i) {
            int cur = hours[i];
            int curGap = cur - prev;
            if (curGap > maxGap) {
                maxGap = curGap;
                gapIndex = i;
            }
            prev = cur;
        }

        // Report the range, leaving out the largest gap.
        if (gapIndex == 0) {
            return String.format("%d-%d", hours[0], hours[hours.length - 1]);
        }
        return String.format("%d-%d", hours[gapIndex], hours[gapIndex - 1]);
    }

    private static Set<Integer> getRaHours(final ISPProgram progShell)  {
        final Set<Integer> hourSet = new TreeSet<>();
        for (ISPObservation obsShell : progShell.getAllObservations()) {
            final ObsClass obsClass = ObsClassService.lookupObsClass(obsShell);
            if (ObsClass.SCIENCE != obsClass) continue;

            // Need to look through all the components to find the target env.
            ISPObsComponent targetEnvComp = null;
            for (ISPObsComponent comp : obsShell.getObsComponents()) {
                if (comp.getType() == TargetObsComp.SP_TYPE) {
                    targetEnvComp = comp;
                    break;
                }
            }
            if (targetEnvComp == null) continue;

            // Figure out the RA of the base position
            final TargetObsComp targetEnv = (TargetObsComp) targetEnvComp.getDataObject();
            final Asterism asterism = targetEnv.getAsterism();
            final Option<Long> when = ((SPObservation) obsShell.getDataObject()).getSchedulingBlockStart();
            final Option<Integer> raHours = getRaHours(asterism, when);
            raHours.forEach(h -> hourSet.add(h));
        }
        return hourSet;

    }

    private static Set<Integer> getTemplateRaHours(final ISPProgram progShell) {
        final Set<Integer> hourSet = new TreeSet<>();
        final ISPTemplateFolder folder = progShell.getTemplateFolder();
        TemplateParameters.foreach(folder, new ApplyOp<TemplateParameters>() {
            @Override
            public void apply(final TemplateParameters tp) {
                final Option<Integer> raHours = getRaHours(tp.getAsterism(), None.instance());
                raHours.foreach(h -> hourSet.add(h));
            }
        });
        return hourSet;
    }

    private static Integer[] getRaHoursIncludingTemplates(ISPProgram progShell) {
        Set<Integer> hourSet = getRaHours(progShell);
        hourSet.addAll(getTemplateRaHours(progShell));

        Integer[] hourArray = hourSet.toArray(new Integer[hourSet.size()]);
        Arrays.sort(hourArray);

        return hourArray;
    }

    /** Gets the ra hours value - if the target is not null, has RA/DEC coordinates and is not a "dummy" target. */
    private static Option<Integer> getRaHours(final Asterism target, Option<Long> when) {
        if (target == null) {
            return None.instance();
        }

        return
            target.getRaDegrees(when).flatMap(r ->
            target.getDecDegrees(when).flatMap(d ->
                (r == 0.0 && d == 0.0) ?  None.instance() : new Some<>(((int) Math.round(r / 15.0)) % 24)
            ));
    }

    public static Site getSiteDesc(SPProgramID id) {
        return id.site();
    }

    public static String semester(String utc) {

        // Semesters go from [Feb .. Jul] and [Aug .. Jan].
        int year = Integer.parseInt(utc.substring(0, 4));
        int month = Integer.parseInt(utc.substring(4, 6));

        switch (month) {
            case 1:
                return (year - 1) + "B";
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                return year + "A";
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
                return year + "B";
            default:
                throw new IllegalArgumentException("Impossible.");
        }


    }


}
