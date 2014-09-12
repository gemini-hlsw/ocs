package edu.gemini.spModel.gemini.calunit.calibration;

import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Diffuser;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Filter;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Lamp;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Shutter;
import edu.gemini.spModel.gemini.seqcomp.smartgcal.SmartgcalSysConfig;

import java.util.*;

import static edu.gemini.spModel.gemini.calunit.CalUnitConstants.*;
import static edu.gemini.spModel.obscomp.InstConstants.*;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.CALIBRATION_CONFIG_NAME;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.OBSERVE_CONFIG_NAME;

/**
 * CalDictionary defines the properties that apply to calibration steps.
 */
public final class CalDictionary {
    private CalDictionary() {}

    public static final String  CAL_SYS = CALIBRATION_CONFIG_NAME;
    public static final ItemKey CAL_KEY = new ItemKey(CAL_SYS);
    public static final String  OBS_SYS = OBSERVE_CONFIG_NAME;
    public static final ItemKey OBS_KEY = new ItemKey(OBS_SYS);

    /** Converts a calibration object to a String. */
    public interface Show { String apply(Object value); }

    public static final Show SHOW_TO_STRING = new Show() { public String apply(Object value) { return value.toString(); }};
    public static final Show SHOW_ENUM_NAME = new Show() { public String apply(Object value) { return ((Enum) value).name(); }};

    /** Converts a String returned by Show to a calibration object. */
    public interface Read { Object apply(String str); }

    public static final Read READ_BOOLEAN = new Read() { public Object apply(String str) { return Boolean.parseBoolean(str); }};
    public static final Read READ_DOUBLE  = new Read() { public Object apply(String str) { return Double.parseDouble(str); }};
    public static final Read READ_INTEGER = new Read() { public Object apply(String str) { return Integer.parseInt(str); }};
    public static final Read READ_STRING  = new Read() { public Object apply(String str) { return str; }};

    /** Extracts the value of an item from an indexed calibrtion step. */
    public interface Extract { Object apply(IndexedCalibrationStep that); }

    /**
     * Whether the property is fundamental or derived from other properties.
     */
    public enum PropertyKind {
        derived,
        fundamental,
    }

    public enum DataAspect {
        normal,
        meta
    }

    /** Defines a property of a calibration step. */
    public static final class Item {
        public final ItemKey key;
        public final String  propName;
        public final Show show;
        public final Read read;
        public final Extract ext;
        public final PropertyKind kind;
        public final DataAspect aspect;

        Item(String propName, Show show, Read read, Extract ext) {
            this(new ItemKey(CAL_KEY, propName), show, read, ext, PropertyKind.fundamental);
        }

        Item(ItemKey key, Show show, Read read, Extract ext, PropertyKind kind) {
            this(key, show, read, ext, kind, DataAspect.normal);
        }

        Item(ItemKey key, Show show, Read read, Extract ext, PropertyKind kind, DataAspect aspect) {
            this.key      = key;
            this.propName = key.getName();
            this.show     = show;
            this.read     = read;
            this.ext      = ext;
            this.kind     = kind;
            this.aspect   = aspect;
        }
    }

    public static final Item LAMP_ITEM = new Item(
       LAMP_PROP,
       new Show() { public String apply(Object value) {return Lamp.show((Collection<Lamp>) value, Lamp.NAME_MAPPER);} },
       new Read() { public Object apply(String str) { return Lamp.read(str); } },
       new Extract() { public Object apply(IndexedCalibrationStep that) { return that.getLamps(); } }
    );

    public static final Item SHUTTER_ITEM = new Item(
       SHUTTER_PROP,
       SHOW_ENUM_NAME,
       new Read() { public Object apply(String str) { return Shutter.getShutter(str); } },
       new Extract() { public Object apply(IndexedCalibrationStep that) { return that.getShutter(); } }
    );

    public static final Item FILTER_ITEM = new Item(
       FILTER_PROP,
       SHOW_ENUM_NAME,
       new Read() { public Object apply(String str) { return Filter.getFilter(str); } },
       new Extract() { public Object apply(IndexedCalibrationStep that) { return that.getFilter(); } }
    );

    public static final Item DIFFUSER_ITEM = new Item(
       DIFFUSER_PROP,
       SHOW_ENUM_NAME,
       new Read() { public Object apply(String str) { return Diffuser.getDiffuser(str); } },
       new Extract() { public Object apply(IndexedCalibrationStep that) { return that.getDiffuser(); } }
    );

    public static final Item EXPOSURE_TIME_ITEM = new Item(
       EXPOSURE_TIME_PROP,
       SHOW_TO_STRING,
       READ_DOUBLE,
       new Extract() { public Object apply(IndexedCalibrationStep that) { return that.getExposureTime(); } }
    );

