package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.wdba.glue.api.WdbaGlueException;
import edu.gemini.spModel.target.env.GuideGroup;

public class TccFieldConfig extends ParamSet {
    private ObservationEnvironment _oe;
    private String _name;

    public TccFieldConfig(ObservationEnvironment oe) {
        super(PARAMSET);
        if (oe == null) throw new NullPointerException("Config requires a non-null observation environment");
        _oe = oe;
        _name = _oe.getBasePositionName();
    }

    /**
     * build will use the <code>(@link TargetEnv}</code> to construct
     * an XML document.
     */
    public boolean build() throws WdbaGlueException {
        TargetObsComp obsComp = _oe.getTargetObsComp();
        if (obsComp == null) {
            _logAbort("No TargetEnv dataobj in observation: " + _oe.getObservationID(), null);
            return false;
        }
        // First add the special parameter pointing to the base
        addAttribute(NAME, _name);
        addAttribute(TYPE, TccNames.FIELD);

        // Add the easy things
        SPObservationID obsId = _oe.getObservationID();
        putParameter(TccNames.PROGRAMID, obsId == null ? "" : obsId.getProgramID().toString());
        putParameter(TccNames.OBSERVATIONID, obsId == null ? "" : obsId.toString());

        // Add a wavelength parameter and then create the config
        WavelengthConfig wc = new WavelengthConfig(_oe);
        if (wc.build()) {
            putParameter(TccNames.WAVELENGTH, wc.getConfigName());
            add(wc);
        }

        // Selected guide group name
        putParameter(TccNames.GUIDE_GROUP, getPrimaryGuideGroupName());

        // Add a rotator parameter and then create the config
        // Note: If the config name contains "Fixed" in some way, it's assumed to be a Fixed rotator config
        RotatorConfig rc = new RotatorConfig(_oe);
        if (rc.build()) {
            String rotConfigName = rc.getConfigName();
            if (rotConfigName.equals(TccNames.ALTAIR_FIXED)) {
                putParameter(TccNames.ROTATOR, rotConfigName);
            } else
            // If the instrument adds something, create a rotator config and add it in
            if (rotConfigName.contains(TccNames.FIXED)) {
                putParameter(TccNames.ROTATOR, rotConfigName);
                add(rc);
            } else {
                putParameter(TccNames.POSANGLE, rotConfigName);
                add(rc);
            }
        }

        GuideConfig gc = new GuideConfig(_oe);
        if (gc.build()) putParamSet(gc);

        addTargets(obsComp.getTargetEnvironment());

        return true;
    }

    private String getPrimaryGuideGroupName() {
        TargetEnvironment env = _oe.getTargetEnvironment();
        final GuideGroup gg = env.getPrimaryGuideGroup();
        //Only return a name if there are more than one group.
        if ( env.getGroups().size() <= 1 ) return "";
        if (!gg.getName().isEmpty()) return gg.getName().getValue();

        final Option<Tuple2<GuideGroup, Integer>> res = env.getGroups().zipWithIndex().find(tup -> tup._1() == gg);
        return res.map(tup -> "Guide Group " + (tup._2() + 1)).getOrElse("");
    }

    private void addTargets(TargetEnvironment env) throws WdbaGlueException {
        addBaseGroup(env);
        for (GuideProbeTargets gt : env.getPrimaryGuideGroup()) addGuideGroup(gt);
    }

    private static boolean isEmpty(String name) {
        return (name == null) || "".equals(name.trim());
    }

    private void addBaseGroup(TargetEnvironment env) throws WdbaGlueException {
        // Add the target itself.
        SPTarget base = env.getArbitraryTargetFromAsterism();
        if (isEmpty(base.getName())) {
            base.setName(TccNames.BASE);
        }
        add(new TargetConfig(base));

        // Add the user targets.
        int pos = 1;
        for (SPTarget user : env.getUserTargets()) {
            if (isEmpty(user.getName())) {
                user.setName(TargetConfig.formatName("User", pos));
            }
            ++pos;
            add(new TargetConfig(user));
        }

        // Add the "group" for the base position.
        add(TargetGroupConfig.createBaseGroup(env));
    }

    private void addGuideGroup(GuideProbeTargets gt) throws WdbaGlueException {
        // Ignore disabled guide targets.
        if (!_oe.getAvailableGuiders().contains(gt.getGuider())) return;

        // Ignore empty guide targets.
        final ImList<SPTarget> targets = gt.getTargets();
        if (targets.size() == 0) return;

        // Add each target, setting any missing names along the way.
        final String tag = TargetConfig.getTag(gt.getGuider());

        int pos = 1;
        for (SPTarget target : targets) {
            if (isEmpty(target.getName())) {
                target.setName(TargetConfig.formatName(tag, pos));
            }
            add(new TargetConfig(target));
            ++pos;
        }

        // Add the group for these targets.
        add(TargetGroupConfig.createGuideGroup(gt));
    }

    // private method to log and throw and exception
    private void _logAbort(String message, Exception ex) throws WdbaGlueException {
        //LOG.error(message);
        throw new WdbaGlueException(message, (ex != null) ? ex : null);
    }

}

