//
// $Id$
//

package jsky.app.ot.editor.seq;

import edu.gemini.spModel.config.MetaDataConfig;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.gemini.seqcomp.smartgcal.SmartgcalSysConfig;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Table model that holds the values in the configuration whose values never
 * change.
 */
public class StaticConfigurationTableModel extends AbstractTableModel {

    private enum Col {
        item("Item") {
            public Object extractValue(ItemKey key, Config config) {
                return key.getPath();
            }
        },
        value("Value") {
            public Object extractValue(ItemKey key, Config config) {
                return config.getItemValue(key);
            }
        },
        ;

        private String _displayValue;

        Col(String displayValue) {
            _displayValue = displayValue;
        }

        public String getDisplayValue() {
            return _displayValue;
        }

        public abstract Object extractValue(ItemKey key, Config config);
    }

    private Config _config;
    private ItemKey[] _staticKeys;

    public void setSequence(ConfigSequence sequence, List<ItemKey> neverShow) {
        if (sequence.size() > 0) {
            _config = sequence.getStep(0);
        } else {
            _config = null;
        }

        List<ItemKey> keys = new ArrayList<ItemKey>(Arrays.asList(sequence.getStaticKeys()));
        for (ListIterator<ItemKey> lit = keys.listIterator(); lit.hasNext(); ) {
            ItemKey key = lit.next();
            if (MetaDataConfig.MATCHER.matches(key) || SmartgcalSysConfig.MATCHER.matches(key)) {
                lit.remove();
            } else {
                for (ItemKey templ : neverShow) if (templ.matches(key)) lit.remove();
            }
        }

        _staticKeys = keys.toArray(new ItemKey[keys.size()]);
        Arrays.sort(_staticKeys);

        fireTableStructureChanged();
    }

    public int getRowCount() {
        if (_staticKeys == null) return 0;
        return _staticKeys.length;
    }

    public String getColumnName(int columnIndex) {
        return Col.values()[columnIndex].getDisplayValue();
    }

    public int getColumnCount() {
        return Col.values().length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (_staticKeys == null) return null;
        ItemKey key = _staticKeys[rowIndex];
        return Col.values()[columnIndex].extractValue(key, _config);
    }

    public ItemKey getItemKeyAt(int row) {
        return _staticKeys[row];
    }
}
