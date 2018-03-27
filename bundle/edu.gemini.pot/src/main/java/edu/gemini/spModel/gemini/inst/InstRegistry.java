package edu.gemini.spModel.gemini.inst;

import static edu.gemini.pot.sp.SPComponentBroadType.INSTRUMENT;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.init.NodeInitializers;
import edu.gemini.spModel.obscomp.SPInstObsComp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides access to all known instruments.
 */
public enum InstRegistry {
    instance;

    private final Set<SPComponentType> types =
       Collections.unmodifiableSet(
           NodeInitializers.instance.obsComp.keySet().stream()
                   .filter(c -> c.broadType == INSTRUMENT)
                   .collect(Collectors.toSet())
       );

    public Set<SPComponentType> types() {
        return types;
    }

    public Collection<SPInstObsComp> prototypes() {
        return NodeInitializers.instance.obsComp.values().stream()
                .map(ini -> (SPInstObsComp) ini.createDataObject())
                .collect(Collectors.toList());
    }

    public Option<SPInstObsComp> prototype(String narrowType) {
        final SPComponentType t = SPComponentType.getInstance(INSTRUMENT, narrowType);
        return ImOption.apply(NodeInitializers.instance.obsComp.get(t))
                  .map(ini -> (SPInstObsComp) ini.createDataObject());
    }

    public Map<SPComponentType, Collection<ISPDataObject>> friends() {
        return Collections.emptyMap();
    }
}
