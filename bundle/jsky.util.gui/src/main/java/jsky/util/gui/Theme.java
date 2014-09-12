/**
 * $Id: GreenTheme.java 7729 2007-04-27 21:09:07Z gillies $
 */

package jsky.util.gui;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyBlue;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

/**
 * Modifies the JGoodies SkyGreen theme to have greenish labels
 */
public final class Theme {

    /**
      * Install the green theme and JGoodies Plastic3DLookAndFeel.
      */
     public static void installGreenTheme() {
         PlasticLookAndFeel.setPlasticTheme(new SkyBlue());
         try {
             UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

            // OT-490: set ToolTip.background color to beige
            UIManager.put("ToolTip.background", new ColorUIResource(245, 245, 220));
         } catch (Exception ex) {
             DialogUtil.error(ex);
         }
     }
}
