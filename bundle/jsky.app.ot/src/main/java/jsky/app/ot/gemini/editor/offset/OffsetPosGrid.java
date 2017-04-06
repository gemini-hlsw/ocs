package jsky.app.ot.gemini.editor.offset;

import jsky.util.gui.NumberBoxWidget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;

/**
 * A panel used to create a grid of offset positions.
 */
public final class OffsetPosGrid<P extends OffsetPosBase> extends JPanel {

    private static final String INSTRUCTIONS =
            "Configure a grid of offset positions to append to the list. " +
            "Initial offset and spacing are specified in arcsecs.";

    private static final int ROW_PAD =  5;
    private static final int COL_PAD = 10;

    private NumberBoxWidget pInitOffset = new NumberBoxWidget();
    private NumberBoxWidget qInitOffset = new NumberBoxWidget();
    private NumberBoxWidget pSpacing    = new NumberBoxWidget();
    private NumberBoxWidget qSpacing    = new NumberBoxWidget();
    private NumberBoxWidget pSteps      = new NumberBoxWidget();
    private NumberBoxWidget qSteps      = new NumberBoxWidget();

    private static class Dialog<P extends OffsetPosBase> extends JDialog {
        private OffsetPosList<P> posList;
        private OffsetPosGrid<P> grid;
        private JButton createButton = new JButton("Create");
        private JButton cancelButton = new JButton("Cancel");

        private Dialog(Frame owner, OffsetPosList<P> posList, double spacing) {
            super(owner, true);
            grid = new OffsetPosGrid<P>(spacing);
            setTitle("Offset Position Grid");
            setResizable(false);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            this.posList = posList;

            createButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    grid.append(Dialog.this.posList);
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
                add(grid, BorderLayout.CENTER);
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

    public static <P extends OffsetPosBase> void showDialog(Frame frame, OffsetPosList<P> posList, double spacing) {
        Dialog<P> dialog = new Dialog<P>(frame, posList, spacing);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    /**
     * Creates an offset grid pattern.
     * @param spacing the increment between steps
     */
    public OffsetPosGrid(double spacing) {
        super(new GridBagLayout());

        addInstructions();

        int headerRow = 1;
        int pRow = 2;
        int qRow = 3;

        int initCol  = 1;
        int spaceCol = 2;
        int stepsCol = 3;

        add(new JLabel("Initial Offset"), createCellGBC(initCol,  false, headerRow, false));
        add(new JLabel("Spacing"),        createCellGBC(spaceCol, false, headerRow, false));
        add(new JLabel("Steps"),          createCellGBC(stepsCol, true,  headerRow, false));

        add(new JLabel("p"),              createCellGBC(0, false, pRow, false));
        add(new JLabel("q"),              createCellGBC(0, false, qRow, true));

        pInitOffset.setColumns(6);
        pInitOffset.setText("0.0");
        qInitOffset.setColumns(6);
        qInitOffset.setText("0.0");

        pSpacing.setColumns(6);
        pSpacing.setText(String.valueOf(spacing));
        qSpacing.setColumns(6);
        qSpacing.setText(String.valueOf(spacing));

        pSteps.setColumns(3);
        pSteps.setText("3");
        qSteps.setColumns(3);
        qSteps.setText("3");

        add(pInitOffset,  createCellGBC(initCol,  false, pRow, false));
        add(qInitOffset,  createCellGBC(initCol,  false, qRow, true));
        add(pSpacing,     createCellGBC(spaceCol, false, pRow, false));
        add(qSpacing,     createCellGBC(spaceCol, false, qRow, true));
        add(pSteps,       createCellGBC(stepsCol, true,  pRow, false));
        add(qSteps,       createCellGBC(stepsCol, true,  qRow, true));


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
        ta.setPreferredSize(new Dimension(0, 50));

        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 5;
        gbc.gridx     = 0;
        gbc.gridy     = 0;
        gbc.weightx   = 1.0;
        gbc.insets    = new Insets(0, 0, 10, 0);
        add(ta, gbc);
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

    private double getPSpacing() {
        return pSpacing.getDoubleValue(0.0);
    }

    private double getQSpacing() {
        return qSpacing.getDoubleValue(0.0);
    }

    private int getPSteps() {
        return pSteps.getIntegerValue(1);
    }

    private int getQSteps() {
        return qSteps.getIntegerValue(1);
    }

    public void append(OffsetPosList<P> offsetList) {
        double p = getPInitOffset();
        double q = getQInitOffset();

        double pSpace = getPSpacing();
        double qSpace = getQSpacing();

        int sign = -1;
        for (int row=0; row<getQSteps(); ++row) {
            for (int col=0; col<getPSteps(); ++col) {
                offsetList.addPosition(p, q);
                p += sign*pSpace;
            }
            sign *= -1;
            p += sign*pSpace;
            q -= qSpace;
        }
    }
}
