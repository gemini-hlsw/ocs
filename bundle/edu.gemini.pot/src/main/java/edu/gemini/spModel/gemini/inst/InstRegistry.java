package edu.gemini.spModel.gemini.inst;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.util.POTUtil;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obscomp.SPInstObsComp;

import java.util.*;

/**
 * Provides access to all known instruments (as identified in
 * spdb-initializer.conf).
 */
public enum InstRegistry {
    instance;

    private <T> Collection<T> get(MapOp<InstNodeInitializer, T> op) {
        Map<String, ISPNodeInitializer> m = POTUtil.getInitializerMap();

        List<T> res = new ArrayList<T>();
        for (ISPNodeInitializer init : m.values()) {
            if (init instanceof InstNodeInitializer) {
                res.add(op.apply((InstNodeInitializer) init));
            }
        }
        return res;
    }

    private static final MapOp<InstNodeInitializer, SPComponentType> TO_TYPE =
        new MapOp<InstNodeInitializer, SPComponentType>() {
            @Override public SPComponentType apply(InstNodeInitializer in) { return in.getType(); }
        };

    public Collection<SPComponentType> types() { return get(TO_TYPE); }

    private static final MapOp<InstNodeInitializer, SPInstObsComp> TO_DATAOBJ =
        new MapOp<InstNodeInitializer, SPInstObsComp>() {
            @Override public SPInstObsComp apply(InstNodeInitializer in) { return in.createDataObject(); }
        };

    public Collection<SPInstObsComp> prototypes() { return get(TO_DATAOBJ); }

    public Option<SPInstObsComp> prototype(String narrowType) {
        for (ISPNodeInitializer init : POTUtil.getInitializerMap().values()) {
            if (init instanceof InstNodeInitializer) {
                InstNodeInitializer ini = ((InstNodeInitializer) init);
                SPComponentType t = ini.getType();
                if (t.narrowType.equals(narrowType)) {
                    return new Some<SPInstObsComp>(ini.createDataObject());
                }
            }
        }
        return None.instance();
    }

    public Map<SPComponentType, Collection<ISPDataObject>> friends() {
        Map<String, ISPNodeInitializer> m = POTUtil.getInitializerMap();
        Map<SPComponentType, Collection<ISPDataObject>> res = new HashMap<SPComponentType, Collection<ISPDataObject>>();

        for (ISPNodeInitializer init : m.values()) {
            if (init instanceof InstNodeInitializer) {
                InstNodeInitializer ini = (InstNodeInitializer) init;
                res.put(ini.getType(), ini.createFriends());
            }
        }
        return Collections.unmodifiableMap(res);
    }
}
