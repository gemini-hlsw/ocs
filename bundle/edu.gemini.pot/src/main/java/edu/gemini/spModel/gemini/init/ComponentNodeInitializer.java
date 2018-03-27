package edu.gemini.spModel.gemini.init;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.data.ISPDataObject;

import java.util.function.Function;
import java.util.function.Supplier;


/**
 * An ISPNodeInitializer implementation that works for many observation or
 * sequence component nodes.  In addition to setting the data object for new
 * nodes, it adds the IConfigBuilder user object so that the node can
 * participate in sequence construction.
 */
public final class ComponentNodeInitializer<N extends ISPNode, D extends ISPDataObject> implements ISPNodeInitializer<N, D> {

    public final SPComponentType                       componentType;
    public final Supplier<D>                           dataObject;
    public final Function<N, ? extends IConfigBuilder> configBuilder;

    public ComponentNodeInitializer(
            SPComponentType                       componentType,
            Supplier<D>                           dataObject,
            Function<N, ? extends IConfigBuilder> configBuilder
    ) {
        this.componentType = componentType;
        this.dataObject    = dataObject;
        this.configBuilder = configBuilder;
    }

    @Override
    public SPComponentType getType() {
        return componentType;
    }

    @Override
    public D createDataObject() {
        return dataObject.get();
    }

    @Override
    public void updateNode(N node) {
        node.putClientData(IConfigBuilder.USER_OBJ_KEY, configBuilder.apply(node));
    }
}
