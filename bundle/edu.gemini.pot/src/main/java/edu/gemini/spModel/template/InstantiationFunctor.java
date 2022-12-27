package edu.gemini.spModel.template;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.env.Asterism$;
import edu.gemini.spModel.target.env.AsterismType;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.util.DefaultSchedulingBlock;


import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.gemini.spModel.template.FunctorHelpers.addIfNotPresent;

/**
 * Template instantiation functor that runs on a program, instantiating a set of TemplateGroup/TemplateParameters
 * pairs. The group and parameters need not come from the same program as that to which the functor is applied, nor
 * do the template parameters need to exist in the template group. We can tighten this down if needed.
 */
public class InstantiationFunctor extends DBAbstractFunctor {

    private static final Logger LOGGER = Logger.getLogger(InstantiationFunctor.class.getName());

    private final Map<ISPTemplateGroup, Set<ISPTemplateParameters>> selection = new HashMap<>();

    /**
     * Add a group/params pair to the set of instantiations to be performed.
     * @param group a template group
     * @param params a set of params
     */
    public void add(ISPTemplateGroup group, ISPTemplateParameters params) {
        Set<ISPTemplateParameters> list = selection.get(group);
        if (list == null) {
            list = new HashSet<>();
            selection.put(group, list);
        }
        list.add(params);
    }

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        try {
            List<ISPGroup> newGroups = new ArrayList<>();
            final ISPProgram prog = db.lookupProgram(node.getProgramKey());
            for (Map.Entry<ISPTemplateGroup, Set<ISPTemplateParameters>> e : selection.entrySet()) {
                for (ISPTemplateParameters ps : e.getValue()) {
                    final ISPTemplateGroup templateGroup = e.getKey();
                    final TemplateParameters templateParametersData = (TemplateParameters) ps.getDataObject();
                    final SPSiteQuality siteQualityData = templateParametersData.getSiteQuality();
                    final SPTarget targetData = templateParametersData.getTarget();
                    final ISPGroup grp = instantiate(db.getFactory(), prog, templateGroup, siteQualityData, targetData);
                    newGroups.add(grp);
                }
            }

            // Update the group list all at once.  This way, only one event
            // is sent to the clients with all the changes.
            List<ISPGroup> allGroups = prog.getGroups();
            allGroups.addAll(newGroups);
            prog.setGroups(allGroups);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Trouble during template instantiation.", e);
            setException(e);
        }
    }

    // Instantiate a given config, conditions, target triple
    private static ISPGroup instantiate(ISPFactory fact, ISPProgram prog, ISPTemplateGroup templateGroup, SPSiteQuality siteQualityData, SPTarget targetData) throws Exception {
        final TemplateGroup templateGroupData = (TemplateGroup) templateGroup.getDataObject();
        final ISPGroup group = createGroup(fact, prog, targetData, templateGroupData);
        copyNotes(fact, prog, templateGroup, group);
        copyObservations(fact, prog, templateGroup, siteQualityData, targetData, group);
        return group;
    }

    // Copy observations from the template group into the destination group
    private static void copyObservations(ISPFactory fact, ISPProgram prog, ISPTemplateGroup templateGroup, SPSiteQuality siteQualityData, SPTarget targetData, ISPGroup group) throws Exception {
        final AsterismType astType = ((TemplateGroup) templateGroup.getDataObject()).getAsterismType();

        for (ISPObservation templateObs: templateGroup.getAllObservations()) {

            // Clone the template obs
            final ISPObservation newObs = fact.createObservationCopy(prog, templateObs, false);
            final SPObservation newObsData = (SPObservation) newObs.getDataObject();

            // Add a default scheduling block
            newObsData.setSchedulingBlock(ImOption.apply(DefaultSchedulingBlock.forProgram(prog)));

            // Save a pointer to the template
            newObsData.setOriginatingTemplate(templateObs.getNodeKey());
            newObs.setDataObject(newObsData);

            // Copy target/conds for relevant obs classes.  Note, if the
            // template obs has an explicitly defined target or conditions node,
            // it is not replaced with template values.
            final ObsClass newObsClass = ObsClassService.lookupObsClass(newObs);
            if (newObsClass != ObsClass.DAY_CAL) {
                copySiteQuality(fact, prog, siteQualityData, newObs);
            }
            if (newObsClass == ObsClass.SCIENCE || newObsClass == ObsClass.ACQ) {
                copyTarget(fact, prog, astType, targetData, newObs);
            }

            // Done
            group.addObservation(newObs);
        }
    }

    // Copy the specified target into the specified observation
    private static void copyTarget(ISPFactory fact, ISPProgram prog, AsterismType astType, SPTarget targetData, ISPObservation newObs) throws SPUnknownIDException, SPNodeNotLocalException, SPTreeStateException {
        final ISPObsComponent comp = fact.createObsComponent(prog, TargetObsComp.SP_TYPE, null);
        final TargetObsComp    toc = (TargetObsComp) comp.getDataObject();
        final Asterism         ast = Asterism$.MODULE$.fromTypeAndTemplateTarget(astType, targetData);
        toc.setTargetEnvironment(toc.getTargetEnvironment().setAsterism(ast));
        comp.setDataObject(toc);
        addIfNotPresent(newObs, comp);
    }

    // Copy the specified site quality into the specified observation
    private static void copySiteQuality(ISPFactory fact, ISPProgram prog, SPSiteQuality siteQualityData, ISPObservation newObs) throws SPUnknownIDException, SPNodeNotLocalException, SPTreeStateException {
        final ISPObsComponent comp = fact.createObsComponent(prog, SPSiteQuality.SP_TYPE, null);
        comp.setDataObject(siteQualityData);
        addIfNotPresent(newObs, comp);
    }

    // Copy all notes from the template group into the target group
    private static void copyNotes(ISPFactory fact, ISPProgram prog, ISPTemplateGroup templateGroup, ISPGroup group) throws SPUnknownIDException, SPNodeNotLocalException, SPTreeStateException {
        for (ISPObsComponent oc: templateGroup.getObsComponents()) {
            final ISPObsComponent newObsComponent = fact.createObsComponentCopy(prog, oc, false);
            group.addObsComponent(newObsComponent);
        }
    }

    // Create a scheduling group based on the specified targetData
    private static ISPGroup createGroup(ISPFactory fact, ISPProgram prog, SPTarget targetData, TemplateGroup templateGroupData) throws SPUnknownIDException, SPNodeNotLocalException, SPTreeStateException {
        final ISPGroup group = fact.createGroup(prog, null);
        final SPGroup groupData = (SPGroup) group.getDataObject();
        groupData.setTitle(createGroupTitle(templateGroupData, targetData));
        groupData.setGroupType(templateGroupData.getGroupType());
        group.setDataObject(groupData);
        return group;
    }

    // Get a group name from the given config, conditions, target triple
    private static String createGroupTitle(TemplateGroup templateGroupData, SPTarget targetData) {
        return String.format("%s - [%s] %s",
                targetData.getName(),
                templateGroupData.getVersionToken(),
                templateGroupData.getTitle());
    }

}
