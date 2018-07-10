package edu.gemini.qpt.ui.view.property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Group;
import edu.gemini.qpt.shared.sp.Note;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.qpt.ui.view.property.adapter.AllocAdapter;
import edu.gemini.qpt.ui.view.property.adapter.GroupAdapter;
import edu.gemini.qpt.ui.view.property.adapter.MarkerAdapter;
import edu.gemini.qpt.ui.view.property.adapter.NoteAdapter;
import edu.gemini.qpt.ui.view.property.adapter.ObsAdapter;
import edu.gemini.qpt.ui.view.property.adapter.ProgAdapter;
import edu.gemini.qpt.ui.view.property.adapter.VariantAdapter;
import edu.gemini.ui.gface.GSelection;

@SuppressWarnings({ "unchecked", "serial" })
public class PropertyTable extends LinkedHashMap<String, Object> {

    private static final Map<Class<?>, Adapter<?>> adapters = new HashMap<>();
    private static final Object MULTIPLE_VALUES = new Object();

    public interface Adapter<T> {

        String PROP_TYPE = "Type/ID";
        String PROP_SUBTYPE = "Subtype";
        String PROP_TITLE = "Title";
        String PROP_SUNSET = "Sunset";
        String PROP_DUSK = "Evening 12˚ twilight"; // it doesn't fit with deg
        String PROP_DAWN = "Morning 12˚ twilight"; // it doesn't fit with deg
        String PROP_SUNRISE = "Sunrise";
        String PROP_REMAINING_PROGRAM_TIME = "Rem. Program Time";
        String PROP_FLAGS = "Flags";
        String PROP_COORDINATES = "Coordinates";
        String PROP_INSTRUMENT = "Instrument";
        String PROP_CONSTRAINTS = "Constraints";
        String PROP_WIND = "Wind";
        String PROP_TIMING = "Timing Windows";
        String PROP_LASER_TIMING = "Laser Timing Window";
        String PROP_WFS = "Wavefront Sensor";
        String PROP_SEQUENCE_FULL = "Sequence (Remaining)";
        String PROP_SEQUENCE_PLANNED = "Sequence (Planned)";
        String PROP_TIME_SLOT = "Time Slot";
        String PROP_AIRMASS = "Airmass";
        String PROP_ELEVATION = "Elevation";
        String PROP_HOUR_ANGLE = "Hour Angle";
        String PROP_PARALLACTIC_ANGLE = "Parallactic Angle";
        String PROP_LUNAR_RANGE = "Lunar Range";
        String PROP_TOTAL_BRIGHTNESS = "Sky Background";
        String TOTAL_BRIGHTNESS_NOT_INCLUDING_SETUP = "SB w/o Setup";

        void setProperties(Variant variant, T target, PropertyTable table);

    }

    static {
        adapters.put(Alloc.class, new AllocAdapter());
        adapters.put(Group.class, new GroupAdapter());
        adapters.put(Marker.class, new MarkerAdapter());
        adapters.put(Note.class, new NoteAdapter());
        adapters.put(Obs.class, new ObsAdapter());
        adapters.put(Prog.class, new ProgAdapter());
        adapters.put(Variant.class, new VariantAdapter());
    }

    private Map<String, Integer> count = new TreeMap<>(); // leaks
    private boolean initializing  = true;

    private PropertyTable() {
        // Add all the keys to set the insertion order.
        put(Adapter.PROP_TYPE, null);
        put(Adapter.PROP_SUBTYPE, null);
        put(Adapter.PROP_TITLE, null);
        put(Adapter.PROP_SUNSET, null);
        put(Adapter.PROP_DUSK, null);
        put(Adapter.PROP_DAWN, null);
        put(Adapter.PROP_SUNRISE, null);
        put(Adapter.PROP_REMAINING_PROGRAM_TIME, null);
        put(Adapter.PROP_FLAGS, null);
        put(Adapter.PROP_COORDINATES, null);
        put(Adapter.PROP_INSTRUMENT, null);
        put(Adapter.PROP_CONSTRAINTS, null);
        put(Adapter.PROP_TIMING, null);
        put(Adapter.PROP_LASER_TIMING, null);
        put(Adapter.PROP_WFS, null);
        put(Adapter.PROP_SEQUENCE_FULL, null);
        put(Adapter.PROP_SEQUENCE_PLANNED, null);
        put(Adapter.PROP_TIME_SLOT, null);
        put(Adapter.PROP_AIRMASS, null);
        put(Adapter.PROP_ELEVATION, null);
        put(Adapter.PROP_HOUR_ANGLE, null);
        put(Adapter.PROP_PARALLACTIC_ANGLE, null);
        put(Adapter.PROP_LUNAR_RANGE, null);
        put(Adapter.PROP_TOTAL_BRIGHTNESS, null);
        put(Adapter.TOTAL_BRIGHTNESS_NOT_INCLUDING_SETUP, null);
        initializing = false;
    }

    @Override
    public Object put(String key, Object value) {
        if (value == null && !initializing) throw new NullPointerException();
        if (!containsKey(key)) {
            count.put(key, initializing ? 0 : 1);
        } else {
            count.put(key, count.get(key) + 1);
            Object old = get(key);
            if (old != null && !value.equals(old))
                value = MULTIPLE_VALUES;
        }
        return super.put(key, value);
    }


    /**
     * Returns a property table for the specified selection. The keyset will be the
     * intersection of keys for all objects in the selection. Each value will be
     * null unless it is equal for all objects in the selection.
     * @param sel
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map<String, Object> forObjects(Variant v, GSelection<?> sel) {
        if (sel != null) {
            PropertyTable ret = new PropertyTable();
            for (Object o: sel) {
                Adapter a = adapters.get(o.getClass());
                if (a != null) {
                    a.setProperties(v, o, ret);
                } else {
                    return Collections.emptyMap();
                }
            }
            for (Iterator<Map.Entry<String, Object>> it = ret.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Object> e = it.next();
                if (e.getValue() == null || ret.count.get(e.getKey()) != sel.size()) it.remove();
                if (e.getValue() == MULTIPLE_VALUES) e.setValue(null);
            }
            return ret;
        } else {
            return Collections.emptyMap();
        }
    }


//    public static void main(String[] args) {
//        
//        adapters.put(String.class, new Adapter<String>() {
//        
//            public void setProperties(String target, PropertyTable table) {
//                table.put(PROP_TYPE, "String");
//                table.put("Value", target);
//                table.put("Length", target.length());
//            }
//        
//        });
//        
//        System.out.println(forObjects(new GSelection<Object>("foo", "bar", "bar")));
//        
//    }

}
