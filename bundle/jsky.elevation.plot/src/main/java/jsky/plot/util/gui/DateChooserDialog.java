/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: DateChooserDialog.java 8039 2007-08-07 20:32:06Z swalker $
 */


package jsky.plot.util.gui;

import org.jfree.ui.DateChooserPanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import jsky.util.Preferences;


/**
 * Provides a top level window for a DateChooserPanel.
 */
public class DateChooserDialog extends JDialog {

    private DateChooserPanel _dateChooserPanel;
    private boolean _canceled = false;

    /**
     * Create a top level window containing an DateChooserPanel panel.
     *
     * @param parent the parent frame (may be null)
     */
    public DateChooserDialog(Frame parent) {
        this(parent, Calendar.getInstance(), true);
    }


    /**
     * Create a top level window containing an DateChooserPanel panel.
     *
     * @param parent the parent frame (may be null)
     */
    public DateChooserDialog(Frame parent, Calendar cal, boolean controlPane) {
        super(parent, true);

        // center dialog in screen
        //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        //setLocation(screenSize.width / 2 - 150, screenSize.height / 2 - 100);

        _dateChooserPanel = new DateChooserPanel(cal, controlPane);
        getContentPane().add(_dateChooserPanel, BorderLayout.CENTER);
        getContentPane().add(_makeButtonPanel(), BorderLayout.SOUTH);
        pack();
        Preferences.manageLocation(this);
        //setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
    }


    /** Return the selected date */
    public Date getDate() {
        return _dateChooserPanel.getDate();
    }

    /** Set the date displayed */
    public void setDate(Date date) {
        _dateChooserPanel.setDate(date);
    }

    /** Return true if the user canceled the dialog */
    public boolean isCanceled() {
        return _canceled;
    }


    /** Make and return the button panel */
    protected JPanel _makeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _canceled = false;
                hide();
            }
        });
        buttonPanel.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _canceled = true;
                hide();
            }
        });
        buttonPanel.add(cancelButton);

        return buttonPanel;
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame f = new JFrame("Test DateChooserDialog");
        JPanel panel = new JPanel();
        JButton dateButton = new JButton("Choose Date...");
        JButton exitButton = new JButton("Exit");
        f.getContentPane().add(panel);
        panel.add(dateButton);
        panel.add(exitButton);
        f.pack();
        f.setVisible(true);

        Calendar cal = Calendar.getInstance();
        final DateChooserDialog dialog = new DateChooserDialog(f, cal, true);

        dateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.show();
                if (!dialog.isCanceled())
                    System.out.println("Selected date: " + dialog.getDate());
            }
        });
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }
}


