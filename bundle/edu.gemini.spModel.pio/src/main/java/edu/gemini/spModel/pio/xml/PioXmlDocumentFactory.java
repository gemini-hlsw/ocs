//
// $Id: PioXmlDocumentFactory.java 4888 2004-08-02 22:56:25Z shane $
//
package edu.gemini.spModel.pio.xml;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;

import java.util.Map;
import java.util.HashMap;

import edu.gemini.shared.util.GeminiRuntimeException;

/**
 * A DocumentFactory extension for creating {@link PioNodeElement}s.
 */
public final class PioXmlDocumentFactory extends DocumentFactory {
    public static final PioXmlDocumentFactory INSTANCE = new PioXmlDocumentFactory();

    public static DocumentFactory getInstance() {
        return INSTANCE;
    }

    private static final Map _typeMap = new HashMap();
    static {
        _typeMap.put(DocumentElement.NAME,  DocumentElement.class);
        _typeMap.put(ContainerElement.NAME, ContainerElement.class);
        _typeMap.put(ParamSetElement.NAME,  ParamSetElement.class);
        _typeMap.put(ParamElement.NAME,     ParamElement.class);
        _typeMap.put(ValueElement.NAME,     ValueElement.class);
    }

    public Element createElement(QName qname) {
        Class type = (Class) _typeMap.get(qname.getName());
        if (type == null) return super.createElement(qname);

        try {
            return (Element) type.newInstance();
        } catch (Exception ex) {
            throw GeminiRuntimeException.newException(ex);
        }
    }

    public Element createElement(String name) {
        return createElement(createQName(name));
    }

    public Element createElement(String qualifiedName, String namespaceURI) {
        return createElement(createQName(qualifiedName, namespaceURI));
    }
}
