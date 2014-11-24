//
// $
//

package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
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
    private final GuidingFeedbackEditor guiding;

    SiderealEditor() {
        final MagnitudeEditor          med = new MagnitudeEditor();
        final OriginalMagnitudeEditor omed = new OriginalMagnitudeEditor();
        final ProperMotionEditor       ped = new ProperMotionEditor();
        this.guiding                       = new GuidingFeedbackEditor();

        posEditors = DefaultImList.create(med, omed, ped, guiding);
        pan        = createPermanentPanel(med, omed, ped, guiding);
    }

    private static JPanel createPermanentPanel(MagnitudeEditor med, OriginalMagnitudeEditor omed, ProperMotionEditor ped, GuidingFeedbackEditor gfr) {
        final JPanel pan = new JPanel(new GridBagLayout());

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
        final JPanel space = new JPanel() {{
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

        pan.add(gfr.getComponent(), new GridBagConstraints() {{
            gridx=0; gridy=2; gridwidth=4; fill=HORIZONTAL; weightx=1.0; insets=new Insets(10,0,0,0);
        }});

        // Original magnitude editor along the bottom, when visible
        pan.add(omed.getComponent(), new GridBagConstraints() {{
            gridx=0; gridy=3; gridwidth=4; fill=HORIZONTAL; weightx=1.0;
        }});


        return pan;
    }

    @Override public Component getComponent() { return pan; }

    @Override
    public void edit(final Option<ObsContext> ctx, final SPTarget target) {
        posEditors.foreach(new ApplyOp<TelescopePosEditor>() {
            @Override public void apply(TelescopePosEditor ed) {
                ed.edit(ctx, target);
            }
        });
    }

    public void updateGuiding(final Option<ObsContext> ctx, final SPTarget target) {
        guiding.edit(ctx, target);
    }
}
