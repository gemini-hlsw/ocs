// Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: ElevationPlotManager.java 4726 2004-05-14 16:50:12Z brighton $
//

package jsky.plot;

import java.awt.Component;
import java.util.Date;

import javax.swing.JDesktopPane;
import javax.swing.JLayeredPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.gemini.spModel.core.Site;
import jsky.util.Preferences;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SwingUtil;


/**
 * This class manages access to the ElevationPlotPanel on behalf of clients.
 */
public final class ElevationPlotManager {

    /** Top level window (or internal frame) displaying the elevation plot */
    private static Component _elevationPlotFrame;

    // The single panel, shared for all instances
    private static ElevationPlotPanel _panel;


    /**
     * Return the elevation plot graph panel instance, if it exists, otherwise null.
     */
    public static ElevationPlotPanel get() {
        return _panel;
    }

    /**
     * Open the elevation plot panel, creating it if necessary, and return a reference to it.
     * If a ChangeListener is supplied and is not null, it is notified the first time the panel
     * is created (for customization).
     */
    public static ElevationPlotPanel open(ChangeListener l) {
        if (_elevationPlotFrame != null) {
            SwingUtil.showFrame(_elevationPlotFrame);
        } else {
            JDesktopPane desktop = DialogUtil.getDesktop();
            if (desktop != null) {
                _elevationPlotFrame = new ElevationPlotInternalFrame();
                desktop.add(_elevationPlotFrame, JLayeredPane.DEFAULT_LAYER);
                desktop.moveToFront(_elevationPlotFrame);
                _panel = ((ElevationPlotInternalFrame) _elevationPlotFrame).getPlotPanel();
            } else {
                _elevationPlotFrame = new ElevationPlotFrame();
                _panel = ((ElevationPlotFrame) _elevationPlotFrame).getPlotPanel();
            }
            if (l != null)
                l.stateChanged(new ChangeEvent(_panel));
        }

        return _panel;
    }


    /**
     * Display an elevation plot for the given targets and site and the current date
     * (overridden by any user settings).
     */
    public static void show(TargetDesc[] targets, Site site, ChangeListener l) {
        String timeZoneDisplayName = ElevationPlotModel.UT;
        String timeZoneId = ElevationPlotModel.UT;
        ElevationPlotPanel plotPanel = open(l);
        ElevationPlotModel oldModel = plotPanel.getModel();
        Date date = new Date();

        if (oldModel == null) {
            // First time, try to restore previously saved settings
            String siteName = Preferences.get(ElevationPlotModel.SITE_PREF_KEY);
            if (siteName != null) {
                final Site oldSite = Site.tryParse(siteName);
                if (oldSite != null) site = oldSite;
            }
            timeZoneDisplayName = Preferences.get(ElevationPlotModel.TIMEZONE_DISPLAY_NAME_PREF_KEY, timeZoneDisplayName);
            timeZoneId = Preferences.get(ElevationPlotModel.TIMEZONE_ID_PREF_KEY, timeZoneId);
        } else {
            // keep existing settings
            timeZoneDisplayName = oldModel.getTimeZoneDisplayName();
            timeZoneId = oldModel.getTimeZoneId();
            site = oldModel.getSite();
            date = oldModel.getDate();
        }

        ElevationPlotModel model = new ElevationPlotModel(site, date, targets, timeZoneDisplayName, timeZoneId);
        plotPanel.setModel(model);
    }
}



