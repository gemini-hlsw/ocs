package jsky.app.ot.gemini.editor.targetComponent.detail;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor;
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor;

import javax.swing.*;
import java.awt.*;

abstract class TargetDetailEditor extends JPanel implements TelescopePosEditor {

    // Constant Fields
    private final ITarget.Tag tag;
    private final MagnitudeEditor magnitudeEditor = new MagnitudeEditor();

    protected TargetDetailEditor(ITarget.Tag tag) {
        this.tag = tag;
        setLayout(new BorderLayout());
        add(magnitudeEditor.getComponent(), BorderLayout.WEST);
    }

    public void edit(final Option<ObsContext> obsContext, final SPTarget spTarget) {

        // Verify that our target has the correct tag. Note that we're racing here but there's not
        // much we can do about it. We just have to assume that nobody is replacing the ITarget
        // right now. If this turns out to be wrong then there will be problems down the line.
        final ITarget.Tag tag = spTarget.getTarget().getTag();
        if (tag != this.tag)
            throw new IllegalArgumentException("Expected " + this.tag + ", received " + tag);

        // Done with bookkeeping. Forward `edit` to the contained editors.
        magnitudeEditor.edit(obsContext, spTarget);

    }

    public ITarget.Tag getTag() {
        return tag;
    }

    static TargetDetailEditor forTag(ITarget.Tag tag) {
        switch (tag) {
            case JPL_MINOR_BODY:   return new JplMinorBodyDetailEditor();
            case MPC_MINOR_PLANET: return new MpcMinorPlanetDetailEditor();
            case NAMED:            return new NamedDetailEditor();
            case SIDEREAL:         return new SiderealDetailEditor();
            default: throw new Error("Unpossible");
        }
    }

}
