package jsky.app.ot.gemini.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import jsky.html.HTMLViewer;

public class Phase1GUI extends JPanel {

    BorderLayout borderLayout1 = new BorderLayout();
    JTabbedPane tabbedPane = new JTabbedPane();
    JPanel commentsPanel = new JPanel();
    JPanel sciencePanel = new JPanel();
    JPanel techPanel = new JPanel();
    JPanel summaryPanel = new JPanel();
    JPanel targetsPanel = new JPanel();
    Border border1;
    Border border2;
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    GridBagLayout gridBagLayout5 = new GridBagLayout();
    JScrollPane jScrollPane1 = new JScrollPane();
    JTable targetsTable = new JTable();
    HTMLViewer summaryPane = new HTMLViewer();
    Border border3;
    JScrollPane jScrollPane3 = new JScrollPane();
    JScrollPane jScrollPane4 = new JScrollPane();
    JScrollPane jScrollPane5 = new JScrollPane();
    JTextArea commentsPane = new JTextArea();
    JTextArea sciencePane = new JTextArea();
    JTextArea techPane = new JTextArea();
    JPanel navigatorPanel = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();

    public Phase1GUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        border1 = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(Color.white, new Color(134, 134, 134)), BorderFactory.createEmptyBorder(10, 10, 10, 10));
        border2 = BorderFactory.createEmptyBorder(30, 30, 30, 30);
        border3 = BorderFactory.createLineBorder(SystemColor.controlText, 1);
        this.setLayout(borderLayout1);
        summaryPanel.setLayout(gridBagLayout1);
        targetsPanel.setLayout(gridBagLayout2);
        techPanel.setLayout(gridBagLayout3);
        sciencePanel.setLayout(gridBagLayout4);
        commentsPanel.setLayout(gridBagLayout5);
        jScrollPane1.setBorder(BorderFactory.createLineBorder(Color.black));
        targetsTable.setBackground(Color.lightGray);
        targetsTable.setEnabled(false);
        //targetsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        //targetsTable.setIntercellSpacing(new Dimension(5, 5));
        summaryPane.getEditorPane().setContentType("text/html");
        summaryPane.getEditorPane().setMargin(new Insets(10, 10, 10, 10));
        summaryPane.getEditorPane().setEditable(false);
        summaryPane.setBorder(BorderFactory.createLineBorder(Color.black));
        commentsPane.setBackground(Color.lightGray);
        commentsPane.setLineWrap(true);
        commentsPane.setWrapStyleWord(true);
        sciencePane.setMargin(new Insets(10, 10, 10, 10));
        sciencePane.setBackground(Color.lightGray);
        sciencePane.setEditable(false);
        sciencePane.setLineWrap(true);
        sciencePane.setWrapStyleWord(true);
        techPane.setMargin(new Insets(10, 10, 10, 10));
        techPane.setLineWrap(true);
        techPane.setWrapStyleWord(true);
        techPane.setBackground(Color.lightGray);
        techPane.setEditable(false);
        jScrollPane5.setBorder(BorderFactory.createLineBorder(Color.black));
        jScrollPane4.setBorder(BorderFactory.createLineBorder(Color.black));
        jScrollPane3.setBorder(BorderFactory.createLineBorder(Color.black));
        navigatorPanel.setLayout(borderLayout2);
        this.add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.add(sciencePanel, "Science");
        sciencePanel.add(jScrollPane4, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                              , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jScrollPane4.getViewport().add(sciencePane, null);
        tabbedPane.add(techPanel, "Technical");
        techPanel.add(jScrollPane3, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                           , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jScrollPane3.getViewport().add(techPane, null);
        tabbedPane.add(commentsPanel, "TAC Comments");
        commentsPanel.add(jScrollPane5, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                               , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jScrollPane5.getViewport().add(commentsPane, null);
        tabbedPane.add(targetsPanel, "Targets");
        targetsPanel.add(jScrollPane1, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                              , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jScrollPane1.getViewport().add(targetsTable, null);
        tabbedPane.add(summaryPanel, "Summary");
        summaryPanel.add(summaryPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                             , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        summaryPanel.add(navigatorPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
                                                                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 20, 5, 20), 0, 0));
    }
}



