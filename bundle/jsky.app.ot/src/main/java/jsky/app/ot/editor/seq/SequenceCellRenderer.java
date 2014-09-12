//
// $Id$
//

package jsky.app.ot.editor.seq;

import static jsky.app.ot.util.OtColor.*;

import edu.gemini.shared.util.StringUtil;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.OptionTypeUtil;
import edu.gemini.spModel.type.DisplayableSpType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders table cells for the static table of information and serves as the
 * base for the dynamic step table renderer.
 */
abstract class SequenceCellRenderer extends DefaultTableCellRenderer {
    private static final Map<ItemKey, Color> COLOR_MAP = new HashMap<ItemKey, Color>();

    static {
        COLOR_MAP.put(new ItemKey("telescope"),   CANTALOUPE);
        COLOR_MAP.put(new ItemKey("instrument"),  HONEY_DEW);
        COLOR_MAP.put(new ItemKey("calibration"), BANANA);
    }

    protected static Color lookupColor(ItemKey key) {
        if (key == null) return SKY;
        Color res = COLOR_MAP.get(key);
        return (res == null) ? lookupColor(key.getParent()) : res;
    }

    protected abstract Color lookupColor(int row, int column);

    protected Color lookupTextColor(int row, int column) {
        return Color.BLACK;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel lab = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        lab.setHorizontalAlignment(SwingConstants.LEFT);

        String text = "";
        if (value instanceof DisplayableSpType) {
            text = ((DisplayableSpType) value).displayValue();
        } else if (value instanceof Number) {
            // Some of the sequence items are actually numbers (the way
            // god intended) instead of strings.  The problem is that they
            // get formatted differently than the majority which are
            // strings.  So make them get formatted like Strings anyway...
            lab.setHorizontalAlignment(SwingConstants.LEFT);
            text = value.toString();
        } else if (value instanceof Option) {
            text = OptionTypeUtil.toDisplayString((Option) value);
        } else if (value instanceof Collection) {
            text = StringUtil.mkString((Collection<?>) value, "", ",", "");
        } else if (value != null) {
            text = value.toString();
        }
        lab.setText(text);
        if (!isSelected) lab.setBackground(lookupColor(row, column));
        lab.setForeground(lookupTextColor(row, column));
        return lab;
    }
}
