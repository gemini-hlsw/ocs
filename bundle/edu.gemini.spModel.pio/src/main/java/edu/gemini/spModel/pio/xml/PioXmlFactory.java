//
// $Id: PioXmlFactory.java 4994 2004-08-19 21:27:58Z shane $
//
package edu.gemini.spModel.pio.xml;

import edu.gemini.spModel.pio.*;

/**
 * A PioFactory implementation that is backed by DOM4J elements.
 */
public class PioXmlFactory implements PioFactory {
    public static String DEFAULT_PUBLIC_ID =
            "-//Gemini Observatory//DTD for Storage of P1 and P2 Documents//EN";
    public static String DEFAULT_SYSTEM_ID =
            "http://ftp.gemini.edu/Support/xml/dtds/SpXML2.dtd";

    public Document createDocument() {

        DocumentElement del = new DocumentElement();

        org.dom4j.Document dom4jdoc;
        dom4jdoc = PioXmlDocumentFactory.INSTANCE.createDocument(del);
        dom4jdoc.addDocType("document", DEFAULT_PUBLIC_ID, DEFAULT_SYSTEM_ID);

        return del.getPioDocument();
    }

    public Container createContainer(String kind, String type, String version) {
        Container c = (new ContainerElement()).getContainer();
        c.setKind(kind);
        c.setType(type);
        c.setVersion(version);
        return c;
    }

    public ParamSet createParamSet(String name) {
        ParamSet ps = (new ParamSetElement()).getParamSet();
        ps.setName(name);
        return ps;
    }

    public Param createParam(String name) {
        Param p = (new ParamElement()).getParam();
        p.setName(name);
        return p;
    }
}
