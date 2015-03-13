package jsky.app.ot.gemini.editor.targetComponent.detail;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor;

import javax.swing.*;
import java.awt.*;

public final class TargetDetailPanel extends JPanel implements TelescopePosEditor {

    // This doodad will ensure that any change event coming from the SPTarget will get turned into
    // a call to `edit`, so we don't have to worry about that case everywhere. Everything from here
    // on down only needs to care about implementing `edit`.
    private final ForwardingTelescopePosWatcher tpw = new ForwardingTelescopePosWatcher(this);

    private TargetDetailEditor tde;

    public TargetDetailPanel() {
        setBorder(BorderFactory.createLineBorder(Color.RED));
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(0, 200)); // UGH
    }

    public void edit(final Option<ObsContext> obsContext, final SPTarget spTarget) {

        // Create or replace the existing detail editor, if needed
        final ITarget.Tag tag = spTarget.getTarget().getTag();
        if (tde == null || tde.getTag() != tag) {
            if (tde != null) {
                remove(tde);
            }
            tde = TargetDetailEditor.forTag(tag);
            add(tde, BorderLayout.CENTER);
            tde.edit(obsContext, spTarget);
        }

        // Forward the `edit` call.
        tpw.edit(obsContext, spTarget);
        tde.edit(obsContext, spTarget);

    }

}


