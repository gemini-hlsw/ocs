//
// $Id: ParamElement.java 4888 2004-08-02 22:56:25Z shane $
//
package edu.gemini.spModel.pio.xml;

import org.dom4j.tree.DefaultElement;
import org.dom4j.DocumentFactory;

import edu.gemini.spModel.pio.xml.PioXmlDocumentFactory;
import edu.gemini.spModel.pio.Param;

/**
 * Element implementation for {@link Param}s.
 */
final class ParamElement extends DefaultElement implements PioNodeElement {
    static final String NAME = "param";

    private ParamImpl _paramImpl;

    public ParamElement() {
        super(NAME);
        _paramImpl = new ParamImpl(this);
    }

    public Param getParam() {
        return _paramImpl;
    }

    public PioNodeImpl getPioNode() {
        return _paramImpl;
    }

    protected DocumentFactory getDocumentFactory() {
        return PioXmlDocumentFactory.INSTANCE;
    }

}
