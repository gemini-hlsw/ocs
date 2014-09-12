//
// $Id$
//

package jsky.app.ot.editor.seq;

import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.OtItemEditor;
import jsky.util.gui.DialogUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This is the default tab displayed in the top level sequence component
 * editor (i.e., {@link jsky.app.ot.editor.seq.IterFolderForm} and
 * {@link jsky.app.ot.editor.seq.EdIteratorFolder}).
 */
public final class SequenceTab extends JPanel {
    private final SequenceTableUI _ui;
    private final OtItemEditor<?,?> _owner;
    private final JButton exportToXml;

    public SequenceTab(OtItemEditor<?,?> owner) {
        super(new GridBagLayout());

        this._owner = owner;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 1));

        _ui = new SequenceTableUI();

        add(_ui, new GridBagConstraints() {{
            gridx     = 0;
            gridy     = 0;
            gridwidth = 2;
            weightx   = 1.0;
            weighty   = 1.0;
            fill      = BOTH;
        }});

        final SequenceEventTestUI testUI = new SequenceEventTestUI();
        if (OTOptions.isTestEnabled()) {
            add(testUI, new GridBagConstraints() {{
                gridx     = 0;
                gridy     = 1;
            }});
        }

        final JPanel btnPanel = new JPanel();
        final JButton printButton = new JButton("Print ...");
        btnPanel.add(printButton);
        printButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String title = _owner.getProgramDataObject().getTitle() + " Observing Sequence";
                    _ui.showPrintDialog(title);
                } catch (Exception ex) {
                    DialogUtil.error(ex);
                }
            }
        });

        exportToXml = new JButton("Export to XML ...") {{
            setVisible(false);
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        SequenceTabUtil.exportAsXML(_owner.getContextObservation(), SequenceTab.this);
                    } catch (Exception ex) {
                        DialogUtil.error(ex);
                    }
                }
            });
        }};
        btnPanel.add(exportToXml);

        add(btnPanel, new GridBagConstraints() {{
            gridx     = 1;
            gridy     = 1;
            weightx   = 1.0;
            weighty   = 0.0;
            fill      = NONE;
            anchor    = EAST;
            insets    = new Insets(0, 0, 0, 0);
        }});

        _ui.init(owner);
        testUI.init(owner);
    }

    public void update() {
        _ui.update(true);
        exportToXml.setVisible(OTOptions.isStaff(_owner.getProgram().getProgramID()));
    }
}
