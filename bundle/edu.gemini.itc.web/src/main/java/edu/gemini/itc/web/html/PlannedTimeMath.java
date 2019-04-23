package edu.gemini.itc.web.html;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.guide.StandardGuideOptions;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;
import edu.gemini.spModel.obs.plannedtime.SetupTime;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;
import edu.gemini.spModel.time.ChargeClass;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// Extracted from edu.gemini.spModel.obs.plannedtime.PlannedTime since it
// doesn't seem useful elsewhere and indeed is only used by this package.

public final class PlannedTimeMath {

    public static final double VISIT_TIME = 7200; // visit time, sec
    public static final double RECENTERING_INTERVAL = 3600; // sec

    private PlannedTimeMath() {
    }

    // total science time (time without setup and re-centering)
    public static long scienceTime(PlannedTime pt) {
        long scienceTime = 0;
        for (PlannedTime.Step step : pt.steps) scienceTime += step.totalTime();
        return scienceTime / 1000;
    }

    private static int numReacq(double obsTime, double reacqInterval) {
        int numReacq = 0;
        if ((obsTime > reacqInterval) && (obsTime % reacqInterval == 0)) {
            numReacq = (int) (obsTime / reacqInterval) - 1;
        } else if ((obsTime > reacqInterval) && (obsTime % reacqInterval != 0)) {
            numReacq = (int) (obsTime / reacqInterval);
        }
        return numReacq;
    }

    // number of acquisitions (setups) per observation
    public static int numAcq(PlannedTime pt) {
        return numReacq(scienceTime(pt), VISIT_TIME) + 1;
    }

    // Number of re-centerings on the slit for spectroscopic observations using PWFS2
    public static int numRecenter(PlannedTime pt, Config config) {
        int numRecenter = 0;
        ItemKey guideWithPWFS2 = new ItemKey("telescope:guideWithPWFS2");

        if (config.containsItem(guideWithPWFS2) &&
                config.getItemValue(guideWithPWFS2).equals(StandardGuideOptions.Value.guide)) {
            long scienceTime = scienceTime(pt);
            double visitTime = (numAcq(pt) == 1) ? scienceTime : VISIT_TIME;
            double lastVisitTime = scienceTime % VISIT_TIME;
            // number of re-centerings per visit
            int visitRecenteringNum = numReacq(visitTime, RECENTERING_INTERVAL);
            // number of re-centerings in the last visit
            int lastVisitRecenteringNum = numReacq(lastVisitTime, RECENTERING_INTERVAL);

            if (VISIT_TIME > RECENTERING_INTERVAL) {
                numRecenter = (numAcq(pt) - 1) * visitRecenteringNum + lastVisitRecenteringNum;
            } else {
                throw new Error("Visit time is smaller than re-centering time");
            }
        }
        return numRecenter;
    }

    // total time with acquisitions and re-acquisitions.
    // Uses the parameter because of GSAOI LGS re-acquisitions.
    public static long totalTimeWithReacq(PlannedTime pt, int numReacq) {
        long totalTimeWithReacq =
                pt.setup.time.fullSetupTime.toMillis() * numAcq(pt) +
                pt.setup.time.reacquisitionOnlyTime.toMillis() * numReacq;
        for (PlannedTime.Step step : pt.steps) totalTimeWithReacq += step.totalTime();
        return totalTimeWithReacq;
    }


    // Extracted from PlannedTimeCalculator since it is only used by the ITC.

    public static PlannedTime calc(Config[] conf, ItcOverheadProvider instr)  {
        ChargeClass obsChargeClass = ChargeClass.PROGRAM;

        // add the setup time for the instrument
        final SetupTime setupTime;
        if ((instr == null) || (conf.length == 0)) {
            setupTime = PlannedTimeCalculator.DEFAULT_SETUP;
        } else {
            final Duration s = instr.getSetupTime(conf[0]);
            final Duration r = instr.getReacquisitionTime(conf[0]);
            setupTime = SetupTime.fromDuration(s, r, SetupTime.Type.FULL)
                                 .getOrElse(PlannedTimeCalculator.DEFAULT_SETUP);
        }
        final PlannedTime.Setup setup = PlannedTime.Setup.apply(setupTime, obsChargeClass);

        // Calculate the overhead time
        Option<Config> prev = None.instance();
        List<PlannedTime.Step> steps = new ArrayList<>();
        ConfigSequence cs = new ConfigSequence(conf);
        for (Config c : cs.getAllSteps()) {
            PlannedTime.CategorizedTimeGroup gtc    = instr.calc(c, prev);
            prev = new Some<>(c);

            steps.add(PlannedTime.Step.apply(gtc));
        }

        return PlannedTime.apply(setup, steps, cs);
    }


}
