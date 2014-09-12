//
// $Id: PioXmlUtil.java 44649 2012-04-20 15:26:59Z swalker $
//
package edu.gemini.spModel.pio.xml;

import edu.gemini.spModel.pio.Document;
import edu.gemini.spModel.pio.PioNode;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;

/**
 * Utility class for reading and writing XML representations of the Science
 * Program model.
 */
public final class PioXmlUtil {
    private static final Logger LOG = Logger.getLogger(PioXmlUtil.class.getName());

    private PioXmlUtil() {
        // defeat instantiation
    }

    public static PioNode read(File file) throws PioXmlException {

        BufferedReader br = null;
        try {
            FileReader fr = new FileReader(file);
            br = new BufferedReader(fr);
            return read(br);
        } catch (FileNotFoundException ex) {
            LOG.log(Level.WARNING, "Could not find the file: " + file);
            throw PioXmlException.newException(ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    public static PioNode read(Reader rdr) throws PioXmlException {
        SAXReader reader = new SAXReader(PioXmlDocumentFactory.INSTANCE, false);
        reader.setEntityResolver(PioEntityResolver.INSTANCE);
        reader.setStripWhitespaceText(true);
        reader.setMergeAdjacentText(true);

        try {
            org.dom4j.Document dom4jDoc = reader.read(rdr);
            PioNodeElement root = (PioNodeElement) dom4jDoc.getRootElement();
            return root.getPioNode();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Problem reading the document", ex);
            throw PioXmlException.newException(ex);
        }
    }

    public static PioNode read(String xml) throws PioXmlException {
        return read(new StringReader(xml));
    }

    public static void write(PioNode node, File file) throws PioXmlException {
        BufferedWriter bw = null;
        try {
            FileWriter fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            write(node, fw);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Problem writing file: " + file, ex);
            throw PioXmlException.newException(ex);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    public static void write(PioNode node, Writer wtr) throws PioXmlException {
        try {
            PioNodeImpl impl = (PioNodeImpl) node;
            Node dom4jnode = impl.getElement();

            if (node instanceof Document) {
                dom4jnode = dom4jnode.getDocument();
            }

            // Now use the JDom XMLOutputter to output the document to the
            // stream
            OutputFormat format = new OutputFormat("  ", true, "UTF-8");
            XMLWriter outer = new XMLWriter(wtr, format);
            outer.write(dom4jnode);
            wtr.close();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "problem writting the document", ex);
            throw PioXmlException.newException(ex);
        }
    }

    public static String toXmlString(PioNode node) throws PioXmlException {
        StringWriter sw = new StringWriter();
        write(node, sw);
        return sw.toString();
    }

    public static Element toElement(PioNode node) {
        return ((PioNodeImpl) node).getElement();
    }
}
