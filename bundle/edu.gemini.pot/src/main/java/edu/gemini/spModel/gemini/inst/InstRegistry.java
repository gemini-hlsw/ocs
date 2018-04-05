package edu.gemini.spModel.gemini.inst;

import static edu.gemini.pot.sp.SPComponentBroadType.INSTRUMENT;
import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.init.NodeInitializers;
import edu.gemini.spModel.obscomp.SPInstObsComp;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides access to all known instruments.
 */
public enum InstRegistry {
    instance;

    private Stream<SPComponentType> componentTypeStream() {
        return Arrays.asList(Instrument.values()).stream().map(i -> i.componentType);
    }

    public Set<SPComponentType> types() {
        return componentTypeStream().collect(Collectors.toSet());
    }

    public Set<SPInstObsComp> prototypes() {
        return componentTypeStream()
                .flatMap(t ->ImOption.apply(NodeInitializers.instance.obsComp.get(t)).toStream())
                .map(ini -> (SPInstObsComp) ini.createDataObject())
                .collect(Collectors.toSet());
    }

    public Option<SPInstObsComp> prototype(String narrowType) {
        final SPComponentType t = SPComponentType.getInstance(INSTRUMENT, narrowType);
        return ImOption.apply(NodeInitializers.instance.obsComp.get(t))
                  .map(ini -> (SPInstObsComp) ini.createDataObject());
    }

}
