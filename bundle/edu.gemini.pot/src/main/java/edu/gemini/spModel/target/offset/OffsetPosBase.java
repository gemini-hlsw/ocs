package edu.gemini.spModel.target.offset;

import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.ARCSECS;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.telescope.IssPort;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//
// I absolve responsibility for this class.  I split the old {@link OffsetPos}
// class into this base class in order to also extend it for
// {@link NiciOffsetPos} for SCT-232.
//

/**
 * A data object that describes an offset position and includes methods
 * for extracting positions.
 */
public abstract class OffsetPosBase extends WatchablePos {
    private static final Logger LOG = Logger.getLogger(OffsetPosBase.class.getName());
    public static final String DEFAULT_GUIDE_OPTION_PARAM = "defaultGuideOption";

    public interface Factory<P extends OffsetPosBase> extends Serializable {
        P create(String tag);
        P create(String tag, double p, double q);
    }

    // paramset tags
    public static final String P_PARAM = "p";
    public static final String Q_PARAM = "q";


    // The Stream Unique Identifier for this class.
    private static final long serialVersionUID = 2L;

    // The prefix of all Offset tags
    public static final String OFFSET_TAG = "Offset";

    /**
     * The x-axis offset in degrees.
     **/
    private double _xAxis;

    /**
     * The y-axis offset in degrees.
     **/
    private double _yAxis;

    // Default guide option
    private DefaultGuideOptions.Value defaultGuideOption = DefaultGuideOptions.Value.on;

    // Overridden guiding options for particular guiders, if any.
    private Map<GuideProbe, GuideOption> _linkMap = Collections.emptyMap();

    private final String _tag;


    /**
     * Create an OffsetPosBase with the given tag and coordinates
     */
    protected OffsetPosBase(String tag, double xAxis, double yAxis) {
        _tag = tag;
        _xAxis = xAxis;
        _yAxis = yAxis;
    }

    /**
     * Create an OffsetPosBase with the given tag and (0,0) coordinates.
     */
    protected OffsetPosBase(String tag) {
        this(tag, 0.0, 0.0);
    }

    /**
     * Creates an {@link Offset} from the information stored in this object.
     */
    public Offset toSkycalcOffset() {
        return new Offset(new Angle(_xAxis, ARCSECS), new Angle(_yAxis, ARCSECS));
    }

    /**
     * Override clone to make sure the position is correctly
     * initialized.
     */
    public Object clone() {
        OffsetPosBase off;
        try {
            off = (OffsetPosBase) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Should not happen
            throw new UnsupportedOperationException();
        }
        // _xAxis and _yAxis are immutable

        // no need to copy since the map is unmodifiable and contains
        // immutable data
//        off._linkMap = Collections.unmodifiableMap(new HashMap<GuideProbe, GuideOption>(_linkMap));
        return off;
    }

    // -------------- Link support.  Sets the guide options for each of the
    // used guide probes in the observation.

    public DefaultGuideOptions.Value getDefaultGuideOption() {
        return defaultGuideOption;
    }

    public void setDefaultGuideOption(DefaultGuideOptions.Value opt) {
        if (opt == null) throw new IllegalArgumentException("default guide option cannot be null");
        if (opt == defaultGuideOption) return;
        defaultGuideOption = opt;
        _notifyOfGenericUpdate();
    }

    public boolean isActive(GuideProbe guider) {
        GuideOption opt = _linkMap.get(guider);
        return (opt == null) ? defaultGuideOption.isActive() : opt.isActive();
    }

    public boolean isFrozen(GuideProbe guider) {
        GuideOption opt = _linkMap.get(guider);
        return (opt == null) ? !defaultGuideOption.isActive() : opt == StandardGuideOptions.Value.freeze;
    }

