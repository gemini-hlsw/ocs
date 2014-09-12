//
// $Id: DocumentImpl.java 4908 2004-08-06 18:44:01Z shane $
//
package edu.gemini.spModel.pio.xml;

import java.util.*;

import edu.gemini.spModel.pio.*;

final class DocumentImpl extends PioNodeParentImpl implements Document {

    DocumentImpl(DocumentElement element) {
        super(element);
    }

    DocumentElement getDocumentElement() {
        return (DocumentElement) getElement();
    }

    public void accept(PioVisitor visitor) {
        visitor.visitDocument(this);
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
        return new PioPath("/");
    }
}
