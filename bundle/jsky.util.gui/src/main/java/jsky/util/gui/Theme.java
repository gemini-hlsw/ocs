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
 * Provides a utility for installing the OT look and feel and color theme.
 */
public final class Theme {

    /**
      * Install the OT theme and look and feel.
      */
     public static void install() {
         PlasticLookAndFeel.setPlasticTheme(new SkyBlue() {
             private final ColorUIResource darkGray = new ColorUIResource(90, 90, 90);
             @Override public ColorUIResource getPrimary1() {
                 return getBlack();
             }
             @Override public ColorUIResource getMenuItemSelectedBackground() {
                 return darkGray;
             }
         });
         try {
             UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

            // OT-490: set ToolTip.background color to beige
            UIManager.put("ToolTip.background", new ColorUIResource(245, 245, 220));
         } catch (Exception ex) {
             DialogUtil.error(ex);
         }
     }
}
