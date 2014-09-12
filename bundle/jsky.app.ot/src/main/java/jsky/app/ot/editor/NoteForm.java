//
// $Id: NoteForm.java 7107 2006-06-02 12:38:01Z shane $
//

package jsky.app.ot.editor;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 *
 */
public class NoteForm extends JPanel {

    JTextField title;
    JLabel imageLabel;
    JTextArea note;

    JList attachList;
    JButton addAttachButton;
    JButton removeAttachButton;
    JButton fetchAttachButton;

    NoteForm(boolean showAttachmentGUI) {
        super(new BorderLayout(10,10));

        add(_initTitleGUI(),      BorderLayout.NORTH);
        add(_initNoteGUI(),       BorderLayout.CENTER);
        if (showAttachmentGUI) {
            add(_initAttachmentGUI(), BorderLayout.SOUTH);
        }

        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    private JComponent _initTitleGUI() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel titleLabel = new JLabel("Title");
        gbc.fill    = GridBagConstraints.NONE;
        gbc.gridx   = 0;
        gbc.gridy   = 0;
        gbc.insets  = new Insets(0, 0, 0, 5);
        gbc.weightx = 0;
        gbc.weighty = 0;
        res.add(titleLabel, gbc);

        title = new JTextField();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.gridx   = 1;
        gbc.gridy   = 0;
        gbc.insets  = new Insets(0, 5, 0, 5);
        gbc.weightx = 1;
        gbc.weighty = 0;
        res.add(title, gbc);

        imageLabel = new JLabel();
        gbc.fill    = GridBagConstraints.NONE;
        gbc.gridx   = 2;
        gbc.gridy   = 0;
        gbc.insets  = new Insets(0, 5, 0, 0);
        gbc.weightx = 0;
        gbc.weighty = 0;
        res.add(imageLabel, gbc);

        return res;
    }

    private JComponent _initNoteGUI() {

        JPanel res = new JPanel(new BorderLayout(0, 5));

        JLabel noteLabel = new JLabel("Note");
        res.add(noteLabel, BorderLayout.NORTH);

        note = new JTextArea();
        note.setBorder(null);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(new EtchedBorder());
        scrollPane.setViewportView(note);

        res.add(scrollPane, BorderLayout.CENTER);

        return res;
    }

    private JComponent _initAttachmentGUI() {

        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel attachLabel = new JLabel("File Attachments");
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridheight = 1;
        gbc.gridwidth  = 2;
        gbc.gridx      = 0;
        gbc.gridy      = 0;
        gbc.insets     = new Insets(5, 0, 5, 0);
        gbc.weightx    = 0;
        res.add(attachLabel, gbc);


        attachList = new JList();
        attachList.setFixedCellWidth(200);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(new EtchedBorder());
        scrollPane.setViewportView(attachList);

        gbc.anchor     = GridBagConstraints.NORTH;
        gbc.fill       = GridBagConstraints.VERTICAL;
        gbc.gridheight = 3;
        gbc.gridwidth  = 1;
        gbc.gridx      = 0;
        gbc.gridy      = 1;
        gbc.insets     = new Insets(0, 0, 0, 5);
        gbc.weightx    = 0;
        res.add(scrollPane, gbc);

        addAttachButton = new JButton("Add");
        gbc.anchor     = GridBagConstraints.NORTH;
        gbc.fill       = GridBagConstraints.HORIZONTAL;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.gridx      = 1;
        gbc.gridy      = 1;
        gbc.insets     = new Insets(0, 5, 5, 0);
        gbc.weightx    = 0;
        res.add(addAttachButton, gbc);

        removeAttachButton = new JButton("Remove");
        gbc.anchor     = GridBagConstraints.NORTH;
        gbc.fill       = GridBagConstraints.HORIZONTAL;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.gridx      = 1;
        gbc.gridy      = 2;
        gbc.insets     = new Insets(5, 5, 5, 0);
        gbc.weightx    = 0;
        res.add(removeAttachButton, gbc);

        fetchAttachButton = new JButton("Fetch");
        gbc.anchor     = GridBagConstraints.NORTH;
        gbc.fill       = GridBagConstraints.HORIZONTAL;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.gridx      = 1;
        gbc.gridy      = 3;
        gbc.insets     = new Insets(5, 5, 0, 0);
        gbc.weightx    = 0;
        res.add(fetchAttachButton, gbc);

        JPanel filler = new JPanel();
        gbc.anchor     = GridBagConstraints.CENTER;
        gbc.fill       = GridBagConstraints.BOTH;
        gbc.gridheight = 4;
        gbc.gridwidth  = 1;
        gbc.gridx      = 2;
        gbc.gridy      = 0;
        gbc.insets     = new Insets(0, 0, 0, 0);
        gbc.weightx    = 1;
        res.add(filler, gbc);

        return res;
    }
}
