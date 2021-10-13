package jsky.app.ot.editor.seq;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.shared.util.StringUtil;
import edu.gemini.spModel.config.MetaDataConfig;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.dataflow.GsaSequenceEditor;
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary;
import edu.gemini.spModel.gemini.ghost.Ghost;
import edu.gemini.spModel.gemini.seqcomp.smartgcal.SmartgcalSysConfig;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;

import javax.swing.table.AbstractTableModel;
import java.util.*;

import static jsky.app.ot.editor.seq.Keys.*;

/**
 * Table model that holds the values in the configuration that change over its
 * course.
 */
public final class DynamicSequenceTableModel extends AbstractTableModel {
    public static final ItemKey[] SORT_ORDER = new ItemKey[] {
            DATALABEL_KEY,
            OBS_CLASS_KEY,
            TEL_P_KEY,
            TEL_Q_KEY,
            OBS_EXP_TIME_KEY,
            OBS_COADDS_KEY,
    };

    private ConfigSequence _sequence;
    private ItemKey[] _iteratedKeys;

    private SPNodeKey _nodeKey;

    public void setSequence(final ConfigSequence sequence, final SPNodeKey key, final List<ItemKey> alwaysShow) {
        _sequence     = sequence;
        _nodeKey      = key;

        final ItemKey[] newIteratedKeys = sort(getIteratedKeys(sequence, alwaysShow));
        if (Arrays.equals(newIteratedKeys, _iteratedKeys)) {
            fireTableDataChanged();
        } else {
            _iteratedKeys = newIteratedKeys;
            fireTableStructureChanged();
        }
    }

    private Set<ItemKey> getIteratedKeys(final ConfigSequence seq, final List<ItemKey> alwaysShow) {
        final Set<ItemKey> res = new HashSet<>();
        final ItemKey[]   keys = seq.getIteratedKeys();
        for (ItemKey key : keys) {
            // Strip out all the smart gcal metadata.
            if (MetaDataConfig.MATCHER.matches(key) || SmartgcalSysConfig.MATCHER.matches(key)) continue;

            // Add the remainder for now.
            res.add(key);
        }

        // Add any items that the caller wishes to always see.
        if (alwaysShow.size() > 0) {
            for (ItemKey templ : alwaysShow) {
                for (ItemKey key : seq.getStaticKeys()) {
                    if (templ.matches(key)) res.add(key);
                }
            }
        }

        // Make sure the data label and obs class are present, since they are
        // stipulated by SCT-123 to always be the first two columns.
        res.add(DATALABEL_KEY);
        res.add(OBS_CLASS_KEY);

        // Take out the calibration class, since it could be confusing and
        // doesn't add any information that the observing class won't add.
        res.remove(CAL_CLASS_KEY);
        // don't show the basecal values (night / day)
        res.remove(CalDictionary.BASECAL_DAY_ITEM.key);
        res.remove(CalDictionary.BASECAL_NIGHT_ITEM.key);

        // Remove the complete / ready flag since we'll be color coding based
        // on this item.
        res.remove(OBS_STATUS_KEY);

        // Remove this confusing value which we use internally
        res.remove(OBS_ELAPSED_KEY);

        // Remove the duplicate instrument exposure time and instrument coadd
        // keys.  The "instrument" system exposure time represents the exposure
        // time configured in the instrument but it is overridden for bias steps
        // etc.  The important parameter for the user is the "observe" system
        // exposure time which is derived from the "instrument" precursor so it.
        res.remove(INST_EXP_TIME_KEY);
        res.remove(INST_COADDS_KEY);

        // GHOST complicates exposure time settings in that it has its own
        // values, though they work in a similar way to the normal instrument
        // exposure time.  Accordingly, we don't want to see the "instrument"
        // system exposure time parameters for Ghost if they are there.
        res.remove(Ghost.RED_EXPOSURE_COUNT_KEY());
        res.remove(Ghost.RED_EXPOSURE_TIME_KEY());
        res.remove(Ghost.BLUE_EXPOSURE_COUNT_KEY());
        res.remove(Ghost.BLUE_EXPOSURE_TIME_KEY());

        // Take out the proprietary months, which isn't important for sequence
        // planning.
        res.remove(GsaSequenceEditor.PROPRIETARY_MONTHS_KEY);

        // Now, if telescope:p or telescope:q is present, then make sure the
        // other is present as well. Otherwise, the table seems incomplete.
        if (res.contains(TEL_P_KEY) || res.contains(TEL_Q_KEY)) {
            res.add(TEL_P_KEY);
            res.add(TEL_Q_KEY);
        }

        return res;
    }