    /**
     * Add a link to this offset.
     * The id is used to store the value of the link.  If the id exists,
     * the value is replaced, otherwise, it is added.
     */
    public void setLink(GuideProbe guider, GuideOption option) {
        if (guider == null) {
            @SuppressWarnings({"ThrowableInstanceNeverThrown"}) Exception ex = new RuntimeException();
            LOG.log(Level.WARNING, "Set a null guider link", ex);
            return;
        }
        if (option == null) {
            @SuppressWarnings({"ThrowableInstanceNeverThrown"}) Exception ex = new RuntimeException();
            LOG.log(Level.WARNING, "Set a null guide option", ex);
            return;
        }

        // Remember the current value
        GuideOption prev = _linkMap.get(guider);
        if (prev == option) return;

        // Update the current value

        Map<GuideProbe, GuideOption> m = new TreeMap<GuideProbe, GuideOption>(GuideProbe.KeyComparator.instance);
        m.putAll(_linkMap);
        m.put(guider, option);
        _linkMap = Collections.unmodifiableMap(m);

        _notifyOfGenericUpdate();
    }

    /*
     * Test to see if a link id exists.
     */
    public boolean linkExists(GuideProbe probe) {
        return _linkMap.containsKey(probe);
    }

    /**
     * Get the link value for this id.  This returns null if the id
     * doesn't exist.
     */
    public GuideOption getLink(GuideProbe probe) {
        return _linkMap.get(probe);
    }

    /**
     * Get the link value for this id.  This returns def if the id
     * doesn't exist.
     */
    public GuideOption getLink(GuideProbe probe, GuideOption def) {
        GuideOption val = _linkMap.get(probe);
        return (val == null) ? def : val;
    }

    /**
     * Returns an unmodifiable map containing the guide option for each of the
     * guide probes in the observation.
     *
     * @return unmodifiable map
     */
    public Map<GuideProbe, GuideOption> getLinks() {
        return _linkMap;
    }

    public Set<GuideProbe> getGuideProbes() {
        return new HashSet<GuideProbe>(_linkMap.keySet());
    }

    public Set<GuideProbe> getGuideProbes(GuideProbe.Type type) {
        Set<GuideProbe> res = new HashSet<GuideProbe>();
        for (GuideProbe probe : _linkMap.keySet()) {
            if (probe.getType() == type) res.add(probe);
        }
        return res;
    }

    /**
     * Returns the number of links to this offset pos.
     */
    public int getLinkCount() {
        return _linkMap.size();
    }

    /**
     * Remove a link to this offset.
     * The id is used to look for a link in this offset position.  If
     * it exists, the id and value are removed.
     *
     * @return the existing link that was removed, if any; <code>null</code> if
     * not changed
     */
    public GuideOption removeLink(GuideProbe probe) {
        GuideOption res = _linkMap.get(probe);
        if (res == null) return null;

        if (_linkMap.size() == 1) {
            _linkMap = Collections.emptyMap();
        } else {
            Map<GuideProbe, GuideOption> newLinks = new TreeMap<GuideProbe, GuideOption>(GuideProbe.KeyComparator.instance);
            newLinks.putAll(_linkMap);
            newLinks.remove(probe);
            _linkMap = Collections.unmodifiableMap(newLinks);
        }
        _notifyOfGenericUpdate();
        return res;
    }

    /**
     * Remove all links.
     */
    public void removeAllLinks() {
        if (_linkMap.isEmpty()) return;

        _linkMap = Collections.emptyMap();
        _notifyOfGenericUpdate();
    }

    /**
     * Diagnostic routine to dump an offset's links.
     */
    public void dumpLinks() {
        StringBuilder buf = new StringBuilder();

        int size = getLinkCount();
        buf.append("Links - count: ").append(size).append("\n");
        if (size == 0) return;
        buf.append(_linkMap.toString());
        System.out.println(buf.toString());
    }

    /**
     * Is this an offset position (always true).  This method is part of
     * the TaggedPos and has to be overridden here to always return true.
     */
    public boolean isOffsetPosition() {
        return true;
    }

