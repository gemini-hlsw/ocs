package edu.gemini.obslog.obslog;

import java.util.logging.Logger;
import java.util.HashMap;

import scala.Option;

public class ConfigMap extends HashMap<String,Object> {
    private static final Logger LOG = Logger.getLogger(ConfigMap.class.getName());

    // Display the contents of the map for diagnostic purposes
    public void dump() {
        forEach((k,v) -> LOG.info(k + ':' + v));
    }

    public String sget(final String key) {
        return (String)get(key);
    }

    public Option<String> sgetOpt(final String key) {
        return Option.apply((String)get(key));
    }
}
