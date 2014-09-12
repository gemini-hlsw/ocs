/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SPViewerGUI.java 8280 2007-11-23 14:28:22Z anunez $
 */

package jsky.app.ot.viewer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;


/**
 * Implements the GUI layout of the user interface for the main OT window.
 * @author Allan Brighton (ported to Swing/JSky)
 * @version $Revision: 8280 $
 */
public abstract class SPViewerGUI extends JPanel {

    private SPViewerFrame _parent;

    private final SPTree _tree;
    private final JTextArea _descriptionBox;
    private final JPanel _contentPresentation;
    private final TitledBorder _border;

    protected SPViewerGUI() {
        setLayout(new BorderLayout());

        _tree = new SPTree();
        _tree.setMinimumSize(new Dimension(200, 200));
        _tree.setPreferredSize(new Dimension(200, 200));

        _descriptionBox = new JTextArea();
        _descriptionBox.setMargin(new Insets(0, 11, 0, 0));
        _descriptionBox.setBackground(getBackground());
        _descriptionBox.setLineWrap(true);
        _descriptionBox.setWrapStyleWord(true);
        _descriptionBox.setEditable(false);

        _contentPresentation = new JPanel(new BorderLayout());

        final JPanel panel = new JPanel(new BorderLayout());
        panel.setMinimumSize(new Dimension(200, 200));

        final JPanel editorPanel = new JPanel();
        editorPanel.setLayout(new BorderLayout());

        editorPanel.add(_descriptionBox, BorderLayout.NORTH);
        editorPanel.add(_contentPresentation, BorderLayout.CENTER);

        final Border b = BorderFactory.createEmptyBorder(5, 0, 0, 0);
        _border = BorderFactory.createTitledBorder(b, "Component Editor",
                TitledBorder.LEFT,
                TitledBorder.CENTER,
                new Font("Dialog", Font.BOLD | Font.ITALIC, 18));
        editorPanel.setBorder(_border);

        panel.add(editorPanel, BorderLayout.CENTER);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _tree, panel);
        splitPane.setOneTouchExpandable(false);
        add(splitPane, BorderLayout.CENTER);
    }


    public SPViewerFrame getParentFrame() {
        return _parent;
    }

    public void setParentFrame(final SPViewerFrame p) {
        _parent = p;
    }

    public final SPTree getTree() {
        return _tree;
    }

    public JPanel getContentPresentation() {
        return _contentPresentation;
    }

    public TitledBorder getTitledBorder() {
        return _border;
    }

    public JTextArea getDescriptionBox() {
        return _descriptionBox;
    }

}

