//
// $Id: ParamSetElement.java 4937 2004-08-14 21:35:20Z shane $
//
package edu.gemini.spModel.pio.xml;

import org.dom4j.tree.DefaultElement;
import org.dom4j.DocumentFactory;

import edu.gemini.spModel.pio.xml.PioXmlDocumentFactory;
import edu.gemini.spModel.pio.ParamSet;

/**
 * Element implementation for {@link ParamSet}.
 */
final class ParamSetElement extends DefaultElement implements PioNodeElement {
    public static final String NAME = "paramset";

    private ParamSetImpl _impl;

    public ParamSetElement() {
        super(NAME);
        _impl = new ParamSetImpl(this);
    }

    public ParamSet getParamSet() {
        return _impl;
    }


    public PioNodeImpl getPioNode() {
        return _impl;
    }

    protected DocumentFactory getDocumentFactory() {
        return PioXmlDocumentFactory.INSTANCE;
    }
}
