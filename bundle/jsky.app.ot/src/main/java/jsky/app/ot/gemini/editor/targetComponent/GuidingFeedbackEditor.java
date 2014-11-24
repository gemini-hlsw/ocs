package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import jsky.app.ot.OT;

import java.awt.*;

/**
 * Displays the guiding feedback associated with a target, if any.
 */
final class GuidingFeedbackEditor implements TelescopePosEditor {
    private final GuidingFeedback.Table tab = new GuidingFeedback.Table();

    public Component getComponent() { return tab.peer(); }

    @Override
    public void edit(final Option<ObsContext> ctxOpt, final SPTarget target) {
        final AgsMagnitude.MagnitudeTable mt = OT.getMagnitudeTable();
        if (ctxOpt.isEmpty()) {
            tab.clear();
        } else {
            tab.showRows(GuidingFeedback.targetAnalysis(ctxOpt.getValue(), mt, target));
        }
    }
}
