//
// $Id: ContainerElement.java 4888 2004-08-02 22:56:25Z shane $
//
package edu.gemini.spModel.pio.xml;

import org.dom4j.tree.DefaultElement;
import org.dom4j.DocumentFactory;

import edu.gemini.spModel.pio.xml.PioXmlDocumentFactory;
import edu.gemini.spModel.pio.Container;

/**
 * Element implementation for {@link Container}.
 */
final class ContainerElement extends DefaultElement implements PioNodeElement {
    public static final String NAME = "container";

    private ContainerImpl _containerImpl;

    public ContainerElement() {
        super(NAME);
        _containerImpl = new ContainerImpl(this);
    }

    public Container getContainer() {
        return _containerImpl;
    }

    public PioNodeImpl getPioNode() {
        return _containerImpl;
    }

    protected DocumentFactory getDocumentFactory() {
        return PioXmlDocumentFactory.INSTANCE;
    }
}
