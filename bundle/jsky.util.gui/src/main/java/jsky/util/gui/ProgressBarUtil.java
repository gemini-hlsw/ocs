/*
 * ESO Archive
 *
 * $Id: ProgressBarUtil.java 38711 2011-11-15 13:35:55Z swalker $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/23  Created
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


/**
 * Adds animation methods to a JProgressBar, to be used, for example,
 * when downloading a URL where the Content-length is unknown.
 */
public class ProgressBarUtil extends JProgressBar {

    /**
     * The model for the progress bar
     */
    protected DefaultBoundedRangeModel model;

    /**
     * Default model range size
     */
    protected static final int DEFAULT_SIZE = 32;


    /**
     * Constructor
     */
    public ProgressBarUtil() {
        setStringPainted(false);
    }

    /**
     * Do something to look busy.
     */
    public void startAnimation() {
        setIndeterminate(true);  // new feature in jdk1.4
    }


    /**
     * Stop looking busy.
     */
    public void stopAnimation() {
        setIndeterminate(false);
    }


    /**
     * Test main.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();

        JPanel top = new JPanel();
        final ProgressBarUtil progressBarUtil = new ProgressBarUtil();
        top.add(progressBarUtil);
        frame.getContentPane().add(top, BorderLayout.NORTH);

        JPanel bot = new JPanel();
        JButton busyButton = new JButton("Busy");
        JButton stopButton = new JButton("Stop");
        bot.add(busyButton);
        bot.add(stopButton);
        frame.getContentPane().add(bot, BorderLayout.SOUTH);

        busyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                progressBarUtil.startAnimation();
            }
        });

        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                progressBarUtil.stopAnimation();
            }
        });

        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

