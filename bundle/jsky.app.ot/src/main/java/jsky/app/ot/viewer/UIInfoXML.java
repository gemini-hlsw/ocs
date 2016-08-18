// Copyright 1997-2001
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: UIInfoXML.java 46768 2012-07-16 18:58:53Z rnorris $
//
package jsky.app.ot.viewer;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import jsky.app.ot.nsp.UIInfo;
import jsky.util.gui.Resources;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.net.URL;

import java.util.*;


/**
 * Reads a file containing OT UIInfo descriptions and provides methods to access
 * the data.
 *
 * @see jsky.app.ot.nsp.UIInfo
 * @author Allan Brighton
 */
public final class UIInfoXML {

    // Maps data object class names to matching UIInfo objects
    private static Map<String, UIInfo> CLASSNAME_MAP;
    private static Map<UIInfo.Id, UIInfo> ID_MAP;

    // Contains the UIInfo objects in the order they were found in the XML file
    private static List<UIInfo> ALL_UIINFO;

    // name of the XML file to read
    private static final String XML_FILE = "conf/UIInfo.xml";

    // Attribute names
    private static final String SITE = "site";
    private static final String ON_SITE = "onsite";
    private static final String DATA_OBJECT = "dataObject";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String IMAGE_KEY = "imageKey";
    private static final String SHORT_DESCRIPTION = "shortDescription";
    private static final String UI_CLASS_NAME = "uiClassName";
    private static final String TOOL_TIP_TEXT = "toolTipText";
    private static final String READ_ONLY = "readOnly";
    private static final String VISIBLE = "visible";
    private static final String EXPERT = "expert";
    private static final String REQUIRES = "requires";


    /** Return the UIInfo for the given data object class name, or null if not found. */
    public static UIInfo getUIInfo(String className) {
        if (className == null) return null;
        return CLASSNAME_MAP.get(className);
    }

    public static UIInfo getUIInfo(UIInfo.Id id) {
        return ID_MAP.get(id);
    }

    /** Return the UIInfo for the given data object, or null if not found. */
    public static UIInfo getUIInfo(Object dataObject) {
        if (dataObject == null) return null;
        return getUIInfo(dataObject.getClass().getName());
    }

    /** Return the UIInfo for the given data object, or null if not found. */
    public static UIInfo getUIInfo(ISPNode node)  {
        // LORD OF DESTRUCTION: DataObjectManager get without set
        return getUIInfo(node.getDataObject());
    }

    /** Initialize by reading the UIInfo.xml file */
    public static void init() {
        try {
            URL url = Resources.getResource(XML_FILE);
            _parseDocument(url);
        } catch (Exception e) {
            e.printStackTrace();

            // really can't continue the application if this info is missing, so just exit
            System.exit(1);
        }
    }

