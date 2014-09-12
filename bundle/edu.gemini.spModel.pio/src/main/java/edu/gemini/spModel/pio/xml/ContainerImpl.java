//
// $Id: ContainerImpl.java 15335 2008-10-30 15:26:53Z swalker $
//
package edu.gemini.spModel.pio.xml;

import edu.gemini.spModel.pio.*;

import java.util.List;

final class ContainerImpl extends PioNodeParentImpl implements Container {
    private static final String KEY_ATTR      = "key";
    private static final String KIND_ATTR     = "kind";
    private static final String SEQUENCE_ATTR = "sequence";
    private static final String SUBTYPE_ATTR  = "subtype";
    private static final String TYPE_ATTR     = "type";
    private static final String VERSION_ATTR  = "version";

    ContainerImpl(ContainerElement element) {
        super(element);
    }

    ContainerElement getContainerElement() {
        return (ContainerElement) getElement();
    }

    public void accept(PioVisitor visitor) {
        visitor.visitContainer(this);
    }

    public String getKind() {
        return getAttribute(KIND_ATTR);
    }

    public void setKind(String kind) {
        if (kind == null) throw new NullPointerException("kind may not be null");
        setAttribute(KIND_ATTR, kind);
    }

    public String getType() {
        return getAttribute(TYPE_ATTR);
    }

    public void setType(String type) {
        if (type == null) throw new NullPointerException("type may not be null");
        setAttribute(TYPE_ATTR, type);
    }

    public String getSubtype() {
        return getAttribute(SUBTYPE_ATTR);
    }

    public void setSubtype(String type) {
        setAttribute(SUBTYPE_ATTR, type);
    }

    public Version getVersion() {
        return Version.match(getAttribute(VERSION_ATTR));
    }

    public void setVersion(String version) {
        if (version == null) throw new NullPointerException("version may not be null");
        setAttribute(VERSION_ATTR, version);
    }

    public String getKey() {
        return getAttribute(KEY_ATTR);
    }

    public void setKey(String key) {
        setAttribute(KEY_ATTR, key);
    }

    public int getSequence() {
        return getAttributeAsInt(SEQUENCE_ATTR, -1);
    }

    public void setSequence(int sequence) {
        setAttribute(SEQUENCE_ATTR, sequence);
    }

    public int getParamSetCount() {
        return getChildCount(ParamSetElement.NAME);
    }

    public ParamSet getParamSet(String name) {
        return (ParamSet) getChild(ParamSetElement.NAME, name);
    }

    public List getParamSets() {
        return getChildren(ParamSetElement.NAME);
    }

    public List getParamSets(String name) {
        return getChildren(ParamSetElement.NAME, name);
    }

    public ParamSet lookupParamSet(PioPath path) {
        PioNode child = lookupNode(path);
        return (child instanceof ParamSet) ? (ParamSet) child : null;
    }

    public void addParamSet(ParamSet child) {
        addChild((PioNodeImpl) child);
    }

    public int getContainerCount() {
        return getChildCount(ContainerElement.NAME);
    }

    public Container getContainer(String name) {
        return (Container) getChild(ContainerElement.NAME, name);
    }

    public List getContainers() {
        return getChildren(ContainerElement.NAME);
    }

    public List getContainers(String name) {
        return getChildren(ContainerElement.NAME, name);
    }

    public Container lookupContainer(PioPath path) {
        PioNode child = lookupNode(path);
        return (child instanceof Container) ? (Container) child : null;
    }

    public void addContainer(Container child) {
        addChild((PioNodeImpl) child);
    }
    public PioPath getPath() {
        String name = getName();
        if (name == null) name = PioPath.anonymousContainer;
        return PioNamedNodeUtil.getPath(this, name);
    }

    public String getName() {
        return PioNamedNodeUtil.getName(this);
    }

    public void setName(String name) {
        PioNamedNodeUtil.setName(this, name);
    }
}
