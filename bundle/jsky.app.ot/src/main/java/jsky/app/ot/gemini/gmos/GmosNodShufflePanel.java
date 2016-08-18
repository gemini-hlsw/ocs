package jsky.app.ot.gemini.gmos;

import jsky.app.ot.util.OtColor;
import jsky.util.gui.Resources;
import jsky.util.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 * Holds nod and shuffle UI widgets for the GMOS editor N&S tab.  These were
 * taken from the (out of control) form designer form and copied here to be
 * manually layed out.
 *
 * NOTE: NOT YET USED...
 */
final class GmosNodShufflePanel extends JPanel {

    static final class SelectedPosPanel extends JPanel {
        final NumberBoxWidget xOffset = new NumberBoxWidget() {{
            setColumns(4);
            setToolTipText("The nod offset from the base position RA in arcsec");
        }};
        final NumberBoxWidget yOffset = new NumberBoxWidget() {{
            setColumns(4);
            setToolTipText("The nod offset from the base position Dec in arcsec");
        }};
        final DropDownListBoxWidget oiwfsBox = new DropDownListBoxWidget() {{
            setToolTipText("The OIWFS setting to use for the selected nod offset");
        }};

        SelectedPosPanel() {
            super(new GridBagLayout());
            addLabel("p",     0); addEditor(xOffset,  0);
            addLabel("q",     1); addEditor(yOffset,  1);
            addLabel("OIWFS", 2); addEditor(oiwfsBox, 2);
        }

        private Insets insets(int row) { return new Insets(row==0?0:5,0,0,0); }

        private void addLabel(String text, final int row) {
            add(new JLabel(text), new GridBagConstraints() {{
                gridx=0; gridy=row; anchor=EAST; insets=insets(row);
            }});
        }

        private void addEditor(JComponent comp, final int row) {
            add(comp, new GridBagConstraints() {{
                gridx=1; gridy=row; anchor=WEST; fill=HORIZONTAL; insets=insets(row);
            }});
        }
    }

    static final class TableButtonPanel extends JPanel {
        final JButton newButton = new JButton(Resources.getIcon("eclipse/add.gif")) {{
            setToolTipText("Add a new nod offset position");
        }};
        final JButton removeButton = new JButton(Resources.getIcon("eclipse/remove.gif")) {{
            setToolTipText("Remove the selected nod offset position");
        }};
        final JButton removeAllButton = new JButton(Resources.getIcon("eclipse/remove_all.gif")) {{
            setToolTipText("Remove all nod offset positions");
        }};
        final JCheckBox electronicOffsetCheckBox = new JCheckBox("Use Electronic Offsetting?") {{
            setToolTipText("Enable or disable electronic offsetting");
        }};

        private static final Insets INSETS = new Insets(0,0,0,5);

        TableButtonPanel() {
            super(new GridBagLayout());

            addButton(newButton,       0);
            addButton(removeButton,    1);
            addButton(removeAllButton, 2);

            add(new JPanel(), new GridBagConstraints() {{
                gridx=3; gridy=0; weightx=1.0; fill=HORIZONTAL;
            }});

            add(electronicOffsetCheckBox, new GridBagConstraints() {{
                gridx=4; gridy=0;
            }});
        }

        private void addButton(JButton btn, final int col) {
            add(btn, new GridBagConstraints() {{
                gridx=col; gridy=0; insets=INSETS;
            }});
        }
    }

    static final class OffsetAndCyclePanel extends JPanel {
        final NumberBoxWidget shuffleOffset = new NumberBoxWidget() {{
            setToolTipText("The shuffle offset in arcsec");
            setColumns(4);
        }};
        final NumberBoxWidget detectorRows = new NumberBoxWidget() {{
            setToolTipText("The shuffle offset in detector rows");
            setColumns(4);
        }};
        final NumberBoxWidget numNSCycles = new NumberBoxWidget() {{
            setToolTipText("The number of nod & shuffle cycles");
            setColumns(4);
        }};
        final JLabel totalTime = new JLabel() {{
            setToolTipText("The total observe time in seconds");
            setForeground(Color.black);
        }};

        OffsetAndCyclePanel() {
            super(new GridBagLayout());

            addLabel("Offset (arcsec)",          0, 0);
            addLabel("Offset (detector rows)",   2, 0);
            addLabel("Number of N&S Cycles",     0, 1);
            addLabel("Total Observe Time (sec)", 2, 1);

            add(shuffleOffset, new GridBagConstraints() {{
                gridx=1; gridy=0; anchor=WEST;
            }});
            add(detectorRows, new GridBagConstraints() {{
                gridx=3; gridy=0; anchor=WEST;
            }});
            add(numNSCycles, new GridBagConstraints() {{
                gridx=1; gridy=1; anchor=WEST;
            }});
            add(totalTime, new GridBagConstraints() {{
                gridx=3; gridy=1; anchor=WEST;
            }});
        }

        private void addLabel(String text, final int x, final int y) {
            final Insets i = new Insets(0, x==0?0:5, y==0?5:0, 5);
            add(new JLabel(text), new GridBagConstraints() {{
                gridx=x; gridy=y; anchor=EAST; insets=i;
            }});
        }
    }

    final SelectedPosPanel selectedPosPanel    = new SelectedPosPanel();
    final TableButtonPanel tableButtonPanel    = new TableButtonPanel();
    final GmosOffsetPosTableWidget offsetTable = new GmosOffsetPosTableWidget();
    final OffsetAndCyclePanel cyclePanel       = new OffsetAndCyclePanel();

    // using the original name from the form...
    final JLabel warning2 = new JLabel("Warning") {{
        setForeground(OtColor.SALMON);
    }};

    public GmosNodShufflePanel() {
        super(new GridBagLayout());
        add(selectedPosPanel, new GridBagConstraints() {{
            gridx=0; gridy=0; fill=NONE; anchor=NORTH;
        }});

        final JScrollPane scrollPane = new JScrollPane(offsetTable);
        add(scrollPane, new GridBagConstraints() {{
            gridx=1; gridy=0; fill=BOTH; weightx=1.0; weighty=1.0; insets=new Insets(0, 5, 5, 0);
        }});
        add(tableButtonPanel, new GridBagConstraints() {{
            gridx=1; gridy=1; fill=HORIZONTAL; weightx=1.0;
        }});
        add(cyclePanel, new GridBagConstraints() {{
            gridx=1; gridy=2; anchor=WEST; insets=new Insets(10,0,0,0);
        }});
        add(warning2, new GridBagConstraints() {{
            gridx=0; gridy=3; gridwidth=2; anchor=WEST;
        }});
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("GMOS N&S");
        JPanel pan   = new GmosNodShufflePanel();
        frame.getContentPane().add(pan, BorderLayout.CENTER);
        frame.pack();
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }

}