    /** Parse the XML document */
    private static void _parseDocument(URL url) {
        SAXReader reader = new SAXReader(true);
        reader.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) {
                //System.out.println("XXX setEntityResolver");
                URL url = Resources.getResource("UIInfo.dtd");
                try {
                    return new InputSource(url.openStream());
                } catch (Exception e) {
                    throw new RuntimeException("Error resolving entity: " +
                                               publicId + ": " + e.toString());
                }
            }
        });

        Map<String, UIInfo> classnameMap = new HashMap<String, UIInfo>();
        Map<UIInfo.Id, UIInfo> idMap = new HashMap<UIInfo.Id, UIInfo>();
        List<UIInfo> allUinfo = new ArrayList<UIInfo>();

        try {
            Document doc = reader.read(url);
            Element rootElement = doc.getRootElement();
            Iterator it = rootElement.elementIterator();
            while (it.hasNext()) {
                Element e = (Element) it.next();
                UIInfo uiInfo = _parseElement(e);

                if (uiInfo.getId() != null) idMap.put(uiInfo.getId(), uiInfo);
                classnameMap.put(uiInfo.getDataObjectClassName(), uiInfo);
                allUinfo.add(uiInfo);
            }
        } catch (DocumentException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error reading " + XML_FILE + ": " + ex.getMessage());
        }

        CLASSNAME_MAP = Collections.unmodifiableMap(classnameMap);
        ID_MAP = Collections.unmodifiableMap(idMap);
        ALL_UIINFO = Collections.unmodifiableList(allUinfo);
    }


    /** Parse the given UIInfo element, adding a UIInfo object to the map. */
    private static UIInfo _parseElement(Element e) {
        UIInfo.UIInfoBuilder uiInfo = new UIInfo.UIInfoBuilder();
        Iterator it = e.attributeIterator();
        while (it.hasNext()) {
            Attribute attr = (Attribute) it.next();
            String attrName = attr.getName();
            String attrValue = attr.getValue();
            if (attrName.equals(DATA_OBJECT))
                 uiInfo.dataObjectClassName(attrValue);
            else if (attrName.equals(ID))
                uiInfo.id(new UIInfo.Id(attrValue));
            else if (attrName.equals(NAME))
                uiInfo.name(attrValue);
            else if (attrName.equals(TYPE))
                uiInfo.type(attrValue);
            else if (attrName.equals(IMAGE_KEY))
                uiInfo.imageKey(attrValue);
            else if (attrName.equals(SHORT_DESCRIPTION))
                uiInfo.shortDescription(attrValue);
            else if (attrName.equals(UI_CLASS_NAME))
                uiInfo.uiClassName(attrValue);
            else if (attrName.equals(TOOL_TIP_TEXT))
                uiInfo.toolTipText(attrValue);
            else if (attrName.equals(READ_ONLY))
                uiInfo.readOnly(Boolean.valueOf(attrValue));
            else if (attrName.equals(VISIBLE))
                uiInfo.visible(Boolean.valueOf(attrValue));
            else if (attrName.equals(EXPERT))
                uiInfo.expert(Boolean.valueOf(attrValue));
            else if (attrName.equals(SITE))
                uiInfo.site(Site.tryParse(attrValue));
            else if (attrName.equals(ON_SITE))
                uiInfo.onSite(Boolean.valueOf(attrValue));
            else if (attrName.equals(REQUIRES)) {
                final StringTokenizer tok = new StringTokenizer(attrValue);
                while (tok.hasMoreTokens()) {
                    uiInfo.addRequires(new UIInfo.Id(tok.nextToken()));
                }
            }
        }
        return uiInfo.build();
    }

    /** Return a list UIInfo objects with the given type, in the order they were found in the XML file */
    public static List<UIInfo> getByType(String type) {
        ArrayList<UIInfo> result = new ArrayList<UIInfo>();
        Iterator it = ALL_UIINFO.listIterator();
        while (it.hasNext()) {
            UIInfo uiInfo = (UIInfo) it.next();
            String t = uiInfo.getType();
            if (t != null && t.equals(type))
                result.add(uiInfo);
        }
        return result;
    }


    /**
     * Return the instrument engineering component type corresponding to the given instrument
     * component type, or null if not found.
     */
    public static SPComponentType getInstEngComponentType(SPComponentType type) {
        Iterator it = ALL_UIINFO.listIterator();
        while (it.hasNext()) {
            UIInfo uiInfo = (UIInfo) it.next();
            String t = uiInfo.getType();
            if ("engComp".equals(t)) {
                SPComponentType spType = uiInfo.getSPType();
                if (spType.narrowType.equals("Eng" + type.narrowType)) {
                    return spType;
                }
            }
        }
        return null;
    }

    /**
     * Return the instrument sequence component type corresponding to the given instrument
     * component type, or null if not found.
     */
    public static SPComponentType getInstSeqComponentType(SPComponentType type) {
        Iterator it = ALL_UIINFO.listIterator();
        while (it.hasNext()) {
            UIInfo uiInfo = (UIInfo) it.next();
            String t = uiInfo.getType();
            if ("iterComp".equals(t)) {
                SPComponentType spType = uiInfo.getSPType();
                if (spType.narrowType.equals(type.narrowType)) {
                    return spType;
                }
            }
        }
        return null;
    }


    /** Test main */
    public static void main(String[] args) {
        UIInfoXML.init();
    }
}



