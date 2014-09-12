package edu.gemini.phase2.core.odb;

import edu.gemini.phase2.core.model.GroupShell;
import edu.gemini.phase2.core.model.TemplateFolderExpansion;
import edu.gemini.phase2.core.model.TemplateGroupShell;
import edu.gemini.pot.sp.*;
import edu.gemini.spModel.template.TemplateGroup;
import edu.gemini.spModel.util.VersionToken;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a service for storing template folder updates.  If used from a
 * remote client, it is highly recommended that you use the
 * TemplateFolderFunctor which calls the TemplateFolderService from a functor
 * running in the ODB.
 */
public final class TemplateFolderService {
    private static final Logger LOG = Logger.getLogger(TemplateFolderService.class.getName());

    public enum BaselineOption {
        /**
         * Create baseline calibration groups, if any are present.
         */
        ADD,

        /**
         * Do not create baseline calibration groups.
         */
        SKIP,
        ;
    }

    public enum TemplateOption {
        /**
         * Adds new template groups to the folder fully configured (with all
         * targets and conditions as configured at Phase 1) leaving any
         * existing groups and their targets and conditions alone.
         */
        ADD,

        /**
         * Adds new template groups to the folder but with no targets and
         * conditions leaving any existing template groups alone.
         */
        ADD_EMPTY,

        /**
         * Adds new template groups to the folder with all their (target,
         * conditions) parameters, removing them any existing template groups.
         */
        ADD_MOVE,

        /**
         * Replaces any existing template groups with new fully configured
         * groups including all their targets and conditions as configured
         * during Phase 1.  This option is essentially to start over from
         * scratch.
         */
        REPLACE,

        /**
         * Do not create any new template groups.
         */
        SKIP,
        ;

    }

    private TemplateFolderService() {}

    public static void store(TemplateFolderExpansion exp, TemplateOption templateOption, BaselineOption baselineOption, ISPProgram program, ISPFactory factory)  {
        storeTemplates(exp.templateGroups, templateOption, program, factory);
        storeBaselines(exp.baselineCalibrations, baselineOption, program, factory);
    }

    public static void storeTemplates(List<TemplateGroupShell> templates, TemplateOption templateOption, ISPProgram program, ISPFactory factory)  {
        if (templateOption == TemplateOption.SKIP) return;

        final ISPTemplateFolder folder = program.getTemplateFolder();
        if (folder == null) {
            LOG.warning("Can't expand templates for program with no template folder: " + program.getProgramID());
            return;
        }

        try {
            final List<ISPTemplateGroup> tgList;

            // If replacing, forget about any existing groups and start with an
            // empty list.  Otherwise, copy existing groups to a new modifiable
            // list.
            if (templateOption == TemplateOption.REPLACE) {
                tgList = new ArrayList<ISPTemplateGroup>();
            } else {
                tgList = new ArrayList<ISPTemplateGroup>(folder.getTemplateGroups());
            }

            // Remove existing parameters if we are "moving" them to the new
            // templates.
            if (templateOption == TemplateOption.ADD_MOVE) {
                for (ISPTemplateGroup existingGroup : tgList) {
                    existingGroup.setTemplateParameters(Collections.EMPTY_LIST);
                }
            }

            // Add new template groups.
            int index = nextTemplateGroupIndex(tgList);
            for (TemplateGroupShell shell : templates) {
                ISPTemplateGroup tg = shell.toSp(factory, program);

                // Set the template group index.
                TemplateGroup dataObj = (TemplateGroup) tg.getDataObject();
                dataObj.setVersionToken(new VersionToken(index++));
                tg.setDataObject(dataObj);

                // But, if we are adding them "empty", then wipe out their
                // parameters.
                if (templateOption == TemplateOption.ADD_EMPTY) {
                    tg.setTemplateParameters(Collections.EMPTY_LIST);
                }
                tgList.add(tg);
            }

            // Store the new template group list.
            folder.setTemplateGroups(tgList);

        } catch (SPException ex) {
            // Logically this should not be possible here.
            LOG.log(Level.SEVERE, "SPNodeNotLocalException", ex);
            throw new RuntimeException(ex);
        }
    }

    // Find the highest unused template group number in the folder, starting
    // at 1 if there are no template groups in the folder.
    private static int nextTemplateGroupIndex(List<ISPTemplateGroup> groups)  {
        int max = 0;
        for (ISPTemplateGroup grp : groups) {
            TemplateGroup dataObj = (TemplateGroup) grp.getDataObject();
            VersionToken tok = dataObj.getVersionToken();
            if ((tok != null) && tok.getFirstSegment() > max) max = tok.getFirstSegment();
        }
        return max + 1;
    }

    public static void storeBaselines(List<GroupShell> baselines, BaselineOption baselineOption, ISPProgram program, ISPFactory factory)  {
        if (baselineOption == BaselineOption.SKIP) return;

        try {
            for (GroupShell shell : baselines) {
                if ((shell.obsComponents.size() > 0) || (shell.observations.size() > 0)) {
                    program.addGroup(shell.toSp(factory, program));
                }
            }
        } catch (SPException ex) {
            // Logically this should not be possible here.
            LOG.log(Level.SEVERE, "SPNodeNotLocalException", ex);
            throw new RuntimeException(ex);
        }
    }
}
