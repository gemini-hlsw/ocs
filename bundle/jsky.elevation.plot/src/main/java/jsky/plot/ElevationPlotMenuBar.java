package jsky.plot;

import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import jsky.util.I18N;
import jsky.util.Preferences;

/**
 * Implements a menubar for an ElevationPlotPanel.
 *
 * @version $Revision: 5900 $
 * @author Allan Brighton
 */
public class ElevationPlotMenuBar extends JMenuBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ElevationPlotMenuBar.class);

    // Target panel
    private ElevationPlotPanel _plotPanel;

    // Handle for the File menu
    private JMenu _fileMenu;

    // Handle for the View menu
    private JMenu _viewMenu;

    private JCheckBoxMenuItem _showLegendMenuItem;

    /**
     * Create the menubar for the given elevation plot panel.
     *
     * @param plotPanel the target panel
     */
    public ElevationPlotMenuBar(ElevationPlotPanel plotPanel) {
        super();
        _plotPanel = plotPanel;

        add(_fileMenu = createFileMenu());
        add(_viewMenu = createViewMenu());
    }

    /**
     * Create the File menu.
     */
    protected JMenu createFileMenu() {
        JMenu menu = new JMenu(_I18N.getString("file"));
        menu.add(_plotPanel.getSaveAsAction());
        menu.add(_plotPanel.getPrintAction());
        menu.addSeparator();
        menu.add(_plotPanel.getCloseAction());

        return menu;
    }

    /**
     * Create the View menu.
     */
    protected JMenu createViewMenu() {
        JMenu menu = new JMenu(_I18N.getString("view"));
        menu.add(_plotPanel.getDateAction());
        menu.add(_showLegendMenuItem = _createViewLegendMenuItem());
        menu.add(createViewSamplePeriodMenu());
        return menu;
    }


    // Create the View => Legend menu item
    private JCheckBoxMenuItem _createViewLegendMenuItem() {
        final String SHOW_LEGEND_PREF_KEY = getClass().getName() + ".showLegend";
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("showLegend"));
        boolean showNow = Preferences.get(SHOW_LEGEND_PREF_KEY, true);
        if (!showNow)
            _plotPanel.setShowLegend(false);
        menuItem.setSelected(showNow);

        menuItem.addItemListener(e -> {
            JCheckBoxMenuItem cb = (JCheckBoxMenuItem) e.getSource();
            boolean show = cb.isSelected();
            _plotPanel.setShowLegend(show);
            Preferences.set(SHOW_LEGEND_PREF_KEY, show);
        });
        return menuItem;
    }

    /**
      * Create the View => "Sample Period" menu
      */
     protected JMenu createViewSamplePeriodMenu() {
        // name used to store setting in user preferences
        final String prefName = getClass().getName() + ".SamplePeriod";

         JMenu menu = new JMenu("Sample Period");
        final String[] choices = new String[] {
            "1 minute",
            "5 minutes",
            "10 minutes",
            "15 minutes",
            "30 minutes"
        };
        final List<JRadioButtonMenuItem> l = new ArrayList<>(choices.length);
        JRadioButtonMenuItem[] ar = new JRadioButtonMenuItem[choices.length];
        for(int i = 0; i < choices.length; i++) {
            l.add(ar[i] = new JRadioButtonMenuItem(choices[i]));
        }
        ar[1].setSelected(true);

        ItemListener itemListener = e -> {
            JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
            if (rb.isSelected()) {
                int i = l.indexOf(rb);
                if (i != -1) {
                    int minutes = Integer.parseInt(choices[i].substring(0,2).trim());
                    ElevationPlotModel model = _plotPanel.getModel();
                    if (model != null) {
                        model.setSampleInterval(minutes);
                    } else {
                        ElevationPlotUtil.setDefaultNumSteps((24*60)/minutes);
                    }
                    Preferences.set(prefName, choices[i]);
                }
            }
        };

        ButtonGroup group = new ButtonGroup();
        for(int i = 0; i < choices.length; i++) {
            menu.add(ar[i]);
            group.add(ar[i]);
            ar[i].addItemListener(itemListener);
        }

         // check for a previous preference setting
         String pref = Preferences.get(prefName);
         if (pref != null) {
             for(int i = 0; i < choices.length; i++) {
                 if (pref.equals(choices[i])) {
                     ar[i].setSelected(true);
                     break;
                 }
             }
         }

         return menu;
     }

    /** Return the handle for the View menu */
    public JMenu getViewMenu() {
        return _viewMenu;
    }

    public JCheckBoxMenuItem getShowLegendMenuItem() {
        return _showLegendMenuItem;
    }
}

