package edu.gemini.spModel.template;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.template.TemplateGroup.Args;

import java.io.Serializable;
import java.util.*;

public final class TemplateFolder extends AbstractDataObject {

    public static final String DEFAULT_TITLE = "Templates";
    public static final String VERSION = "2014A-1";
    public static final SPComponentType SP_TYPE = SPComponentType.TEMPLATE_FOLDER;

    public static final String PARAM_MAP_KEY = "templateFolderMapKey";

    /**
     * The original Phase 1 observations grouped by blueprint.  This is used
     * to (re)generate a program's TemplateGroups.
     */
    public static final class Phase1Group implements Serializable {
        public static final String PARAM_SET_NAME  = "phase1Group";
        public static final String PARAM_BLUEPRINT = "blueprint";

        public final String blueprintId;
        public final List<Args> argsList;

        public Phase1Group(String blueprintId, List<Args> args) {
            this.blueprintId = blueprintId;
            this.argsList = Collections.unmodifiableList(new ArrayList<Args>(args));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Phase1Group that = (Phase1Group) o;
            if (!argsList.equals(that.argsList)) return false;
            if (!blueprintId.equals(that.blueprintId)) return false;
            return true;
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
            for (Args a : argsList) ps.addParamSet(a.getParamSet(factory));
            return ps;
        }

        public static Phase1Group fromParamSet(ParamSet paramSet) {
            String blueprint = Pio.getValue(paramSet, PARAM_BLUEPRINT);
            List<Args> args = new ArrayList<Args>();
            for (ParamSet ps : paramSet.getParamSets(Args.PARAM_SET_NAME)) {
                args.add(new Args(ps));
            }
            return new Phase1Group(blueprint, args);
        }
    }

    // The template folder contains the master list of blueprints, targets, and siteQualities
    // derived from the Phase I document. These are referenced by key in each template group.
    private Map<String, SpBlueprint> blueprints;
    private Map<String, SPTarget> targets = new TreeMap<String, SPTarget>();
    private Map<String, SPSiteQuality> siteQualities = new TreeMap<String, SPSiteQuality>();

    private List<Phase1Group> groups = new ArrayList<Phase1Group>();

    public TemplateFolder() {
        setTitle(DEFAULT_TITLE);
        setType(SP_TYPE);
        setVersion(VERSION);
        blueprints    = Collections.emptyMap();
        targets       = Collections.emptyMap();
        siteQualities = Collections.emptyMap();
        groups        = Collections.emptyList();
    }

    public TemplateFolder(
            Map<String, SpBlueprint> blueprints,
            Map<String, SPTarget> targets,
            Map<String, SPSiteQuality> siteQualities,
            List<Phase1Group> groups) {
        setTitle(DEFAULT_TITLE);
        setType(SP_TYPE);
        setVersion(VERSION);
        this.blueprints    = Collections.unmodifiableMap(new TreeMap<String, SpBlueprint>(blueprints));
        this.targets       = Collections.unmodifiableMap(new TreeMap<String, SPTarget>(targets));
        this.siteQualities = Collections.unmodifiableMap(new TreeMap<String, SPSiteQuality>(siteQualities));
        this.groups        = Collections.unmodifiableList(new ArrayList<Phase1Group>(groups));
    }

    // All accessors return unmodifiable data structures.

    public Map<String, SpBlueprint> getBlueprints()      { return blueprints; }
    public Map<String, SPTarget> getTargets()            { return targets; }
    public Map<String, SPSiteQuality> getSiteQualities() { return siteQualities; }
    public List<Phase1Group> getGroups()                 { return groups; }


    @Override
    public ParamSet getParamSet(PioFactory factory) {
        final ParamSet ps = super.getParamSet(factory);

        // Amazingly (seriously) we can't do this polymorphically

        for (Map.Entry<String, SpBlueprint> e : blueprints.entrySet())
            addKeyValue(factory, ps, e.getKey(), e.getValue().toParamSet(factory));

        for (Map.Entry<String, SPTarget> e : targets.entrySet())
            addKeyValue(factory, ps, e.getKey(), e.getValue().getParamSet(factory));

        for (Map.Entry<String, SPSiteQuality> e : siteQualities.entrySet())
            addKeyValue(factory, ps, e.getKey(), e.getValue().getParamSet(factory));

        for (Phase1Group p1g : groups) ps.addParamSet(p1g.getParamSet(factory));

        return ps;
    }

    private void addKeyValue(PioFactory factory, ParamSet parent, String key, ParamSet value) {
        Pio.addParam(factory, value, PARAM_MAP_KEY, key);
        parent.addParamSet(value);
    }

    @Override
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        Map<String, SpBlueprint> blueprintMap = new TreeMap<String, SpBlueprint>();
        for (final ParamSet ps : paramSet.getParamSets()) {
            if (SpBlueprintFactory.isSpBlueprintParamSet(ps)) {
                final String key = Pio.getValue(ps, PARAM_MAP_KEY);
                final SpBlueprint value = SpBlueprintFactory.fromParamSet(ps);
                blueprintMap.put(key, value);
            }
        }
        blueprints = Collections.unmodifiableMap(blueprintMap);

        Map<String, SPTarget> targetMap = new TreeMap<String, SPTarget>();
        for (final ParamSet ps : paramSet.getParamSets(SPTarget.PARAM_SET_NAME)) {
            final String key = Pio.getValue(ps, PARAM_MAP_KEY);
            final SPTarget value = new SPTarget();
            value.setParamSet(ps);
            targetMap.put(key, value);
        }
        targets = Collections.unmodifiableMap(targetMap);

        Map<String, SPSiteQuality> siteQualityMap = new TreeMap<String, SPSiteQuality>();
        for (final ParamSet ps : paramSet.getParamSets(SPSiteQuality.SP_TYPE.readableStr)) {
            final String key = Pio.getValue(ps, PARAM_MAP_KEY);
            final SPSiteQuality value = new SPSiteQuality();
            value.setParamSet(ps);
            siteQualityMap.put(key, value);
        }
        siteQualities = Collections.unmodifiableMap(siteQualityMap);

        List<Phase1Group> phase1GroupList = new ArrayList<Phase1Group>();
        for (final ParamSet ps : paramSet.getParamSets(Phase1Group.PARAM_SET_NAME)) {
            Phase1Group p1g = Phase1Group.fromParamSet(ps);
            phase1GroupList.add(p1g);
        }
        groups = Collections.unmodifiableList(phase1GroupList);
    }
}
