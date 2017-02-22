package edu.gemini.spModel.template;

import edu.gemini.pot.sp.ISPTemplateFolder;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The original Phase 1 blueprints with their grouped observations.
 */
public final class Phase1Folder implements Serializable {
    public static final String PARAM_SET_NAME = "Phase1Folder";
    public static final String BLUEPRINT_ENTRY_PARAM_SET = "BlueprintEntry";
    public static final String BLUEPRINT_ID_PARAM = "BlueprintId";

    private static final Logger LOG = Logger.getLogger(Phase1Folder.class.getName());

    public final Map<String, SpBlueprint> blueprintMap;
    public final List<Phase1Group> groups;

    public Phase1Folder(Map<String, SpBlueprint> blueprintMap, List<Phase1Group> groups) {
        this.blueprintMap = Collections.unmodifiableMap(new HashMap<>(blueprintMap));
        this.groups       = Collections.unmodifiableList(new ArrayList<>(groups));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Phase1Folder that = (Phase1Folder) o;
        if (!blueprintMap.equals(that.blueprintMap)) return false;
        return groups.equals(that.groups);
    }

    @Override
    public int hashCode() {
        int result = blueprintMap.hashCode();
        result = 31 * result + groups.hashCode();
        return result;
    }

    public ParamSet getParamSet(PioFactory factory) {
        final ParamSet ps = factory.createParamSet(PARAM_SET_NAME);

        // Add all id -> blueprint entries
        for (Map.Entry<String, SpBlueprint> me : blueprintMap.entrySet()) {
            final ParamSet bpPs = factory.createParamSet(BLUEPRINT_ENTRY_PARAM_SET);

            Pio.addParam(factory, bpPs, BLUEPRINT_ID_PARAM, me.getKey());
            bpPs.addParamSet(me.getValue().toParamSet(factory));
            ps.addParamSet(bpPs);
        }

        // Add all pigs
        groups.forEach(pig -> ps.addParamSet(pig.getParamSet(factory)));

        return ps;
    }

    public static Phase1Folder fromParamSet(ParamSet paramSet) {
        final Map<String, SpBlueprint> blueprintMap = new HashMap<>();

        final List<ParamSet> bpEntries = paramSet.getParamSets(BLUEPRINT_ENTRY_PARAM_SET);
        for (ParamSet bpEntry : bpEntries) {
            final String id = Pio.getValue(bpEntry, BLUEPRINT_ID_PARAM);
            final List<ParamSet> psList = bpEntry.getParamSets();
            if ((psList.size() != 1) || !SpBlueprintFactory.isSpBlueprintParamSet(psList.get(0))) {
                final String msg;
                final String prefix = "Expecting a single ParamSet containing an SpBlueprint";
                if (psList.size() != 1) {
                    msg = String.format("%s but found %d ParamSets", prefix, psList.size());
                } else {
                    msg = String.format("%s but couldn't find the matching constructor for %s", prefix, psList.get(0).getName());
                }
                LOG.warning(msg);
                throw new RuntimeException(msg);
            }
            blueprintMap.put(id, SpBlueprintFactory.fromParamSet(psList.get(0)));
        }

        final List<Phase1Group> pigs = paramSet.getParamSets(Phase1Group.PARAM_SET_NAME)
                .stream()
                .map(Phase1Group::fromParamSet)
                .collect(Collectors.toList());
        return new Phase1Folder(blueprintMap, pigs);
    }

    public static Phase1Folder extract(ISPTemplateFolder folder) {
        final TemplateFolder tf = (TemplateFolder) folder.getDataObject();
        return new Phase1Folder(tf.getBlueprints(), Phase1Group.extract(folder));
    }
}
