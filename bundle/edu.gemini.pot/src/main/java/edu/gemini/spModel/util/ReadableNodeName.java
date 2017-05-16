package edu.gemini.spModel.util;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obscomp.SPNote;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.template.TemplateParameters;

/**
 * Produces a human-readable short string that indicates the type and/or
 * content of a given program node.
 */
public final class ReadableNodeName {
    private ReadableNodeName() {}

    private static final class Formatter implements ISPProgramVisitor {
        private String result;

        private String formatTarget(ISPObsComponent oc) {
            final TargetObsComp toc = (TargetObsComp) oc.getDataObject();
            return String.format("Target Environment '%s'", toc.getAsterism().name());
        }

        private String formatInstrument(ISPObsComponent oc) {
            return oc.getType().narrowType;
        }

        private String formatGenericWithTitle(ISPObsComponent oc) {
            final String type  = formatGeneric(oc);
            final String title = getTitle(oc);
            return "".equals(title) || type.equals(title) ? type : String.format("%s '%s'", type, title);
        }

        private String formatGeneric(ISPObsComponent oc) {
            return oc.getType().readableStr;
        }

        private String formatGeneric(ISPSeqComponent sc) {
            return sc.getType().readableStr;
        }

        private String getTitle(ISPNode node) {
            final ISPDataObject dataObject = node.getDataObject();
            final String title = dataObject.getTitle();
            return (title == null) ? "" : title;
        }

        @Override public void visitObsComponent(ISPObsComponent node) {
            final Object dataObject = node.getDataObject();
            if (SPTreeUtil.isTargetEnv(node)) {
                result = formatTarget(node);
            } else if (SPTreeUtil.isInstrument(node)) {
                result = formatInstrument(node);
            } else if (dataObject instanceof SPNote) {
                result = formatGenericWithTitle(node);
            } else {
                result = formatGeneric(node);
            }
        }

        @Override public void visitConflictFolder(ISPConflictFolder node) {
            result = "Conflict Folder";
        }

        @Override public void visitObsQaLog(ISPObsQaLog node) {
            result = "Observing Qa Log";
        }

        @Override public void visitObsExecLog(ISPObsExecLog node) {
            result = "Observing Exec Log";
        }

        @Override public void visitObservation(ISPObservation node) {
            final ISPDataObject o = node.getDataObject();
            if (o instanceof SPObservation) {
                final SPObservation spo = (SPObservation) o;
                if (spo.getLibraryId() != null) {
                    result = String.format("Observation %d (Library id %s)", node.getObservationNumber(), spo.getLibraryId());
                    return;
                }
            }
            result = String.format("Observation %d", node.getObservationNumber());
        }

        @Override public void visitGroup(ISPGroup node) {
            result = String.format("Group '%s'", getTitle(node));
        }

        @Override public void visitProgram(ISPProgram node) {
            SPProgramID pid = node.getProgramID();
            result = String.format("Program '%s'", pid != null ? pid.toString() : "");
        }

        @Override public void visitSeqComponent(ISPSeqComponent node) {
            result = formatGeneric(node);
        }

        @Override public void visitTemplateFolder(ISPTemplateFolder node) {
            result = "Template Folder";
        }

        @Override public void visitTemplateGroup(ISPTemplateGroup node) {
            result = String.format("Template Group '%s'", getTitle(node));
        }

        // Truly awful formatting of a template parameters node
        @Override public void visitTemplateParameters(ISPTemplateParameters node) {
            final TemplateParameters tp = (TemplateParameters) node.getDataObject();
            result = "Template Parameters";
            final SPTarget      t = tp.getTarget();
            final SPSiteQuality c = tp.getSiteQuality();
            final TimeValue     v = tp.getTime();
            if ((t != null) && (c != null) && (v != null)) {
                final String ts = t.getName();
                final String cs = c.conditions().toString();
                final String vs = v.toString(2);
                if (ts != null) {
                    result = String.format("%s, %s, %s", ts, cs, vs);
                }
            }
        }
    }

    public static String format(ISPNode node) {
        if (!(node instanceof ISPProgramNode)) return "";
        final Formatter f = new Formatter();
        ((ISPProgramNode) node).accept(f);
        return f.result;
    }
}
