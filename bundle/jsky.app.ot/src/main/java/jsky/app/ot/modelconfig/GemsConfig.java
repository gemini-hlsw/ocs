package jsky.app.ot.modelconfig;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray;
import edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.Config.Direction;
import edu.gemini.spModel.telescope.IssPort;

import static edu.gemini.spModel.gemini.gsaoi.GsaoiDetectorArray.Config.Direction.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies GeMS configuration options.
 * As this applies to directions and the Canopus patrol field is symmetric, we don't need to do anything for it.
 */
public enum GemsConfig implements ConfigApply {
    instance;

    private static final Logger LOG = Logger.getLogger(GemsConfig.class.getName());

    public static final String ODGW_UP            = "odgwUp";
    public static final String ODGW_SIDE          = "odgwSide";
    public static final String ODGW_DIRECTION_KEY = "direction";
    public static final String ODGW_TOP_LEFT_KEY  = "topLeft";

    private static final Map<String, Direction> m;

    static {
        Map<String, GsaoiDetectorArray.Config.Direction> t = new HashMap<>();
        t.put("clockwise", clockwise);
        t.put("counterclockwise", counterClockwise);
        t.put("cw", clockwise);
        t.put("cc", counterClockwise);
        t.put("counter", counterClockwise);
        t.put("counter clockwise", counterClockwise);
        m = Collections.unmodifiableMap(t);
    }

    private static Option<Direction> parseDirection(String str) {
        return ImOption.apply(m.get(str.toLowerCase()));
    }

    private static Option<Direction> getDirection(String key, ModelConfig config) {
        Option<String> dirOpt = config.get(key + "." + ODGW_DIRECTION_KEY);
        return dirOpt.flatMap(GemsConfig::parseDirection);
    }

    private static Option<GsaoiDetectorArray.Id> parseId(Integer i) {
        for (GsaoiDetectorArray.Id id : GsaoiDetectorArray.Id.values()) {
            if (id.index() == i) return new Some<>(id);
        }
        return None.instance();
    }

    private static Option<GsaoiDetectorArray.Id> getId(String key, ModelConfig config) {
        return config.getInteger(key + "." + ODGW_TOP_LEFT_KEY).flatMap(GemsConfig::parseId);
    }

    private void setOdgw(String key, ModelConfig config) {
        Option<Direction> dirOpt = getDirection(key, config);
        if (dirOpt.isEmpty()) return;
        Option<GsaoiDetectorArray.Id> idOpt = getId(key, config);
        if (idOpt.isEmpty()) return;

        Direction dir = dirOpt.getValue();
        GsaoiDetectorArray.Id id = idOpt.getValue();

        IssPort port = ODGW_SIDE.equals(key) ? IssPort.SIDE_LOOKING : IssPort.UP_LOOKING;
        String msg  = String.format("Set ODGW %s: top left = %d, direction = %s", port.displayValue(), id.index(), dir.name());
        LOG.log(Level.INFO, msg);

        GsaoiDetectorArray.Config c = new GsaoiDetectorArray.Config(id, dir);
        GsaoiDetectorArray.setQuadrantConfig(port, c);
    }

    @Override public void apply(ModelConfig config) {
        setOdgw(ODGW_SIDE, config);
        setOdgw(ODGW_UP,   config);
    }
}