    public static final Item COADDS_ITEM = new Item(
       COADDS_PROP,
       SHOW_TO_STRING,
       READ_INTEGER,
       new Extract() { public Object apply(IndexedCalibrationStep that) { return that.getCoadds(); } }
    );

    public static final Item BASECAL_DAY_ITEM = new Item(
       BASECAL_DAY_PROP,
       SHOW_TO_STRING,
       READ_BOOLEAN,
       new Extract() { public Object apply(IndexedCalibrationStep that) { return that.isBasecalDay(); } }
    );

    public static final Item BASECAL_NIGHT_ITEM = new Item(
       BASECAL_NIGHT_PROP,
       SHOW_TO_STRING,
       READ_BOOLEAN,
       new Extract() { public Object apply(IndexedCalibrationStep that) { return that.isBasecalNight(); } }
    );

    public static final Item STEP_COUNT_ITEM = new Item(
      SmartgcalSysConfig.STEP_KEY,
      SHOW_TO_STRING,
      READ_INTEGER,
      new Extract() { public Object apply(IndexedCalibrationStep that) { return that.getIndex(); } },
      PropertyKind.fundamental,
      DataAspect.meta
    );

    public static final Item OBS_CLASS_ITEM = new Item(
      new ItemKey(OBS_KEY, OBS_CLASS_PROP),
      SHOW_TO_STRING,
      READ_STRING,
      new Extract() { public Object apply(IndexedCalibrationStep that) {
          return that.getObsClass();
      }},
      PropertyKind.fundamental
    );

    public static final Item OBS_TYPE_ITEM = new Item(
      new ItemKey(OBS_KEY, OBSERVE_TYPE_PROP),
      SHOW_TO_STRING,
      READ_STRING,
      new Extract() { public Object apply(IndexedCalibrationStep that) {
          return that.isFlat() ? FLAT_OBSERVE_TYPE : ARC_OBSERVE_TYPE;
      }},
      PropertyKind.derived
    );

    public static final Item OBS_EXPOSURE_TIME_ITEM = new Item(
      new ItemKey(OBS_KEY, EXPOSURE_TIME_PROP),
      SHOW_TO_STRING,
      READ_DOUBLE,
      new Extract() { public Object apply(IndexedCalibrationStep that) { return that.getExposureTime(); } },
      PropertyKind.derived
    );

    public static final Item OBS_COADDS_ITEM = new Item(
      new ItemKey(OBS_KEY, COADDS_PROP),
      SHOW_TO_STRING,
      READ_INTEGER,
      new Extract() { public Object apply(IndexedCalibrationStep that) { return that.getCoadds(); } },
      PropertyKind.derived
    );

    public static final Item OBS_OBJECT_ITEM = new Item(
      new ItemKey(OBS_KEY, OBJECT_PROP),
      SHOW_TO_STRING,
      READ_STRING,
      new Extract() { public Object apply(IndexedCalibrationStep that) {
          Set<Lamp> lamps = that.getLamps();
          return lamps == null ? "" : Lamp.show(lamps, Lamp.TCC_MAPPER);
      }},
      PropertyKind.derived
    );

    public static final Collection<Item> ITEMS = Arrays.asList(
      LAMP_ITEM,
      SHUTTER_ITEM,
      FILTER_ITEM,
      DIFFUSER_ITEM,
      EXPOSURE_TIME_ITEM,
      COADDS_ITEM,
      BASECAL_DAY_ITEM,
      BASECAL_NIGHT_ITEM,
      STEP_COUNT_ITEM,
      OBS_CLASS_ITEM,
      OBS_TYPE_ITEM,
      OBS_EXPOSURE_TIME_ITEM,
      OBS_COADDS_ITEM,
      OBS_OBJECT_ITEM
    );

    /** ItemKeys for all systems that are used to configure a calibration. */
    public static final Set<ItemKey> SYSTEM_KEYS = new HashSet<ItemKey>() {{
        for (Item item : ITEMS) add(item.key.getParent());
    }};

    /** The subset of all items that are fundamental properties of a calibration step. */
    public static final Collection<Item> FUNDAMENTAL_ITEMS = new ArrayList<Item>() {{
        for (Item item : ITEMS) if (item.kind == PropertyKind.fundamental) add(item);
    }};

    /** The subset of all items that are derived from fundamental properties. */
    public static final Collection<Item> DERIVED_ITEMS = new ArrayList<Item>() {{
        for (Item item : ITEMS) if (item.kind == PropertyKind.derived) add(item);
    }};
}
