package edu.gemini.qpt.ui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.sp.MiniModel;
import edu.gemini.ui.workspace.util.Factory;

@SuppressWarnings("serial")
public class ConfigErrorDialog extends JDialog {

    private static final String TITLE = "Configuration Warning";

    private static final String MESSAGE =
        "The following observations have a status of Ready or Ongoing, but have " +
        "no unexecuted sequence steps. These observations will be ignored by QPT, " +
        "but QPT will continue to bug you until someone fixes them.";

    private static final String BUTTON_TEXT = "Ok, thanks.";

    public ConfigErrorDialog(Collection<String> misconfiguredObservations, Frame parent) {
        super(parent);

        // Window
        setTitle(TITLE);
        setModal(true);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Content pane
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        getContentPane().setLayout(new BorderLayout(8, 8));

        // Message Area
        JTextArea ta = new JTextArea(MESSAGE);
        ta.setWrapStyleWord(true);
        ta.setEditable(false);
        ta.setLineWrap(true);
        add(ta, BorderLayout.NORTH);

        // List
        JList<String> list = new JList<>(new Vector<>(misconfiguredObservations));
        JScrollPane scroll = Factory.createStrippedScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        add(scroll, BorderLayout.CENTER);

        // Button
        JButton button = new JButton(BUTTON_TEXT);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(button, BorderLayout.EAST);
        add(panel, BorderLayout.SOUTH);
        button.addActionListener(e -> {
            setVisible(false);
            dispose();
        });

        // Finish up
        pack();
        setSize(new Dimension(300, 300));

        setLocationRelativeTo(parent);

    }

    public static void show(Schedule sched, Frame parent) {
        show(sched.getMiniModel(), parent);
    }

    public static void show(MiniModel miniModel, Frame parent) {
        show(miniModel.getMisconfiguredObservations(), parent);
    }

    public static void show(Collection<String> misconfiguredObservations, Frame parent) {
        if (misconfiguredObservations.size() > 0) {
            new ConfigErrorDialog(misconfiguredObservations, parent).setVisible(true);
        }
    }

}
