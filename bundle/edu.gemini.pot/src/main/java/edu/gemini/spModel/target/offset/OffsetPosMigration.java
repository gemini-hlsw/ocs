package edu.gemini.spModel.target.offset;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.DefaultGuideOptions;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideOptions;
import edu.gemini.spModel.guide.GuideProbe;

import java.util.*;

final class OffsetPosMigration {

    static <P extends OffsetPosBase> void apply(List<P> positions) {
        final Map<GuideProbe, List<DefaultGuideOptions.Value>> map = collectDefaults(invert(positions));

        // If the map is empty, nothing can be set from the default guide
        // options.  All the probe links (if any) have to be considered
        // advanced guiding.
        if (map.size() == 0) return;

        // Pick the first guide probe in the map and use it as the default
        // values.
        final GuideProbe master = map.keySet().iterator().next();
        List<DefaultGuideOptions.Value> defValues = map.remove(master);
        filterMatching(map, defValues);
        map.put(master, defValues);
        final Set<GuideProbe> defaultProbes = map.keySet();

        // Now update the positions, setting the default values and removing
        // any guide probes whose values match the defaults.
        for (int i=0; i<positions.size(); ++i) {
            P pos = positions.get(i);
            DefaultGuideOptions.Value def = defValues.get(i);
            pos.setDefaultGuideOption(def);
            for (GuideProbe gp : defaultProbes) pos.removeLink(gp);
        }
    }



    // Essentially a function List<Map<GuideProbe, GuideOption>> => Map<GuideProbe, List<GuideOption>>
    private static <P extends OffsetPosBase> Map<GuideProbe, List<GuideOption>> invert(List<P> positions) {
        if (positions.size() == 0) return Collections.EMPTY_MAP;

        // Use the first position to know what GuideProbes we care about.
        final Set<GuideProbe> keys = positions.get(0).getLinks().keySet();

        // Initialize the map with empty GuideOption lists.
        final Map<GuideProbe, List<GuideOption>> res = makeProbeMap();
        for (GuideProbe key : keys) res.put(key, new ArrayList<GuideOption>());

        for (P pos : positions) {
            final Map<GuideProbe, GuideOption> links = pos.getLinks();
            for (GuideProbe key : keys) {
                GuideOption option = links.get(key);
                // Shouldn't be null but you never know with this code base
                if (option == null) option = key.getGuideOptions().getDefaultActive();
                res.get(key).add(option);
            }
        }
        return res;
    }


    // Collect only the guide probes for which the guide options can be
    // converted into default guiding options (on / off).  Park, for example,
    // cannot be accommodated.  Any list that cannot be converted is removed
    // from the results.
    private static Map<GuideProbe, List<DefaultGuideOptions.Value>> collectDefaults(Map<GuideProbe, List<GuideOption>> map) {
        final Map<GuideProbe, List<DefaultGuideOptions.Value>> res = makeProbeMap();

        nextProbe: for (Map.Entry<GuideProbe, List<GuideOption>> me : map.entrySet()) {
            final GuideProbe        guideProbe      = me.getKey();
            final List<GuideOption> guideOptionList = me.getValue();
            final GuideOptions      guideOptions    = guideProbe.getGuideOptions();

            final List<DefaultGuideOptions.Value> defOptions = new ArrayList<DefaultGuideOptions.Value>(guideOptionList.size());
            for (GuideOption guideOption : guideOptionList) {
                if (guideOptions.getDefaultActive() == guideOption) {
                    defOptions.add(DefaultGuideOptions.Value.on);
                } else if (guideOptions.getDefaultInactive() == guideOption) {
                    defOptions.add(DefaultGuideOptions.Value.off);
                } else {
                    // skip this probe, leaving it out of the result map
                    continue nextProbe;
                }
            }
            res.put(guideProbe, defOptions);
        }
        return res;
    }


    // Filter the Map<GuideProbe, List<DefaultGuideOptions.Value>> keeping only
    // those whose List of default options match the given list.  The ones we
    // remove require advanced guiding because they don't match the default
    // setting at each position.
    private static void filterMatching(Map<GuideProbe, List<DefaultGuideOptions.Value>> map, List<DefaultGuideOptions.Value> lst) {
        final Set<GuideProbe> rmSet = new TreeSet<GuideProbe>(GuideProbe.KeyComparator.instance);

        for (Map.Entry<GuideProbe, List<DefaultGuideOptions.Value>> me : map.entrySet()) {
            if (!lst.equals(me.getValue())) rmSet.add(me.getKey());
        }
        for (GuideProbe key : rmSet) map.remove(key);
    }

    private static <V> SortedMap<GuideProbe, V> makeProbeMap() {
        return new TreeMap<GuideProbe, V>(GuideProbe.KeyComparator.instance);
    }
}
