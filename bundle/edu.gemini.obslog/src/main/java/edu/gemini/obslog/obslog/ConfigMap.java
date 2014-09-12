package edu.gemini.obslog.obslog;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//
// Gemini Observatory/AURA
// $Id: ConfigMap.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class ConfigMap extends HashMap<String,Object> {
    private static final Logger LOG = Logger.getLogger(ConfigMap.class.getName());

    // Display the contents of the map for diag purposes
    public void dump() {
        Set<Map.Entry<String,Object>> keys = entrySet();

        for (Map.Entry<String,Object> e : keys) {
            LOG.log(Level.INFO, e.getKey() + ':' + e.getValue());
        }
    }

    public String sget(String key) {
        return (String)get(key);
    }

}
