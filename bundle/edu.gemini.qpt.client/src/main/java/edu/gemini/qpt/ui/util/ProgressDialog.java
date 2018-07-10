package edu.gemini.qpt.ui.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * Dialog that renders a ProgressModel, showing a message, progress bar, and cancel button.
 * @author rnorris
 */
@SuppressWarnings("serial")
public class ProgressDialog extends JDialog implements ActionListener, PropertyChangeListener {

    private final JLabel label = new JLabel("Progress");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JButton cancel = new JButton("Cancel");
    private final ProgressModel pi;
    
    public ProgressDialog(Frame owner, String title, boolean modal, ProgressModel pi) throws HeadlessException {
        super(owner, title, modal);        
        addWindowListener(new WindowAdapter() {
        
            @Override
            public void windowClosing(WindowEvent e) {
                ProgressDialog.this.pi.setCancelled(true);
            }
        
        });        
        this.pi = pi;
        init();        
    }
    
    private void init() {
        
        setResizable(false);
        setAlwaysOnTop(true);

        JPanel contentPane = new JPanel(new BorderLayout(3, 3));
        setContentPane(contentPane);
        
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(3, 3));
        contentPane.setMinimumSize(new Dimension(300, 500));
        contentPane.add(label, BorderLayout.NORTH);
        contentPane.add(progress, BorderLayout.CENTER);
        contentPane.add(cancel, BorderLayout.EAST);
        cancel.addActionListener(this);
        pack();
                
        pi.addPropertyChangeListener(this);
        
    }

    public void actionPerformed(ActionEvent e) {
        pi.setCancelled(true);
        label.setText("Cancelling...");
    }
 
    @Override
    public void setVisible(boolean b) {

        if (b) {
            label.setText(pi.getMessage());
            progress.setIndeterminate(pi.isIndeterminate());
            progress.setMaximum(pi.getMax());
            progress.setValue(pi.getValue());
            pack();    
            setLocationRelativeTo(getParent());
        }
        
        super.setVisible(b);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if (name.equals(ProgressModel.PROP_INDETERMINATE)) {
            progress.setIndeterminate(pi.isIndeterminate());
        } else if (name.equals(ProgressModel.PROP_MAX)) {
            progress.setMaximum(pi.getMax());
        } else if (name.equals(ProgressModel.PROP_MESSAGE)) {
            label.setText(pi.getMessage());
        } else if (name.equals(ProgressModel.PROP_VALUE)) {
            progress.setValue(pi.getValue());
        }
    }
    
}