    private ItemKey[] sort(final Set<ItemKey> keys) {
        final List<ItemKey> res = new ArrayList<>(keys.size());

        // First put in the keys from the sort order that are present in the
        // iterated keys set.
        for (ItemKey key : SORT_ORDER) {
            if (keys.remove(key)) {
                res.add(key);
            }
        }

        // Now sort everything that's left by name.
        final List<ItemKey> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);

        // Now we're left with the everything whose order isn't fixed.  Split
        // them up by path prefix.  In other words, group the instrument items,
        // telescope items, etc.
        final Map<String, List<ItemKey>> groupedItems = new HashMap<>();
        for (final ItemKey key : sortedKeys) {

            // Get the broad category for the item -- the root parent
            ItemKey parent = key;
            ItemKey tmp    = parent.getParent();
            while (tmp != null) {
                parent = tmp;
                tmp    = parent.getParent();
            }

            // Add it to the list for that parent.
            List<ItemKey> lst = groupedItems.get(parent.getName());
            if (lst == null) {
                lst = new ArrayList<>();
                groupedItems.put(parent.getName(), lst);
            }
            lst.add(key);
        }

        // Put in the instrument items first.
        List<ItemKey> lst = groupedItems.remove("instrument");
        if (lst != null) res.addAll(lst);

        // Next the telescope items.
        lst = groupedItems.remove("telescope");
        if (lst != null) res.addAll(lst);

        // Next the calibration items.
        lst = groupedItems.remove("calibration");
        if (lst != null) res.addAll(lst);

        // Now what ever is remaining.
        for (final String category : groupedItems.keySet()) {
            res.addAll(groupedItems.get(category));
        }

        return res.toArray(new ItemKey[res.size()]);
    }

    public int getRowCount() {
        if (_sequence == null) return 0;
        return _sequence.size();
    }

    public int getColumnCount() {
        if (_iteratedKeys == null) return 0;
        return _iteratedKeys.length;
    }

    private boolean isGcal(final Config config) {
        final String type = (String) config.getItemValue(OBS_TYPE_KEY);
        return InstConstants.FLAT_OBSERVE_TYPE.equals(type) ||
               InstConstants.ARC_OBSERVE_TYPE.equals(type);
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
        if (_sequence == null) return null;
        if (_sequence.size() == 0) return null;

        final Config config = _sequence.getStep(rowIndex);
        final ItemKey   key = _iteratedKeys[columnIndex];
        if (key.getParent() == null) return null;
        if (!isGcal(config) && "calibration".equals(key.getParent().toString())) return "";
        Object    val = config.getItemValue(key);

        if (OBS_CLASS_KEY.equals(key)) {
            String classStr = (String) val;
            // Make the Obs Class show up in a more reasonable way
            val = (classStr == null) ? null : ObsClass.parseType(classStr);
        }

        return val;
    }

    public ItemKey getItemKeyAt(final int columnIndex) {
        if ((_iteratedKeys == null) || (_iteratedKeys.length == 0)) return null;
        return _iteratedKeys[columnIndex];
    }

    public Class<?> getColumnClass(final int columnIndex) {
        return Object.class; // sorry, the columns are dynamically generated
    }

    public String getColumnName(final int columnIndex) {
        if (_iteratedKeys == null) return null;
        final ItemKey key = _iteratedKeys[columnIndex];
        final String path = key.getPath();

        final String name  = StringUtil.toDisplayName(key.getName());
        final String res   = path.startsWith("calibration") ? "Cal " + name : name;
        // returning an html snippet allows for column headers with multiple lines
        return "<html>" + res.replaceFirst(" ", "<br/>") + "</html>";
    }

    public boolean isComplete(final int step) {
        if (_sequence == null) return false;
        final Object val = _sequence.getItemValue(step, OBS_STATUS_KEY);
        return "complete".equals(val);
    }

    public boolean isError(final int step) {
        if (_sequence == null) return false;
        final Object val = _sequence.getItemValue(step, SmartgcalSysConfig.MAPPING_ERROR_KEY);
        return Boolean.TRUE.equals(val);
    }

    public boolean matchesNodeId(final int step) {
        if (_nodeKey == null) return true;
        final Object val = _sequence.getItemValue(step, SP_NODE_KEY);
        final List<?> keys = (List<?>) val;
        return (keys == null) || keys.contains(_nodeKey);
    }
}
