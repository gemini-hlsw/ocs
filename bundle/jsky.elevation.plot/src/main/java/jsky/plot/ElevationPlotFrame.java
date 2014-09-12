// Copyright 2003
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: ElevationPlotFrame.java 4731 2004-05-17 20:46:49Z brighton $

package jsky.plot;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import jsky.util.Preferences;


/**
 * Provides a top level window for an ElevationPlotPanel panel.
 *
 * @version $Revision: 4731 $
 * @author Allan Brighton
 */
public class ElevationPlotFrame extends JFrame {

    // The GUI panel
    private ElevationPlotPanel _plotPanel;

    /**
     * Create a top level window containing an ElevationPlotPanel.
     */
    public ElevationPlotFrame() {
        super("Elevation Plot");
        _plotPanel = new ElevationPlotPanel(this);
        setJMenuBar(new ElevationPlotMenuBar(_plotPanel));
        getContentPane().add(_plotPanel, BorderLayout.CENTER);
        pack();
        Preferences.manageLocation(this);
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    public ElevationPlotPanel getPlotPanel() {
        return _plotPanel;
    }
}

