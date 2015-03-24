package jsky.app.ot.gemini.editor.targetComponent.detail;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.DropDownListBoxWidgetWatcher;

import javax.swing.*;
import java.awt.*;

public final class TargetDetailPanel extends JPanel implements TelescopePosEditor {

    // This doodad will ensure that any change event coming from the SPTarget will get turned into
    // a call to `edit`, so we don't have to worry about that case everywhere. Everything from here
    // on down only needs to care about implementing `edit`.
    private final ForwardingTelescopePosWatcher tpw = new ForwardingTelescopePosWatcher(this);

    // Dropdown to change the target type (!)
    private final DropDownListBoxWidget targetType = new DropDownListBoxWidget() {{
        setChoices(ITarget.Tag.values());
        addWatcher(new DropDownListBoxWidgetWatcher() {
            public void dropDownListBoxAction(DropDownListBoxWidget w, int index, String val) {
                final ITarget.Tag tag = (ITarget.Tag) w.getSelectedItem();
                if (target != null) {
                    target.setTargetType(tag);
                }
            }
        });
    }};

    private SPTarget target = null;
    private TargetDetailEditor tde;

    public TargetDetailPanel() {
        setBorder(BorderFactory.createLineBorder(Color.RED));
        setLayout(new GridBagLayout());

        final GridBagConstraints tlc = new GridBagConstraints();
        tlc.gridx = 0;
        tlc.gridy = 0;
        tlc.fill = GridBagConstraints.HORIZONTAL;
        tlc.insets = new Insets(0, 0, 0, 5);
        add(new JLabel("Target Type"), tlc);

        final GridBagConstraints ttc = new GridBagConstraints();
        ttc.gridx = 1;
        ttc.gridy = 0;
        tlc.insets = new Insets(0, 5, 0, 0);
        ttc.fill = GridBagConstraints.HORIZONTAL;
        add(targetType, ttc);

    }

    public void edit(final Option<ObsContext> obsContext, final SPTarget spTarget) {

        // Create or replace the existing detail editor, if needed
        final ITarget.Tag tag = spTarget.getTarget().getTag();
        if (tde == null || tde.getTag() != tag) {
            if (tde != null) {
                remove(tde);
            }
            tde = TargetDetailEditor.forTag(tag);

            // Add editor
            final GridBagConstraints tdec = new GridBagConstraints();
            tdec.gridx = 0;
            tdec.gridy = 1;
            tdec.gridwidth = 2;
            tdec.insets = new Insets(15, 0, 0, 0);
            tdec.fill = GridBagConstraints.BOTH;


            add(tde, tdec);

            tde.edit(obsContext, spTarget);
        }

        // Forward the `edit` call.
        tpw.edit(obsContext, spTarget);
        tde.edit(obsContext, spTarget);

        // Local updates
        target = spTarget;
        targetType.setSelectedItem(target.getTarget().getTag());

    }

}


