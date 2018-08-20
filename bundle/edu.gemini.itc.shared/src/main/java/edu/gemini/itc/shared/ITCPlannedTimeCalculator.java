package edu.gemini.itc.shared;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.time.ChargeClass;

import java.util.ArrayList;
import java.util.List;

final public class ITCPlannedTimeCalculator {

    // Not meant to be instantiated.
    private ITCPlannedTimeCalculator() {
    }

    public static PlannedTime calc(Config[] conf, PlannedTime.ItcOverheadProvider instr)  {
        ChargeClass obsChargeClass = ChargeClass.PROGRAM;

        // add the setup time for the instrument
        final double setupTime;
        final double reacqTime;
        if (instr == null) {
            setupTime = 15 * 60;
            reacqTime = 0;
        } else {
            setupTime = instr.getSetupTime(conf[0]);
            reacqTime = instr.getReacquisitionTime();
        }
        PlannedTime.Setup setup = PlannedTime.Setup.fromSeconds(setupTime, reacqTime, obsChargeClass);

        // Calculate the overhead time
        Option<Config> prev = None.instance();
        final List<PlannedTime.Step> steps = new ArrayList<>();
        final ConfigSequence cs = new ConfigSequence(conf);
        for (final Config c : cs.getAllSteps()) {
            final PlannedTime.CategorizedTimeGroup gtc = instr.calc(c, prev);
            prev = new Some<>(c);

            steps.add(PlannedTime.Step.apply(gtc));
        }

        return PlannedTime.apply(setup, steps, cs);
    }
}
