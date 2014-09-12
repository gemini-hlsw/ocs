//
// $Id: DocumentElement.java 5933 2005-04-08 15:37:56Z shane $
//
package edu.gemini.spModel.pio.xml;

import edu.gemini.spModel.pio.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.tree.DefaultElement;

/**
 * Element implementation for {@link Document}.
 */
final class DocumentElement extends DefaultElement implements PioNodeElement {
    public static final String NAME = "document";

    private DocumentImpl _documentImpl;

    public DocumentElement() {
        super(NAME);
        _documentImpl = new DocumentImpl(this);
    }

    public Document getPioDocument() {
        return _documentImpl;
    }

    public PioNodeImpl getPioNode() {
        return _documentImpl;
    }

    protected DocumentFactory getDocumentFactory() {
        return PioXmlDocumentFactory.INSTANCE;
    }
}
