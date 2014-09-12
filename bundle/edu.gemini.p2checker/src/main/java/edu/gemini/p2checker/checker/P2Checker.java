//
//$Id: P2Checker.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.p2checker.checker;

import edu.gemini.p2checker.ags.AgsAnalysisRule;
import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.p2checker.rules.general.EmptySequenceRule;
import edu.gemini.p2checker.rules.general.GeneralRule;
import edu.gemini.p2checker.rules.general.SmartgcalMappingRule;
import edu.gemini.p2checker.rules.general.StructureRule;
import edu.gemini.p2checker.rules.gmos.GmosRule;
import edu.gemini.p2checker.rules.gnirs.GnirsRule;
import edu.gemini.p2checker.rules.gpi.GpiRule;
import edu.gemini.p2checker.rules.gsaoi.GsaoiRule;
import edu.gemini.p2checker.rules.michelle.MichelleRule;
import edu.gemini.p2checker.rules.nici.NiciRule;
import edu.gemini.p2checker.rules.nifs.NifsRule;
import edu.gemini.p2checker.rules.niri.NiriRule;
import edu.gemini.p2checker.rules.phoenix.PhoenixRule;
import edu.gemini.p2checker.rules.pwfs.PwfsRule;
import edu.gemini.p2checker.rules.trecs.TrecsRule;
import edu.gemini.p2checker.rules.flamingos2.Flamingos2Rule;
import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.obsComp.TargetObsComp;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main class used to apply rules on a particular program to get potential problems on it.
 */
public class P2Checker {
    private static P2Checker _instance;

    private Map<SPComponentType, IRule> _ruleMap;

    private class RuleComposite implements IRule {

        private List<IRule> _compositeRules;

        public RuleComposite(IRule rule) {
            _compositeRules = new ArrayList<IRule>();
            _compositeRules.add(new GeneralRule());
            _compositeRules.add(new EmptySequenceRule());
            _compositeRules.add(new SmartgcalMappingRule());
            _compositeRules.add(new PwfsRule());
            _compositeRules.add(new AgsAnalysisRule());
            _compositeRules.add(rule);
        }


        public IP2Problems check(ObservationElements node) {
            IP2Problems problems = new P2Problems();

            //first check the structure of the observation (missing instrument, missing obs.cond, etc)
            IP2Problems structureProblems = StructureRule.INSTANCE.check(node);
            //stop checking the rest until the structure is fixed.
            if (structureProblems.getProblemCount() > 0) {
                return structureProblems;
            }

            for (IRule rule : _compositeRules) {
                problems.append(rule.check(node));
            }
            return problems;
        }
    }


    public static P2Checker getInstance() {
        if (_instance == null) {
            _instance = new P2Checker();
        }
        return _instance;
    }


    private P2Checker() {
        _ruleMap = new HashMap<SPComponentType, IRule>();
        IRule gmosRule = new GmosRule();
        //add the GMOS Rule to all the instruments that supports it
        _ruleMap.put(InstGmosSouth.SP_TYPE, gmosRule);
        _ruleMap.put(InstGmosNorth.SP_TYPE, gmosRule);
        _ruleMap.put(InstNIFS.SP_TYPE, new NifsRule());
        _ruleMap.put(InstMichelle.SP_TYPE, new MichelleRule());
        _ruleMap.put(Flamingos2.SP_TYPE, new Flamingos2Rule());
        _ruleMap.put(Gpi.SP_TYPE, new GpiRule());
        _ruleMap.put(InstGNIRS.SP_TYPE, new GnirsRule());
        _ruleMap.put(Gsaoi.SP_TYPE, new GsaoiRule());
        _ruleMap.put(InstNICI.SP_TYPE, new NiciRule());
        _ruleMap.put(InstNIRI.SP_TYPE, new NiriRule());
        _ruleMap.put(InstTReCS.SP_TYPE, new TrecsRule());
        _ruleMap.put(InstPhoenix.SP_TYPE, new PhoenixRule());
    }

    private IRule _getRule(ObservationElements elements) {
        if (elements == null) return null;

        SPInstObsComp instrument = elements.getInstrument();
        if (instrument == null) return null;

        return _ruleMap.get(elements.getInstrumentNode().getType());
    }


    /**
     * Main entry point for checking a program node.
     *
     * @param node The node to be checked. Could be the whole program or only a subset of it
     * @return the problems found in the given node after applying the rules to it.
     */
    public IP2Problems check(ISPNode node) {
        //obs/seq component require us to find the observation first
        if (node instanceof ISPSeqComponent ||
                node instanceof ISPObsComponent) {
            if (node.getParent() instanceof ISPObservation) {
                return _checkObservation((ISPObservation) node.getParent());
            } else {
                return check(node.getParent()); //recursively attempt to find the Observation
            }
            //observations can be checked immediately
        } else if (node instanceof ISPObservation) {
            return _checkObservation((ISPObservation) node);
            //groups contain observations, check them individually
        } else if (node instanceof ISPGroup) {
            ISPGroup group = (ISPGroup) node;
            IP2Problems problems = new P2Problems();
            for (ISPObservation obs : group.getObservations()) {
                problems.append(check(obs));
            }
            return problems;
            //a program has groups and observations. Check them all.
        } else if (node instanceof ISPProgram) {
            ISPProgram program = (ISPProgram) node;
            IP2Problems problems = new P2Problems();

            for (ISPObservation obs : program.getObservations()) {
                problems.append(check(obs));
            }

            for (Object o : program.getGroups()) {
                ISPGroup group = (ISPGroup) o;
                problems.append(check(group));
            }

            // Also a template folder
            problems.append(check(program.getTemplateFolder()));

            return problems;

        } else if (node instanceof ISPTemplateFolder) {

            // Template folder has groups in it
            IP2Problems problems = new P2Problems();
            final ISPTemplateFolder tf = (ISPTemplateFolder) node;
            for (ISPTemplateGroup tg : tf.getTemplateGroups())
                problems.append(check(tg));
            return problems;

        } else if (node instanceof ISPTemplateGroup) {

            // Template group has obs in it
            IP2Problems problems = new P2Problems();
            final ISPTemplateGroup tg = (ISPTemplateGroup) node;
            for (ISPObservation o : tg.getAllObservations())
                problems.append(check(o));
            return problems;

        } else {
            // node is null or irrelevant
        }

        return null;
    }

    //Perform the checking of an observation, the small
    //unit that can be checked individually.
    private IP2Problems _checkObservation(ISPObservation node) {
        ObservationElements elements = new ObservationElements(node);

        IRule rule = _getRule(elements);
        if (rule == null) {
            //we don't have rules for this stuff so just check the structure of
            //the observation
            return StructureRule.INSTANCE.check(elements);
        }

        //add the rule
        RuleComposite rc = new RuleComposite(rule);

        //and apply the rule for the entire observation elements
        return rc.check(elements);
    }
}
