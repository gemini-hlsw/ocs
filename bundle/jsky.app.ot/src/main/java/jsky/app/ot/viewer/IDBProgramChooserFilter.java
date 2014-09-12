// Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IDBProgramChooserFilter.java 7450 2006-11-22 21:26:45Z shane $
//

package jsky.app.ot.viewer;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.util.DBProgramInfo;

import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;


/**
 * An interface for an extra panel to be inserted in the DBProgramChooser window,
 * which can be used to filter the list of programs displayed there.
 */
public abstract interface IDBProgramChooserFilter {
    /** Return an option panel to be inserted below the list of programs */
    public JPanel getFilterPanel();

    /** Return a filtered version of the given list of {@link DBProgramInfo} objects */
    public List<DBProgramInfo> filter(IDBDatabaseService odb, List<DBProgramInfo> infoList);

    /** Add a listener to be called when something in the filter changes */
    public void addActionListener(ActionListener l);

    /** Remove the ActionListener listener */
    public void removeActionListener(ActionListener l);
}
