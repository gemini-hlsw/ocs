package jsky.app.ot.gemini.gmos;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.ObsoletableSpType;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;

/**
 * Class GmosDetectorManufacturerComboBoxRenderer
 *
 * @author Nicolas A. Barriga
 *         Date: 4/25/11
 */
public class GmosDetectorManufacturerComboBoxRenderer extends BasicComboBoxRenderer implements ListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList jList, Object value, int index, boolean isSelected, boolean hasFocus) {
        String text = value.toString();
        if (value instanceof DisplayableSpType) {
            text = ((DisplayableSpType) value).displayValue();
            //This is a hack for the OT browser to show "E2V North/South" and the OT instrument component to just show "E2V"
            if(text.startsWith("E2V")){
                text="E2V";
            }
        }
        if (value instanceof ObsoletableSpType) {
            ObsoletableSpType otype = (ObsoletableSpType) value;
            if (otype.isObsolete()) {
                text = text + "*";
            }
        }
        return super.getListCellRendererComponent(jList, text, index, isSelected, hasFocus);
    }
}
