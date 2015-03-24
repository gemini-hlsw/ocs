package jsky.app.ot.gemini.editor.targetComponent.detail;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.ITarget;

import javax.swing.*;
import java.awt.*;

final class NamedDetailEditor extends TargetDetailEditor {

    private final JLabel label = new JLabel();

    NamedDetailEditor() {
        super(ITarget.Tag.NAMED);
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
    }

    public void edit(final Option<ObsContext> obsContext, final SPTarget spTarget) {
        super.edit(obsContext, spTarget);
        label.setText("NAMED: " + spTarget.getTarget().toString());
    }

}
