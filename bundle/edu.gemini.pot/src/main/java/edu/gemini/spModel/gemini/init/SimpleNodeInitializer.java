package edu.gemini.spModel.gemini.init;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.ISPDataObject;

import java.util.function.Supplier;

/**
 * An ISPNodeInitializer that simply sets the data object for newly created
 * nodes.
 */
public final class SimpleNodeInitializer<N extends ISPNode, D extends ISPDataObject> implements ISPNodeInitializer<N, D> {

    public final SPComponentType componentType;
    public final Supplier<D>     dataObject;

    public SimpleNodeInitializer(
            SPComponentType componentType,
            Supplier<D>     dataObject
    ) {
        this.componentType = componentType;
        this.dataObject    = dataObject;
    }

    @Override
    public SPComponentType getType() {
        return componentType;
    }

    @Override
    public D createDataObject() {
        return dataObject.get();
    }
}
