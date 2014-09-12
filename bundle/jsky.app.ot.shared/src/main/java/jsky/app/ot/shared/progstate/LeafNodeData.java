package jsky.app.ot.shared.progstate;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.shared.util.VersionVector;
import edu.gemini.spModel.data.ISPDataObject;

import java.io.Serializable;

/** A tuple (SPNodeKey, ISPTypedDataObject, VersionVector[UUID, Integer]) */
public final class LeafNodeData implements Serializable {
    public final SPNodeKey nodeKey;
    public final ISPDataObject dataObject;
    public final VersionVector<LifespanId, Integer> versions;

    public LeafNodeData(SPNodeKey nodeKey, ISPDataObject dataObject, VersionVector<LifespanId, Integer> versions) {
        if (nodeKey == null) throw new NullPointerException("nodeKey is null");
        if (dataObject == null) throw new NullPointerException("dataObject is null");
        if (versions == null) throw new NullPointerException("versions is null");
        this.nodeKey = nodeKey;
        this.dataObject = dataObject;
        this.versions = versions;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("LeafNodeData{");
        sb.append("nodeKey=").append(nodeKey);
        sb.append(", dataObject=").append(dataObject);
        sb.append(", versions=").append(versions);
        sb.append('}');
        return sb.toString();
    }

    // equals and hashCode not defined because there's no way to do it
    // correctly in general for data objects
}