    /**
     * Get the x-axis (in arcsec).
     */
    public final synchronized double getXaxis() {
        return _xAxis;
    }

    /**
     * Get the y-axis (in arcsec).
     */
    public final synchronized double getYaxis() {
        return _yAxis;
    }

    /**
     * Get the x-axis as a String.
     */
    public synchronized String getXAxisAsString() {
        return Double.toString(_xAxis);
    }

    /**
     * Get the y-axis as a String.
     */
    public synchronized String getYAxisAsString() {
        return Double.toString(_yAxis);
    }

    public synchronized void setXAxis(double xAxis) {
        _xAxis = xAxis;
        _notifyOfLocationUpdate();
    }

    public synchronized void setYAxis(double yAxis) {
        _yAxis = yAxis;
        _notifyOfLocationUpdate();
    }

    /**
     * Allow setting x and y axes without notifying observers.  This can
     * come in handy when you want to do a bunch of updates in a row and
     * don't want to waste time notifying observers of each change.
     */
    public synchronized void noNotifySetXY(double xAxis, double yAxis, IssPort port) {
        _xAxis = xAxis;
        _yAxis = yAxis;
    }

    /**
     * Set the xAxis and the yAxis in arcsec.
     */
    public void setXY(double xAxis, double yAxis, IssPort port) {
        noNotifySetXY(xAxis, yAxis, port);
        _notifyOfLocationUpdate();
    }

    /**
     * Set the xAxis and the yAxis as Strings (arcsec).
     */
    public void setXY(String xAxis, String yAxis, IssPort port) {
        double x = 0.0;
        double y = 0.0;
        try {
            x = Double.valueOf(xAxis);
            y = Double.valueOf(yAxis);
        } catch (Exception ex) {
            // ignore
        }

        setXY(x, y, port);
    }

    /**
     * Compute the p and q offset that would result from a call to
     * {@link #setXY(double, double, IssPort)} with the given arguments.  Some offset
     * position subclasses constrain the legal p,q positions and will adjust the
     * provided location to meet the constraint.  By default, this method
     * returns a Point containing the coordinates passed in to the method.
     */
    public Point2D.Double computeXY(double xAxis, double yAxis, IssPort port) {
        return new Point2D.Double(xAxis, yAxis);
    }

    /**
     * Test to see if a tag is a valid OffsetPosBase tag.
     * @return true if the tag is valid, otherwise false.
     */
    public static boolean isValidTag(String tag) {
        return tag.startsWith(OFFSET_TAG);
    }

    private static final String LINK_PREFIX = "";

    /** Return a paramset describing this object */
    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet paramSet = factory.createParamSet(name);

        Pio.addParam(factory, paramSet, P_PARAM, getXAxisAsString());
        Pio.addParam(factory, paramSet, Q_PARAM, getYAxisAsString());

        Pio.addEnumParam(factory, paramSet, DEFAULT_GUIDE_OPTION_PARAM, defaultGuideOption);

