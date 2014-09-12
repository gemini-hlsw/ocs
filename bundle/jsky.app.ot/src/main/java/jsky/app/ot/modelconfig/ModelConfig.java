//
// $
//

package jsky.app.ot.modelconfig;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Parses an spModelConfig.xml file into a map with simple string keys and
 * values.  XML elements may contain nested XML elements or property blocks
 * in Java Properties format.  The name of each property is prefixed by the
 * name of the XML elements that lead to its definition separated by ".".
 */
public final class ModelConfig {
    private static final Logger LOG = Logger.getLogger(ModelConfig.class.getName());

    public static final String FILE_NAME = "spModelConfig.xml";
    public static final File GEMSOFT_CONFIG = new File("/gemsoft/etc/ot/" + FILE_NAME);

    private final Map<String, String> properties;

    public ModelConfig(Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(new HashMap<String, String>(properties));
    }

    public Option<String> get(String key) {
        String res = properties.get(key);
        return (res == null) ? None.STRING : new Some<String>(res);
    }

    public Option<Integer> getInteger(final String key) {
        return get(key).flatMap(new MapOp<String, Option<Integer>>() {
            @Override public Option<Integer> apply(String s) {
                try {
                    return new Some<Integer>(Integer.parseInt(s));
                } catch (Exception ex) {
                    LOG.log(Level.INFO, "Could not parse '" + key + "' with value '" + s + "' as an integer.", ex);
                    return None.instance();
                }
            }
        });
    }

    public Option<Double> getDouble(final String key) {
        return get(key).flatMap(new MapOp<String, Option<Double>>() {
            @Override public Option<Double> apply(String s) {
                try {
                    return new Some<Double>(Double.parseDouble(s));
                } catch (Exception ex) {
                    LOG.log(Level.INFO, "Could not parse '" + key + "' with value '" + s + "' as a double.", ex);
                    return None.instance();
                }
            }
        });
    }

    private static Properties parse(String content) throws IOException {
        Properties props = new Properties();
        props.load(new StringReader(content));
        return props;
    }

    @SuppressWarnings({"unchecked"})
    private static void parse(Node n, String prefix, Map<String, String> res) throws IOException {
        NodeList lst = n.getChildNodes();
        for (int i=0; i<lst.getLength(); ++i) {
            Node c = lst.item(i);

            switch (c.getNodeType()) {
                case Node.ELEMENT_NODE:
                    parse(c, prefix + c.getNodeName() + ".", res);
                    break;
                case Node.TEXT_NODE:
                    Properties p = parse(c.getTextContent());
                    for (Object suffix : p.keySet()) {
                        res.put(prefix + suffix, (String) p.get(suffix));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public static ModelConfig parse(File f) throws IOException {
        Map<String, String> res = new HashMap<String, String>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder        db  = dbf.newDocumentBuilder();
            Document               doc = db.parse(f);
            parse(doc.getDocumentElement(), "", res);
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        } catch (SAXException ex) {
            throw new IOException(ex);
        }
        return new ModelConfig(res);
    }


    private static File getUserConfig() {
        // Get the name of the user's home directory
        String home = System.getProperty("user.home");
        if (home == null) home = "";
        String sep = System.getProperty("file.separator");
        return new File(home + sep + ".jsky" + sep + FILE_NAME);
    }


    public static Option<ModelConfig> load() throws IOException {
        File userConfig = getUserConfig();

        if (GEMSOFT_CONFIG.exists()) {
            return new Some<ModelConfig>(parse(GEMSOFT_CONFIG));
        } else if (userConfig.exists()) {
            return new Some<ModelConfig>(parse(userConfig));
        }

        return None.instance();
    }
}
