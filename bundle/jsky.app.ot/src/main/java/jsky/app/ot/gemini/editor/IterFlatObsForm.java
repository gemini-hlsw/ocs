package jsky.app.ot.gemini.editor;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.obsclass.ObsClass;
import jsky.util.gui.DropDownListBoxWidget;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import java.awt.*;

/**
 * @author Allan Brighton
 */
public class IterFlatObsForm extends JPanel {
    public IterFlatObsForm() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        JLabel label1 = new JLabel();
        JPanel panel1 = new JPanel();
        lamp1 = new JRadioButton();
        lamp2 = new JRadioButton();
        lamp3 = new JRadioButton();
        JLabel label2 = new JLabel();
        JPanel panel2 = new JPanel();
        arc1 = new JCheckBox();
        arc2 = new JCheckBox();
        arc3 = new JCheckBox();
        arc4 = new JCheckBox();
        JLabel label3 = new JLabel();
        shutter = new DropDownListBoxWidget<>();
        JLabel label4 = new JLabel();
        filter = new JComboBox<>();
        JLabel label5 = new JLabel();
        diffuser = new DropDownListBoxWidget<>();
        JLabel label6 = new JLabel();
        repeatSpinner = new JSpinner();
        JLabel label7 = new JLabel();
        exposureTime = new TextBoxWidget();
        JLabel label72 = new JLabel();
        JLabel label8 = new JLabel();
        coadds = new TextBoxWidget();
        JLabel label82 = new JLabel();
        JLabel label42 = new JLabel();
        obsClass = new DropDownListBoxWidget<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setLayout(new FormLayout(
            new ColumnSpec[] {
                new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                ColumnSpec.decode("max(pref;50dlu)"),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.FILL, Sizes.PREFERRED, FormSpec.DEFAULT_GROW),
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                FormFactory.PREF_COLSPEC,
                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX7, FormSpec.NO_GROW)
            },
            new RowSpec[] {
                FormFactory.PARAGRAPH_GAP_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.LINE_GAP_ROWSPEC,
                new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
            }));
        ((FormLayout)getLayout()).setRowGroups(new int[][] {{5, 25}});

        //---- label1 ----
        label1.setText("Lamp");
        add(label1, cc.xy(3, 7));

        //======== panel1 ========
        {
            panel1.setLayout(new FormLayout(
                new ColumnSpec[] {
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.UNRELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.UNRELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC
                },
                RowSpec.decodeSpecs("default")));

            //---- lamp1 ----
            lamp1.setText("text");
            lamp1.setSelected(true);
            panel1.add(lamp1, cc.xy(1, 1));

            //---- lamp2 ----
            lamp2.setText("text");
            panel1.add(lamp2, cc.xy(3, 1));

            //---- lamp3 ----
            lamp3.setText("text");
            panel1.add(lamp3, cc.xy(5, 1));
        }
        add(panel1, cc.xywh(5, 7, 5, 1));

        //---- label2 ----
        label2.setText("Arcs");
        add(label2, cc.xy(3, 9));

        //======== panel2 ========
        {
            panel2.setLayout(new FormLayout(
                new ColumnSpec[] {
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.UNRELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.UNRELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.UNRELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.UNRELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC
                },
                RowSpec.decodeSpecs("default")));

            //---- arc1 ----
            arc1.setText("text");
            panel2.add(arc1, cc.xy(1, 1));

            //---- arc2 ----
            arc2.setText("text");
            panel2.add(arc2, cc.xy(3, 1));

            //---- arc3 ----
            arc3.setText("text");
            panel2.add(arc3, cc.xy(5, 1));

            //---- arc4 ----
            arc4.setText("text");
            arc4.setHorizontalAlignment(SwingConstants.LEADING);
            panel2.add(arc4, cc.xy(7, 1));
        }
        add(panel2, cc.xywh(5, 9, 5, 1));

        //---- label3 ----
        label3.setText("Shutter");
        add(label3, cc.xy(3, 11));
        add(shutter, cc.xy(5, 11));

        //---- label4 ----
        label4.setText("Filter");
        add(label4, cc.xy(3, 13));
        add(filter, cc.xy(5, 13));

        //---- label5 ----
        label5.setText("Diffuser");
        add(label5, cc.xy(3, 15));
        add(diffuser, cc.xy(5, 15));

        //---- label6 ----
        label6.setText("Observe");
        add(label6, cc.xy(3, 17));

        //---- repeatSpinner ----
        repeatSpinner.setMinimumSize(new Dimension(80, 20));
        repeatSpinner.setOpaque(false);
        repeatSpinner.setPreferredSize(new Dimension(80, 20));
        add(repeatSpinner, cc.xy(5, 17));

        //---- label7 ----
        label7.setText("Exposure Time");
        add(label7, cc.xy(3, 19));

        //---- exposureTime ----
        exposureTime.setMinimumSize(new Dimension(80, 20));
        exposureTime.setPreferredSize(new Dimension(80, 20));
        add(exposureTime, cc.xy(5, 19));

        //---- label72 ----
        label72.setText("(sec)");
        add(label72, cc.xy(7, 19));

        //---- label8 ----
        label8.setText("Coadds");
        add(label8, cc.xy(3, 21));

        //---- coadds ----
        coadds.setMinimumSize(new Dimension(80, 20));
        coadds.setPreferredSize(new Dimension(80, 20));
        add(coadds, cc.xy(5, 21));

        //---- label82 ----
        label82.setText("(exp / obs)");
        add(label82, cc.xy(7, 21));

        //---- label42 ----
        label42.setText("Class");
        add(label42, cc.xywh(7, 3, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        add(obsClass, cc.xywh(9, 3, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    JRadioButton lamp1;
    JRadioButton lamp2;
    JRadioButton lamp3;
    JCheckBox arc1;
    JCheckBox arc2;
    JCheckBox arc3;
    JCheckBox arc4;
    DropDownListBoxWidget<CalUnitParams.Shutter> shutter;
    JComboBox<CalUnitParams.Filter> filter;
    DropDownListBoxWidget<CalUnitParams.Diffuser> diffuser;
    JSpinner repeatSpinner;
    TextBoxWidget exposureTime;
    TextBoxWidget coadds;
    DropDownListBoxWidget<ObsClass> obsClass;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
