package jsky.app.ot.gemini.visitor;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import jsky.app.ot.gemini.editor.ComponentEditor;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyDescriptor;

/**
 * User Interface for visitor instruments.
 */
public class VisitorEditor extends ComponentEditor<ISPObsComponent, VisitorInstrument> {
    private final JPanel pan = new JPanel(new GridBagLayout());

    private final TextFieldPropertyCtrl<VisitorInstrument, Double> expTimeCtrl;
    private final TextFieldPropertyCtrl<VisitorInstrument, Double> posAngleCtrl;
    private final TextFieldPropertyCtrl<VisitorInstrument, String> nameCtrl;
    private final TextFieldPropertyCtrl<VisitorInstrument, Double> wavelengthCtrl;
    private static final int gapCol        = 1;
    private static final int rightLabelCol = 1;
    private static final int rightWidgetCol= 2;
    private static final int rightUnitsCol = 3;
    private static final int colCount      = rightUnitsCol + 1;

    public VisitorEditor() {
        pan.setBorder(PANEL_BORDER);

        int row = 0;

        // Name
        PropertyDescriptor instrumentNameProp = VisitorInstrument.NAME_PROP;
        nameCtrl = TextFieldPropertyCtrl.createStringInstance(instrumentNameProp);
        nameCtrl.setColumns(30);

        nameCtrl.getTextField().addMouseListener(focusOnCaretPositionListener);
        pan.add(new JLabel(instrumentNameProp.getDisplayName()), propLabelGbc(rightLabelCol, row));
        pan.add(nameCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));

        ++row;

        // Exposure Time
        PropertyDescriptor exposureTimeProp = VisitorInstrument.EXPOSURE_TIME_PROP;
        expTimeCtrl = TextFieldPropertyCtrl.createDoubleInstance(exposureTimeProp, 1);
        expTimeCtrl.setColumns(30);
        expTimeCtrl.getTextField().addMouseListener(focusOnCaretPositionListener);

        pan.add(new JLabel("Exp Time"), propLabelGbc(rightLabelCol, row));
        pan.add(expTimeCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));
        pan.add(new JLabel("sec"), propUnitsGbc(rightUnitsCol, row));

        ++row;
        // Position Angle
        PropertyDescriptor positionAngleProp = VisitorInstrument.POS_ANGLE_PROP;
        posAngleCtrl = TextFieldPropertyCtrl.createDoubleInstance(positionAngleProp, 1);
        posAngleCtrl.setColumns(30);
        posAngleCtrl.getTextField().addMouseListener(focusOnCaretPositionListener);
        pan.add(new JLabel(positionAngleProp.getDisplayName()), propLabelGbc(rightLabelCol, row));
        pan.add(posAngleCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));
        pan.add(new JLabel("deg E of N"), propUnitsGbc(rightUnitsCol, row));

        ++row;
        // Wavelength
        PropertyDescriptor wavelengthAngleProp = VisitorInstrument.WAVELENGTH_PROP;
        wavelengthCtrl = TextFieldPropertyCtrl.createDoubleInstance(wavelengthAngleProp, 1);
        wavelengthCtrl.setColumns(30);
        wavelengthCtrl.getTextField().addMouseListener(focusOnCaretPositionListener);
        pan.add(new JLabel(wavelengthAngleProp.getDisplayName()), propLabelGbc(rightLabelCol, row));
        pan.add(wavelengthCtrl.getComponent(), propWidgetGbc(rightWidgetCol, row));
        pan.add(new JLabel("microns"), propUnitsGbc(rightUnitsCol, row));

        // Filler
        pan.add(new JPanel(), pushGbc(colCount, row + 1));
    }

    @Override
    public JPanel getWindow() {
        return pan;
    }

    @Override
    public void handlePostDataObjectUpdate(VisitorInstrument inst) {
        expTimeCtrl.setBean(inst);
        posAngleCtrl.setBean(inst);
        nameCtrl.setBean(inst);
        wavelengthCtrl.setBean(inst);
    }

}
