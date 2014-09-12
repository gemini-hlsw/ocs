//
// $Id: PioTestUtil.java 47163 2012-08-01 23:09:47Z rnorris $
//

package edu.gemini.spModel.pio.xml.test;

import junit.framework.Assert;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 *
 */
public class PioTestUtil {

    public static void assertEquals(Element expected, Element actual) {
        // Make sure the elements have the same name.
        Assert.assertEquals("element names don't match",
                expected.getName(), actual.getName());

        Assert.assertEquals("element texts don't match",
                expected.getText(), actual.getText());

        // Compare each attribute of expected verses actual.  Order does't
        // matter.
        Set<String> names = new HashSet<String>();
        List lst = expected.attributes();
        for (Object obj : lst) {
            Attribute xattr = (Attribute) obj;
            String xname  = xattr.getName();
            String xvalue = xattr.getValue();

            Assert.assertEquals("attribute '" + xname + "'",
                    xvalue, actual.attributeValue(xname));

            names.add(xname);
        }

        // Now get any attribute names in actual that don't exist in the
        // expected.
        if (expected.attributeCount() != actual.attributeCount()) {
            lst = actual.attributes();
            for (Object obj : lst) {
                Attribute aattr = (Attribute) obj;
                String aname  = aattr.getName();
                Assert.assertTrue(aname + " missing in expected attribute set",
                        names.contains(aname));
            }
        }

        List xchildren = expected.elements();
        List achildren = actual.elements();

        if (xchildren == null) {
            Assert.assertNull(achildren);
            return;
        }

        Assert.assertEquals("child element count",
                xchildren.size(), achildren.size());

        // Compare each element against its counterpart.  Order matters.
        Iterator xit = expected.elementIterator();
        Iterator ait = actual.elementIterator();

        while (xit.hasNext() && ait.hasNext()) {
            Element xchild = (Element) xit.next();
            Element achild = (Element) ait.next();
            assertEquals(xchild, achild);
        }
        Assert.assertEquals("element counts differ", xit.hasNext(), ait.hasNext());
    }
}
