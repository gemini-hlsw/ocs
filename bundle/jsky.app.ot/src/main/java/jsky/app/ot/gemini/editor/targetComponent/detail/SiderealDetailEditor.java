package jsky.app.ot.gemini.editor.targetComponent.detail;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.ot.gemini.editor.targetComponent.GuidingFeedbackEditor;
import jsky.app.ot.gemini.editor.targetComponent.ProperMotionEditor;
import jsky.app.ot.gemini.editor.targetComponent.TrackingEditor;

import java.awt.*;

final class SiderealDetailEditor extends TargetDetailEditor {

    private final ProperMotionEditor ped = new ProperMotionEditor();
    private final GuidingFeedbackEditor gfe = new GuidingFeedbackEditor();
    private final TrackingEditor te  = new TrackingEditor();

    SiderealDetailEditor() {
        super(ITarget.Tag.SIDEREAL);
        setLayout(new BorderLayout());
        add(ped.getComponent(), BorderLayout.CENTER);
        add(te .getComponent(), BorderLayout.EAST);
        add(gfe.getComponent(), BorderLayout.SOUTH);
    }

    public void edit(final Option<ObsContext> obsContext, final SPTarget spTarget) {
        super.edit(obsContext, spTarget);
        ped.edit(obsContext, spTarget);
        te .edit(obsContext, spTarget);
        gfe.edit(obsContext, spTarget);
    }

}