        addLinkParameters(_linkMap, LINK_PREFIX, factory, paramSet);
        return paramSet;
    }

    private void addLinkParameters(Map<GuideProbe, GuideOption> map, String prefix, PioFactory factory, ParamSet paramSet) {
        for (Map.Entry<GuideProbe, GuideOption> me : map.entrySet()) {
            GuideProbe guider = me.getKey();
            GuideOption   opt = me.getValue();
            if (opt == null) opt = guider.getGuideOptions().getDefaultActive();
            Pio.addParam(factory, paramSet, prefix + guider.getKey(), opt.name());
        }
    }

    // Initialize this object from the given paramset
    public void setParamSet(ParamSet paramSet) {
        _xAxis = Double.parseDouble(Pio.getValue(paramSet, P_PARAM));
        _yAxis = Double.parseDouble(Pio.getValue(paramSet, Q_PARAM));

        defaultGuideOption = Pio.getEnumValue(paramSet, DEFAULT_GUIDE_OPTION_PARAM, DefaultGuideOptions.Value.on);

        Map<GuideProbe, GuideOption> links = new TreeMap<GuideProbe, GuideOption>(GuideProbe.KeyComparator.instance);
        setLinkParameters(links, LINK_PREFIX, paramSet);
        if (links.size() == 0) {
            _linkMap = Collections.emptyMap();
        } else {
            _linkMap = Collections.unmodifiableMap(links);
        }
    }

    public static void setLinkParameters(Map<GuideProbe, GuideOption> map, String prefix, ParamSet paramSet) {
        @SuppressWarnings({"unchecked"})
        List<Param> params = paramSet.getParams();

        for (Param param : params) {
            String key = param.getName();
            if (key.startsWith(prefix)) key = key.substring(prefix.length());

            GuideProbe probe = GuideProbeMap.instance.get(key);
            // Not all parameters represent guide probes, so skip it if this
            // doesn't parse into a GuideProbe.
            if (probe == null) continue;

            GuideOption opt;
            try {
                opt = probe.getGuideOptions().parse(param.getValue());
            } catch (Exception ex) {
                LOG.log(Level.INFO, "Could not parse guide option: " + param.getValue(), ex);
                continue;
            }
            map.put(probe, opt);
        }
    }

    /**
     * Extracts a Skycalc {@link Offset} from the given configuration.
     *
     * @return an {@link Offset} as expressed in the given <code>config</code>,
     * if any
     */
    public static Option<Offset> extractSkycalcOffset(IConfig config) {
        // Fish out the telescope config info, if any.
        ISysConfig tel = config.getSysConfig(SeqConfigNames.TELESCOPE_CONFIG_NAME);
        if (tel == null) return None.instance();

        // Extract the p and q offset positions, if any.
        String pStr = (String) tel.getParameterValue(OffsetPosBase.P_PARAM);
        if (pStr == null) return None.instance();
        String qStr = (String) tel.getParameterValue(OffsetPosBase.Q_PARAM);
        if (qStr == null) return None.instance();

        // Convert the string values into doubles, if possible.
        double p, q;
        try {
            p = Double.parseDouble(pStr);
            q = Double.parseDouble(qStr);
        } catch (NumberFormatException ex) {
            String msg = String.format("Could not parse offset (%s,%s).", pStr, qStr);
            LOG.log(Level.WARNING, msg, ex);
            return None.instance();
        }

        return new Some<Offset>(new Offset(new Angle(p, ARCSECS), new Angle(q, ARCSECS)));
    }

    public static final ItemKey TEL_P_KEY       = new ItemKey("telescope:p");
    public static final ItemKey TEL_Q_KEY       = new ItemKey("telescope:q");

    private static Double parsePqVal(Object val) {
        if (val instanceof Double) {
            return (Double) val;
        } else {
            return Double.parseDouble(val.toString());
        }

    }
    public static Option<Offset> extractSkycalcOffset(Config config) {
        Object p = config.getItemValue(TEL_P_KEY);
        Object q = config.getItemValue(TEL_Q_KEY);
        if ((p == null) || (q == null)) return None.instance();

        Double pd, qd = null;
        try {
            pd = parsePqVal(p);
            qd = parsePqVal(q);
        } catch (NumberFormatException ex) {
            String msg = String.format("Could not parse offset value (%s,%s).", p,q);
            LOG.log(Level.WARNING, msg, ex);
            return None.instance();
        }

        return new Some<Offset>(new Offset(new Angle(pd, ARCSECS), new Angle(qd, ARCSECS)));
    }

    /**
     * Return a string representation of this object for debugging.
     */
    public synchronized String toString() {
        String links = "";
        if (!_linkMap.isEmpty()) links = " " + _linkMap.toString();
        return getClass().getName() +
                "[tag=" + getTag() + ", xAxis=" + _xAxis + ", yAxis=" + _yAxis +
                links + "]";
    }

    public String getTag() {
        return _tag;
    }

}