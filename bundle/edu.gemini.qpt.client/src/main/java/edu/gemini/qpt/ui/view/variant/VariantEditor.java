package edu.gemini.qpt.ui.view.variant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Conds;
import edu.gemini.qpt.core.util.ApproximateAngle;

@SuppressWarnings({"serial"})
class VariantEditor extends JDialog {

    private String variantName;
    private Conds variantConditions;
    private ApproximateAngle variantWindConstraint;
    private Boolean variantLgsConstraint;
    private int result = CANCEL_OPTION;

    private static final int OK_OPTION = JOptionPane.OK_OPTION;
    private static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;

    private JTextField text, angle, variance;
    private JComboBox<Byte> iq, cc, wv;
    private JCheckBox windConstraint;
    private JCheckBox lgsConstraint;

    VariantEditor(Frame owner) {
        super(owner, "Edit Variant", true);
        setResizable(false);

        setBackground(Color.LIGHT_GRAY);
        setLayout(new BorderLayout());

        add(new JPanel(new BorderLayout(8, 8)) {{
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            add(new JPanel(new BorderLayout()) {{
                add(new JLabel("Name: "), BorderLayout.WEST);
                add(text = new JTextField(), BorderLayout.CENTER);
            }}, BorderLayout.NORTH);


            class CondComboBox extends JComboBox<Byte> {
                CondComboBox(Byte... vals) {
                    super(vals);
                    setRenderer(new DefaultListCellRenderer() {
                        @Override
                        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                            JLabel ret = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                            if (value.equals((byte) 0)) {
                                ret.setText("-");
                            } else if (value.equals((byte) 100)) {
                                ret.setText("\u00ABAny\u00BB");
                            }
                            return ret;
                        }
                    });
                }

            }

            add(new JPanel(new GridLayout(5, 2)) {{
                add(new JLabel("Image Quality: ", SwingConstants.RIGHT)); add(iq = new CondComboBox((byte) 0, (byte) 20, (byte) 70, (byte) 85, (byte) 100));
                add(new JLabel("Cloud Cover: ", SwingConstants.RIGHT));   add(cc = new CondComboBox((byte) 0, (byte) 20, (byte) 50, (byte) 70, (byte) 80, (byte) 100));
                add(new JLabel("Water Vapor: ", SwingConstants.RIGHT));   add(wv = new CondComboBox((byte) 0, (byte) 20, (byte) 50, (byte) 80, (byte) 100));

                add(windConstraint = new JCheckBox("Wind Constraint: ") {{
                    addActionListener(e -> {
                        angle.setEnabled(isSelected());
                        variance.setEnabled(isSelected());
                    });
                }});

                 class NumericField extends JTextField {

                     NumericField(int cols) {
                         super(cols);
                         setEnabled(false);
                     }

                     protected Document createDefaultModel() {
                         return new NumericDocument();
                     }

                     class NumericDocument extends PlainDocument {

                         public void insertString(int offs, String s, AttributeSet a) throws BadLocationException {
                             if (s == null) return;
                             StringBuilder sb = new StringBuilder();
                             for (int i = 0; i < s.length(); i++) {
                                 char c = s.charAt(i);
                                 if (Character.isDigit(c))
                                     sb.append(c);
                             }
                             super.insertString(offs, sb.toString(), a);
                         }
                     }
                 }

                add(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {{
                    add(angle = new NumericField(3));
                    add(new JLabel("\u00b0\u00b1"));
                    add(variance = new NumericField(3));
                    variance.setText(Integer.toString(Variant.DEFAULT_WIND_CONSTRAINT_VARIANCE));
                    add(new JLabel("\u00b0"));
                }});

                add(lgsConstraint = new JCheckBox("LGS"));

            }}, BorderLayout.CENTER);

            add(new JPanel(new FlowLayout(FlowLayout.RIGHT)) {{
                add(new JButton("Ok") {{
                    VariantEditor.this.getRootPane().setDefaultButton(this);
                    addActionListener(e -> {
                        result = OK_OPTION;
                        VariantEditor.this.setVisible(false);
                    });
                }});
                add(new JButton("Cancel") {{
//                    NewDialog.this.getRootPane().set;
                    addActionListener(e -> {
                        result = CANCEL_OPTION;
                        VariantEditor.this.setVisible(false);
                    });
                }});
            }}, BorderLayout.SOUTH);

        }});

        pack();

    }

    @Override
    public void setVisible(boolean b) {
        setLocationRelativeTo(getParent());
        super.setVisible(b);
    }


    private int showEditor(String name, byte cc, byte wv, byte iq, ApproximateAngle wind, Boolean lgs) {
        text.setText(name);
        text.selectAll();
        this.cc.setSelectedItem(cc);
        this.wv.setSelectedItem(wv);
        this.iq.setSelectedItem(iq);

        if (wind != null) {
            this.windConstraint.setSelected(true);
            angle.setEnabled(true);
            variance.setEnabled(true);
            this.angle.setText(Integer.toString(wind.getAngle()));
            this.variance.setText(Integer.toString(wind.getVariance()));
        }

        if(lgs != null){
            lgsConstraint.setSelected(lgs);
        }

        setVisible(true);
        dispose();

        variantName = text.getText();
        variantConditions = new Conds(
            (byte) 0, // SB is unconstrained
            (Byte) this.cc.getSelectedItem(),
            (Byte) this.iq.getSelectedItem(),
            (Byte) this.wv.getSelectedItem()
            );

        if (windConstraint.isSelected()) {
            int a = Integer.valueOf("0" + angle.getText());
            int v = Integer.valueOf("0" + variance.getText());
            if (v > 180) v = 180;
            variantWindConstraint = new ApproximateAngle(a, v);
        }

        variantLgsConstraint = lgsConstraint.isSelected();

        return result;
    }

    int showEditor(Variant v) {
        return showEditor(v.getName(), v.getConditions().cc, v.getConditions().wv, v.getConditions().iq, v.getWindConstraint(), v.getLgsConstraint());
    }

    int showNew(String name, byte cc, byte wv, byte iq) {
        setTitle("Add Variant");
        return showEditor(name, cc, wv, iq, null, false);
    }

    Conds getVariantConditions() {
        return variantConditions;
    }

    String getVariantName() {
        return variantName;
    }

    ApproximateAngle getVariantWindConstraint() {
        return variantWindConstraint;
    }

    Boolean getVariantLgsConstraint() {
        return variantLgsConstraint;
    }

    protected JRootPane createRootPane() {
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane rootPane = new JRootPane();
        rootPane.registerKeyboardAction(e -> VariantEditor.this.setVisible(false), stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
    }

}
