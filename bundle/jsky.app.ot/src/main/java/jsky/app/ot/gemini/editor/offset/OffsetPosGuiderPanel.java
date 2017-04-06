package jsky.app.ot.gemini.editor.offset;

import edu.gemini.spModel.guide.DefaultGuideOptions;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeMap;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A JPanel containing widgets for editing offset position WFS choices.
 */
public final class OffsetPosGuiderPanel extends JPanel {
    private final JComboBox defaultCombo;  // holds the default guide state
    private final Map<GuideProbe, JComboBox> comboMap;

    private static final int ROWS = 2;

    public OffsetPosGuiderPanel() {
        super(new GridBagLayout());

        // Create the combo box for default guide options.
        final List<GuideOption> defOpts = DefaultGuideOptions.instance.getAll();
        final ComboBoxModel defModel    = new DefaultComboBoxModel(defOpts.toArray(new GuideOption[defOpts.size()]));
        defaultCombo = new JComboBox(defModel);

        // Create all the advanced guiding combo boxes.
        Map<GuideProbe, JComboBox> m = new HashMap<GuideProbe, JComboBox>();
        for (GuideProbe guider : GuideProbeMap.instance.values()) {
            JComboBox cb = new JComboBox();
            cb.setEditable(false);

            List<GuideOption> opts = guider.getGuideOptions().getAll();
            cb.setModel(new DefaultComboBoxModel(opts.toArray(new GuideOption[opts.size()])));
            m.put(guider, cb);
        }

        comboMap = Collections.unmodifiableMap(m);
    }

    public JComboBox getDefaultCombo() { return defaultCombo; }
    public JComboBox getAdvancedCombo(GuideProbe guider) {
        return comboMap.get(guider);
    }

    public void setGuiders(Set<GuideProbe> guiders) {
        // Wipe out the old combo boxes
        removeAll();

        // Sort the guiders by key.
        final java.util.List<GuideProbe> guideList = sortGuiders(guiders);

        final int sz = guideList.size();
        final int cols = (sz+1)/ROWS + (((sz+1)%ROWS == 0) ? 0 : 1);

        GridBagConstraints gbc = new GridBagConstraints();

        boolean def = true;
        int i = 0;
        for (int col=0; col<cols; ++col) {
            for (int row=0; row<ROWS && (i<sz || def); ++row) {
                final String label;
                final JComboBox box;
                if (def) {
                    label = "Guiding";
                    box   = defaultCombo;
                    def   = false;
                } else {
                    final GuideProbe guider = guideList.get(i++);
                    label = guider.getKey();
                    box   = comboMap.get(guider);
                }

                int bottomPad = (row+1 == ROWS) ? 0 :  5;
                int rightPad  = (col+1 == cols) ? 0 : 15;

                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridx  = col*2;
                gbc.gridy  = row;
                gbc.insets = new Insets(0, 0, bottomPad, 5);
                gbc.fill   = GridBagConstraints.NONE;
                add(new JLabel(label), gbc);

                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridx  = col*2 + 1;
                gbc.insets = new Insets(0, 0, bottomPad, rightPad);
                gbc.fill   = GridBagConstraints.HORIZONTAL;
                add(box, gbc);
            }
        }

        revalidate();
        repaint();
    }

    private java.util.List<GuideProbe> sortGuiders(Collection<GuideProbe> guiders) {
        java.util.List<GuideProbe> guideList = new ArrayList<GuideProbe>(guiders);
        Collections.sort(guideList, GuideProbe.KeyComparator.instance);
        return guideList;
    }

}
