package jsky.app.ot.gemini.editor.offset;

import edu.gemini.shared.gui.MultilineLabel;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

final class AdvancedGuiderSelectionEditor<P extends OffsetPosBase> {
    public static final String INSTRUCTIONS =
        "Select any guiders for which the default guiding selection should be overridden.";

    private static final JComponent NOTE = new MultilineLabel(INSTRUCTIONS) {{
        setForeground(Color.DARK_GRAY);
        setFont(getFont().deriveFont(getFont().getSize2D()-1));
    }};
    private static final Border THIN_BORDER = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY);

    private Set<GuideProbe> available = Collections.emptySet();
    private OffsetPosList<P> posList;

    final JPanel pan;
    private final JPanel guiderPanel;

    AdvancedGuiderSelectionEditor() {
        guiderPanel = new JPanel(new GridBagLayout()) {{
            setOpaque(false);
        }};

        pan = new JPanel(new GridBagLayout()) {{
            setOpaque(false);
            add(NOTE, new GridBagConstraints() {{
                gridx=0; gridy=0; weightx=1.0; anchor=WEST; fill=HORIZONTAL;
            }});
            add(new JPanel() {{setOpaque(false); setBorder(THIN_BORDER);}}, new GridBagConstraints() {{
                gridx=0; gridy=1; weightx=1.0; fill=HORIZONTAL; insets=new Insets(10,0,0,0);
            }});
            add(guiderPanel, new GridBagConstraints() {{
                gridx=0; gridy=2; weightx=1.0; anchor=WEST;
            }});
            add(new JPanel() {{ setOpaque(false); }}, new GridBagConstraints() {{
                gridx=0; gridy=3; weighty=1.0; fill=VERTICAL;
            }});
        }};
    }


    void setAvailableGuiders(Set<GuideProbe> available) {
        this.available = Collections.unmodifiableSet(new HashSet<GuideProbe>(available));
        update();
    }

    void setPosList(OffsetPosList<P> posList) {
        this.posList = posList;
        update();
    }

    private void update() {
        if (posList == null) return;

        guiderPanel.removeAll();

        final Set<GuideProbe> advProbes = posList.getAdvancedGuiding();
        final Set<GuideProbe> allProbes = new TreeSet<GuideProbe>(GuideProbe.KeyComparator.instance);
        allProbes.addAll(available);
        allProbes.addAll(advProbes);

        int row = 0;
        for (final GuideProbe probe : allProbes) {
            JCheckBox box = new JCheckBox(probe.getKey()) {{
                setOpaque(false);
                setSelected(advProbes.contains(probe));
                addChangeListener(new ChangeListener() {
                    @Override public void stateChanged(ChangeEvent evt) {
                        if (isSelected()) {
                            posList.addAdvancedGuiding(probe);
                        } else {
                            posList.removeAdvancedGuiding(probe);
                        }
                    }
                });
            }};
            final GridBagConstraints gbc = new GridBagConstraints() {{
                fill=NONE; anchor=WEST; insets=new Insets(10, 0, 0, 0);
            }};
            gbc.gridy = row++;
            guiderPanel.add(box, gbc);
        }
    }
}
