package jsky.app.ot.editor.conflict;

import edu.gemini.pot.sp.ISPConflictFolder;
import edu.gemini.shared.gui.MultilineLabel;
import edu.gemini.spModel.conflict.ConflictFolder;
import jsky.app.ot.editor.OtItemEditor;
import jsky.util.gui.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class EdConflictFolder extends OtItemEditor<ISPConflictFolder, ConflictFolder> {
    public static final String INSTRUCTIONS =
        "This folder contains conflicting items that could not be automatically " +
        "merged with updates from the database made by other users.  You should " +
        "review this folder's contents and either delete each item or else " +
        "manually drag and drop each into place in the program tree. When the " +
        "Conflicts folder is empty, it will automatically be removed from the " +
        "program.  You can also delete the entire folder and its contents at " +
        "any time if it contains nothing of interest to you.";

    public static final String WARNING =
        "A program with a Conflicts folder cannot be stored to the database.";

    final JPanel panel = new JPanel(new GridBagLayout());

    public EdConflictFolder() {
        final MultilineLabel inst = new MultilineLabel(INSTRUCTIONS) {{
            setForeground(Color.darkGray.darker());
        }};

        panel.add(inst, new GridBagConstraints() {{
            insets    = new Insets(20,50,10,50);
            fill      = HORIZONTAL;
            weightx   = 1.0;
            gridwidth = 2;
        }});

        panel.add(new JLabel(Resources.getIcon("eclipse/alert.gif")), new GridBagConstraints() {{
            gridy     = 1;
            insets    = new Insets(0,50,20,5);
        }});

        final MultilineLabel warn = new MultilineLabel(WARNING) {{
            setForeground(Color.black);
            setFont(getFont().deriveFont(Font.BOLD));
        }};

        panel.add(warn, new GridBagConstraints() {{
            gridx   = 1;
            gridy   = 1;
            insets  = new Insets(0,0,20,50);
            fill    = HORIZONTAL;
            weightx = 1.0;
        }});

        final JButton deleteButton = new JButton("Delete Folder and All Contents", Resources.getIcon("eclipse/remove.gif")) {{
            setToolTipText("Deletes this folder and all the program nodes it contains.");
            addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    getNode().getParent().removeConflictFolder();
                }
            });
        }};

        panel.add(deleteButton, new GridBagConstraints() {{
            gridy     = 2;
            gridwidth = 2;
        }});

        panel.add(new JPanel(), new GridBagConstraints() {{
            gridy     = 3;
            gridwidth = 2;
            fill      = BOTH;
            weighty   = 1.0;
        }});
    }

    @Override public JPanel getWindow() { return panel; }

    public void init() {
        // no-op
    }

}
