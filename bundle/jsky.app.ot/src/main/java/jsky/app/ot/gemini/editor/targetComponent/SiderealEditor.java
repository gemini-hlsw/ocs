//
// $
//

package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.target.SPTarget;

import javax.swing.*;
import java.awt.*;

/**
 * The sidereal editor combines editors for sidereal targets including the
 * {@link MagnitudeEditor}, the {@link ProperMotionEditor}, and the
 * {@link TrackingEditor}.
 */
final class SiderealEditor implements TelescopePosEditor {
    private final JPanel pan;
    private final ImList<TelescopePosEditor> posEditors;

    SiderealEditor() {

        MagnitudeEditor          med = new MagnitudeEditor();
        OriginalMagnitudeEditor omed = new OriginalMagnitudeEditor();
        ProperMotionEditor       ped = new ProperMotionEditor();

        posEditors = DefaultImList.create(med, omed, ped);
        pan = createPermanentPanel(med, omed, ped);
    }


    private static JPanel createPermanentPanel(MagnitudeEditor med, OriginalMagnitudeEditor omed, ProperMotionEditor ped) {
        JPanel pan = new JPanel(new GridBagLayout());

        // Place the editors in the panel.

        // Magnitude Editor
        pan.add(new JLabel("Brightness"), new GridBagConstraints() {{
            gridx=0; gridy=0; anchor=WEST; fill=HORIZONTAL; insets=new Insets(0,0,5,0);
        }});
        pan.add(med.getComponent(), new GridBagConstraints() {{
            gridx=0; gridy=1; anchor=WEST; fill=BOTH; weighty=1.0;
        }});

        // Add a spacer
        final Dimension d = new Dimension(40,0);
        JPanel space = new JPanel() {{
            setPreferredSize(d); setMinimumSize(d); setMaximumSize(d);
        }};
        pan.add(space, new GridBagConstraints() {{ gridx = 1; gridy = 0; }});

        // Proper Motion Editor
        pan.add(new JLabel("Proper Motion"), new GridBagConstraints() {{
            gridx=2; gridy=0; anchor=WEST; insets=new Insets(0,0,5,0);
        }});
        pan.add(ped.getComponent(), new GridBagConstraints() {{
            gridx=2; gridy=1; anchor=NORTHWEST; insets=new Insets(0,5,0,0);
        }});

        // Push everything to the left
        pan.add(new JPanel(), new GridBagConstraints() {{
            gridx=3; gridy=0; fill=BOTH; weightx=1.0;
        }});

        // Original magnitude editor along the bottom, when visible
        pan.add(omed.getComponent(), new GridBagConstraints() {{
            gridx=0; gridy=2; gridwidth=4; fill=HORIZONTAL; insets=new Insets(0,0,0,0); weightx=1.0;
        }});

        return pan;
    }

    @Override public Component getComponent() { return pan; }

    @Override
    public void edit(final SPTarget target) {
        posEditors.foreach(new ApplyOp<TelescopePosEditor>() {
            @Override public void apply(TelescopePosEditor ed) {
                ed.edit(target);
            }
        });
    }
}
