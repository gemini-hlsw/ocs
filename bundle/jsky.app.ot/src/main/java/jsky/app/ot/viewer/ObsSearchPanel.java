/**
 * $Id: ObsSearchPanel.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package jsky.app.ot.viewer;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.util.DBSearchService;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.TextBoxWidget;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Defines a label and text box to use for searching for observations.
 */
class ObsSearchPanel extends JPanel {

    // The viewer containing this widget
    private SPViewer _viewer;

    // used to ignore observations that are not displayed
    private final StatusFilter _statusFilter;
    private final SPTree _tree;

    /**
     * Create the search panel GUI
     */
    public ObsSearchPanel(StatusFilter statusFilter, SPTree tree) {
        _statusFilter = statusFilter;
        _tree = tree;
        setLayout(new BorderLayout());
        add(new JLabel(" Show "), BorderLayout.WEST);
        TextBoxWidget textBox = new TextBoxWidget();
        add(textBox, BorderLayout.CENTER);
        textBox.setToolTipText(
                "Show observation: Enter observation number or beginning of title and hit <Enter>");

        textBox.addWatcher(new jsky.util.gui.TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
            }

            public void textBoxAction(TextBoxWidget tbwe) {
                try {
                    _search(tbwe.getText());
                } catch (Exception e) {
                    DialogUtil.error(e);
                }
            }
        });
    }

    /**
     * Set a reference to the SPViewer instance displaying this tree
     */
    public void setViewer(SPViewer viewer) {
        _viewer = viewer;
    }

    // Search for an observation based on the given string
    private void _search(String s)  {
        ISPProgram prog = _viewer.getProgram();
        if (prog != null && s != null && s.length() != 0) {
            ISPNode n = _viewer.getTree().getCurrentNode();
             // search from current position, making all observations visible first
            _statusFilter.setStatusEnabled();
            ISPObservation obs = (n != null) ? n.getContextObservation() : null;
            ISPObservation start = obs;
            obs = DBSearchService.search(SPDB.get(), prog, true, s, start);
            if (obs == null) {
                if (start == null) return;
                obs = DBSearchService.search(SPDB.get(), prog, true, s, null);
            }
            if (obs == null) return;
            ViewerManager.open(obs);
        }
    }
}
