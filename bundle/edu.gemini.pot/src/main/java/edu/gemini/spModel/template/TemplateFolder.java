package edu.gemini.spModel.template;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPTemplateFolder;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.init.SimpleNodeInitializer;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.util.*;

public final class TemplateFolder extends AbstractDataObject {

    public static final String DEFAULT_TITLE = "Templates";
    public static final String VERSION = "2015A-1";
    public static final SPComponentType SP_TYPE = SPComponentType.TEMPLATE_FOLDER;

    public static final ISPNodeInitializer<ISPTemplateFolder, TemplateFolder> NI =
         new SimpleNodeInitializer<>(SP_TYPE, () -> new TemplateFolder());

    public static final String PARAM_MAP_KEY = "templateFolderMapKey";


    // The template folder contains the master list of blueprints, targets, and siteQualities
    // derived from the Phase I document. These are referenced by key in each template group.
    private Map<String, SpBlueprint> blueprints;

    public TemplateFolder() {
        setTitle(DEFAULT_TITLE);
        setType(SP_TYPE);
        setVersion(VERSION);
        blueprints = Collections.emptyMap();
    }

    public TemplateFolder(Map<String, SpBlueprint> blueprints) {
        setTitle(DEFAULT_TITLE);
        setType(SP_TYPE);
        setVersion(VERSION);
        this.blueprints = Collections.unmodifiableMap(new TreeMap<String, SpBlueprint>(blueprints));
    }

    // All accessors return unmodifiable data structures.

    public Map<String, SpBlueprint> getBlueprints()      { return blueprints; }


    @Override
    public ParamSet getParamSet(PioFactory factory) {
        final ParamSet ps = super.getParamSet(factory);

        for (Map.Entry<String, SpBlueprint> e : blueprints.entrySet())
            addKeyValue(factory, ps, e.getKey(), e.getValue().toParamSet(factory));

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
    }
}
