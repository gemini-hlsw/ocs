//
// $Id: ItemKeyTest.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.spModel.config2.test;
 
import junit.framework.TestCase;
import edu.gemini.spModel.config2.ItemKey;
 
public class ItemKeyTest extends TestCase {
 
   public void testAll() {
       ItemKey parentKey = new ItemKey("apple");
       ItemKey childKey1 = new ItemKey(parentKey, "hardware");
       ItemKey childKey2 = new ItemKey(childKey1, "powermac");
 
       assertNull(parentKey.getParent());
       assertEquals("apple", parentKey.getName());
 
       assertEquals(parentKey, childKey1.getParent());
       assertEquals("apple", childKey1.getParent().getPath());
       assertEquals("hardware", childKey1.getName());
 
       assertEquals(childKey1, childKey2.getParent());
       assertEquals("apple:hardware", childKey2.getParent().getPath());
       assertEquals("powermac", childKey2.getName());
 
       assertTrue(parentKey.compareTo(childKey1) < 0);
       assertTrue(childKey1.compareTo(parentKey) > 0);
       assertTrue(parentKey.compareTo(childKey2) < 0);
       assertTrue(childKey2.compareTo(parentKey) > 0);
 
       assertTrue(childKey1.compareTo(childKey2) < 0);
       assertTrue(childKey2.compareTo(new ItemKey("apple:hardware:powermac")) == 0);
   }
}
