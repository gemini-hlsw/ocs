package edu.gemini.spModel.template;

import edu.gemini.pot.sp.ISPTemplateFolder;
import edu.gemini.pot.sp.ISPTemplateGroup;
import edu.gemini.pot.sp.ISPTemplateParameters;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;
import java.util.*;

/**
 * The Phase 1 observations (i.e., TemplateParameters) grouped by blueprint id.
 */
public final class Phase1Group implements Serializable {
    public static final String PARAM_SET_NAME = "phase1Group";
    public static final String PARAM_BLUEPRINT = "blueprint";

    public final String blueprintId;
    public final List<TemplateParameters> argsList;

    public Phase1Group(String blueprintId, List<TemplateParameters> args) {
        this.blueprintId = blueprintId;
        this.argsList    = Collections.unmodifiableList(new ArrayList<TemplateParameters>(args));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phase1Group that = (Phase1Group) o;
        if (!argsList.equals(that.argsList)) return false;
        return blueprintId.equals(that.blueprintId);
    }

    @Override
    public int hashCode() {
        int result = blueprintId.hashCode();
        result = 31 * result + argsList.hashCode();
        return result;
    }

    public ParamSet getParamSet(PioFactory factory) {
        final ParamSet ps = factory.createParamSet(PARAM_SET_NAME);
        Pio.addParam(factory, ps, PARAM_BLUEPRINT, blueprintId);
        for (TemplateParameters a : argsList)
            ps.addParamSet(a.getParamSet(factory));
        return ps;
    }

    public static Phase1Group fromParamSet(ParamSet paramSet) {
        final String blueprint = Pio.getValue(paramSet, PARAM_BLUEPRINT);
        final List<TemplateParameters> args = new ArrayList<TemplateParameters>();
        for (ParamSet ps : paramSet.getParamSets(TemplateParameters.SP_TYPE.readableStr)) {
            args.add(new TemplateParameters(ps));
        }
        return new Phase1Group(blueprint, args);
    }

    private static Map<String, List<List<TemplateParameters>>> discover(ISPTemplateFolder folder) {
        final Map<String, List<List<TemplateParameters>>> pigMap = new HashMap<String, List<List<TemplateParameters>>>();

        for (ISPTemplateGroup tg : folder.getTemplateGroups()) {
            final List<TemplateParameters> args = new ArrayList<TemplateParameters>();
            for (ISPTemplateParameters tp : tg.getTemplateParameters()) {
                args.add((TemplateParameters) tp.getDataObject());
            }

            final String blueId = ((TemplateGroup) tg.getDataObject()).getBlueprintId();
            List<List<TemplateParameters>> groupedArgs = pigMap.get(blueId);
            if (groupedArgs == null) {
                groupedArgs = new ArrayList<List<TemplateParameters>>();
                pigMap.put(blueId, groupedArgs);
            }
            groupedArgs.add(args);
        }

        return pigMap;
    }

    /**
     * Combines the Phase1Groups with the same blueprint id into single
     * groups.  The order in which the groups are returned matches the order
     * of their first appearance in the argument.
     */
    public static List<Phase1Group> extract(ISPTemplateFolder folder) {
        final Map<String, List<List<TemplateParameters>>> pigMap = discover(folder);
        final Map<String, List<TemplateParameters>> combinedPigMap = new HashMap<String, List<TemplateParameters>>();

        // Combine partitioned args into one big argument list.
        for (Map.Entry<String, List<List<TemplateParameters>>> me : pigMap.entrySet()) {
            final List<TemplateParameters> combined = new ArrayList<TemplateParameters>();
            for (List<TemplateParameters> cur : me.getValue()) {
                combined.addAll(cur);
            }
            combinedPigMap.put(me.getKey(), combined);
        }

        // Create combined Phase1Groups in the order in which template groups
        // appear in the folder.
        final List<Phase1Group> pigs = new ArrayList<Phase1Group>();
        for (ISPTemplateGroup tg : folder.getTemplateGroups()) {
            final String id = ((TemplateGroup) tg.getDataObject()).getBlueprintId();
            final List<TemplateParameters> args = combinedPigMap.get(id);
            if (args != null) {
                pigs.add(new Phase1Group(id, args));
                combinedPigMap.remove(id);
            }
        }
        return pigs;
    }
}