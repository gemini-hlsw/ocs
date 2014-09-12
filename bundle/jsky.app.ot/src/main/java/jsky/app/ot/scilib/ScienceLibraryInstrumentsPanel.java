package jsky.app.ot.scilib;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

/**
 *
 */
final class ScienceLibraryInstrumentsPanel extends JPanel {


    private final List<SPComponentType> _selectedItems = new ArrayList<SPComponentType>();

    ScienceLibraryInstrumentsPanel() {
        setLayout(new GridBagLayout());
        final Map<SPComponentType, ScienceLibraryInfo> map = ScienceLibraryInfo.getLibraryInfoMap();

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        final Insets leftColumnInset = new Insets(0, 15, 5, 5);
        final Insets rightColumnInset = new Insets(0, 25, 5, 20);

        gbc.insets = new Insets(0, 15, 10, 15);
        JLabel label = new JLabel("North Instruments");


        add(label, gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 25, 10, 20);
        label = new JLabel("South Instruments");
        add(label, gbc);
        gbc.gridy++;


        //gbc.insets = new Insets(0,8,5,8);
        int rowNorth = gbc.gridy;
        int rowSouth = gbc.gridy;
        for (final SPComponentType instrument : map.keySet()) {
            final ScienceLibraryInfo instInfo = map.get(instrument);
            if (instInfo.getSite() == Site.GN) {
                gbc.gridx = 0;
                gbc.gridy = rowNorth;
                gbc.insets = leftColumnInset;
                rowNorth++;
            } else {
                gbc.gridx = 1;
                gbc.gridy = rowSouth;
                gbc.insets = rightColumnInset;
                rowSouth++;
            }
            final JCheckBox cb = new JCheckBox(instrument.readableStr);
            add(cb, gbc);
            _setListener(cb, instrument);
        }
    }

    Collection<SPComponentType> getSelectedItems() {
        return Collections.unmodifiableList(_selectedItems);
    }

    private void _setListener(final JCheckBox cb, final SPComponentType type) {
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent event) {
                if (event.getItemSelectable() != cb) {
                    return;
                }
                if (cb.isSelected()) {
                    _selectedItems.add(type);
                } else {
                    _selectedItems.remove(type);
                }
            }
        });
    }

}
