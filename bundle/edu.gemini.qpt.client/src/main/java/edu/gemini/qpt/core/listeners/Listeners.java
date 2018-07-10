package edu.gemini.qpt.core.listeners;

import java.util.Arrays;
import java.util.List;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;

/**
 * Listener registry (hardcoded right now).
 * @author rnorris
 */
@SuppressWarnings("unchecked")
public class Listeners {

    private static final List<MarkerModelListener<Schedule>> SCHEDULE_LISTENERS = Arrays.asList(
        new EmptyScheduleListener(),
        new EmptyIctdListener()
    );

    private static final List<MarkerModelListener<Variant>> VARIANT_LISTENERS = Arrays.asList(
        new EmptyVariantListener(),
        new LimitsListener(),
//        new OutsideBlockListener(),
        new OverlappingAllocListener(),
        new SetupListener(),
        new OverAllocationListener(),
        new AzimuthListener(),
        new TruncatedAllocListener(),
        new TargetEnvironmentListener(),
        new ParallacticAngleListener(),
        new AgsAnalysisListener(),
        new PropagationWindowsListener()
    );


    public static void attach(Schedule schedule) {
        for (ModelListener<Schedule> o: SCHEDULE_LISTENERS)
            o.subscribe(schedule);
    }

    public static void attach(Variant variant) {
        for (ModelListener<Variant> o: VARIANT_LISTENERS)
            o.subscribe(variant);
    }

    public static void detach(Schedule schedule) {
        for (ModelListener<Schedule> o: SCHEDULE_LISTENERS)
            o.unsubscribe(schedule);
    }

    public static void detach(Variant variant) {
        for (ModelListener<Variant> o: VARIANT_LISTENERS)
            o.unsubscribe(variant);
    }

}
