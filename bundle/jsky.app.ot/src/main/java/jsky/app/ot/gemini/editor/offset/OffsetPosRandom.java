package jsky.app.ot.gemini.editor.offset;

import jsky.util.gui.NumberBoxWidget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Random;

import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;

/**
 * A panel used to create a grid of offset positions.
 */
public final class OffsetPosRandom<P extends OffsetPosBase> extends JPanel {

    private static final String INSTRUCTIONS =
            "Configure a set of random offset positions to append to the " +
            "list. Initial and max offset are specified in arcsecs.";

    private static final int ROW_PAD =  5;
    private static final int COL_PAD = 10;

    private NumberBoxWidget pInitOffset = new NumberBoxWidget();
    private NumberBoxWidget qInitOffset = new NumberBoxWidget();
    private NumberBoxWidget maxOffset   = new NumberBoxWidget();
    private NumberBoxWidget steps      = new NumberBoxWidget();

    private static class Dialog<P extends OffsetPosBase> extends JDialog {
        private OffsetPosList<P> posList;
        private OffsetPosRandom<P> rand;
        private JButton createButton = new JButton("Create");
        private JButton cancelButton = new JButton("Cancel");

        private Dialog(Frame owner, OffsetPosList<P> posList, double maxOffsetValue) {
            super(owner, true);
            setTitle("Random Offset Positions");
            rand = new OffsetPosRandom<P>(maxOffsetValue);
            setResizable(false);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            this.posList = posList;

            createButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    rand.append(Dialog.this.posList);
                    Dialog.this.setVisible(false);
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Dialog.this.setVisible(false);
                }
            });

            JPanel content = new JPanel(new BorderLayout(10, 10)) {{
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 1, 1, Color.GRAY),
                        BorderFactory.createEmptyBorder(20, 5, 5, 5)));
                add(rand, BorderLayout.CENTER);
                add(new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0)) {{
                    setBorder(
                       BorderFactory.createCompoundBorder(
                               BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                               BorderFactory.createEmptyBorder(10, 0, 0, 0)));
                    add(createButton);
                    add(cancelButton);
                }}, BorderLayout.SOUTH);
            }};
            setContentPane(content);
            pack();
        }
    }

    public static <P extends OffsetPosBase> void showDialog(Frame frame, OffsetPosList<P> posList, double maxOffsetValue) {
        Dialog<P> dialog = new Dialog<P>(frame, posList, maxOffsetValue);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    public OffsetPosRandom(double maxOffsetValue) {
        super(new GridBagLayout());

        addInstructions();

        int headerRow = 1;
        int pRow = 2;
        int qRow = 3;

        add(new JLabel("Initial Offset"), createCellGBC( 1, false, headerRow, false));
        add(new JLabel("p"),              createLabelGBC(0, pRow, false));
        add(new JLabel("q"),              createLabelGBC(0, qRow, true));

        pInitOffset.setColumns(6);
        pInitOffset.setText("0.0");

        qInitOffset.setColumns(6);
        qInitOffset.setText("0.0");

        add(pInitOffset, createCellGBC(1, false, pRow, false));
        add(qInitOffset, createCellGBC(1, false, qRow, true));

        add(new JLabel("Number of Positions"), createLabelGBC(2, pRow, false));
        add(new JLabel("Max Offset"),          createLabelGBC(2, qRow, true));

        steps.setColumns(6);
        steps.setText("10");

        maxOffset.setColumns(6);
        maxOffset.setText(String.valueOf(maxOffsetValue));

        add(steps,     createCellGBC(3, false, pRow, false));
        add(maxOffset, createCellGBC(3, false, qRow, true));
    }

    private void addInstructions() {
        GridBagConstraints gbc = new GridBagConstraints();

        JTextArea ta = new JTextArea(INSTRUCTIONS);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(0, 0, 10, 0)
        ));
        ta.setEditable(false);
        Font f = ta.getFont();
        ta.setFont(f.deriveFont(f.getSize2D() - 2.0f));
        ta.getPreferredSize();
        ta.setPreferredSize(new Dimension(0, 40));

        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 5;
        gbc.gridx     = 0;
        gbc.gridy     = 0;
        gbc.weightx   = 1.0;
        gbc.insets    = new Insets(0, 0, 10, 0);
        add(ta, gbc);
    }

    private GridBagConstraints createLabelGBC(int col, int row, boolean lastRow) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor    = GridBagConstraints.EAST;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.gridx     = col;
        gbc.gridy     = row;
        gbc.insets    = new Insets(0, 0, lastRow ? 0 : ROW_PAD, 5);
        return gbc;
    }

    private GridBagConstraints createCellGBC(int col, boolean lastCol, int row, boolean lastRow) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor    = GridBagConstraints.CENTER;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.gridx     = col;
        gbc.gridy     = row;
        gbc.insets    = new Insets(0, 0, lastRow ? 0 : ROW_PAD, lastCol ? 0 : COL_PAD);
        return gbc;
    }

    private double getPInitOffset() {
        return pInitOffset.getDoubleValue(0.0);
    }

    private double getQInitOffset() {
        return qInitOffset.getDoubleValue(0.0);
    }

    private int getSteps() {
        return steps.getIntegerValue(1);
    }

    private double getMaxOffset() {
        return maxOffset.getDoubleValue(0.0);
    }

    public void append(OffsetPosList<P> offsetList) {
        double pBase = getPInitOffset();
        double qBase = getQInitOffset();

        int steps = getSteps();
        double maxOffset = getMaxOffset();

        Random rand = new Random();
        for (int i=0; i<steps; ++i) {
            // Get 2 random values between -1 and 1
            double dp = rand.nextDouble() * 2.0 - 1;
            double dq = rand.nextDouble() * 2.0 - 1;

            double p = pBase + dp * maxOffset;
            double q = qBase + dq * maxOffset;

            // chop off excess digits of precision
            p = ((int) ((p * 100.) + 0.5)) / 100.;
            q = ((int) ((q * 100.) + 0.5)) / 100.;

            offsetList.addPosition(p, q);
        }
    }
}
