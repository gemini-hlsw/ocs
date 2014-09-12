/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: DnDUtils.java 4336 2004-01-20 07:57:42Z gillies $
 */

//package AdvancedSwing.Chapter8;
package jsky.app.ot.util;

import java.awt.dnd.*;

public class DnDUtils {

    private static boolean debugEnabled = (System.getProperty("DnDExamples.debug") != null);
    //private static boolean debugEnabled = true;

    public static String showActions(int action) {
        String actions = "";
        if ((action & (DnDConstants.ACTION_LINK | DnDConstants.ACTION_COPY_OR_MOVE)) == 0) {
            return "None";
        }

        if ((action & DnDConstants.ACTION_COPY) != 0) {
            actions += "Copy ";
        }

        if ((action & DnDConstants.ACTION_MOVE) != 0) {
            actions += "Move ";
        }

        if ((action & DnDConstants.ACTION_LINK) != 0) {
            actions += "Link";
        }

        return actions;
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void debugPrintln(String s) {
        if (debugEnabled) {
            System.out.println(s);
        }
    }
}
