package jsky.app.ot.scilib;

import edu.gemini.pot.sp.SPComponentType;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Collections;

/**
 * The panel to ask the user to select the instruments for which libraries are needed.
 * This has some simple logic to return the selection from the user directly.
 */
final class ScienceLibraryFetcherPanel {
    private static final ScienceLibraryInstrumentsPanel _instrumentsPanel = new ScienceLibraryInstrumentsPanel();


    private final JButton _cancelButton;
    private final JButton _finishButton;
    private final JPanel _mainPanel;
    private boolean _wasConfirmed = false;

    ScienceLibraryFetcherPanel() {
        _mainPanel = new JPanel();
        _mainPanel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        // Borders for the title bar and the control panel (at the bottom)
        final Border bevel = BorderFactory.createRaisedBevelBorder();
        final Border padding = BorderFactory.createEmptyBorder(2, 5, 2, 5);
        final Border compound = BorderFactory.createCompoundBorder(bevel, padding);

        // Add the title bar.  It is placed at the top and made as wide as
        // possible.  The gridwidth is two because it overlaps the area taken
        // by the icon (if any).
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        final JLabel titleLabel = new JLabel("Select from the instruments below");

        // Make the font bigger and bolder
        final Font f = titleLabel.getFont();
        final float size = f.getSize2D();
        //_titleLabel.setFont(f.deriveFont(Font.BOLD, (float) (size * 1.5)));
        titleLabel.setFont(f.deriveFont(Font.BOLD, (float) (size * 1.2)));
        titleLabel.setBorder(compound);
        _mainPanel.add(titleLabel, gbc);

        // Add the controls panel, where Cancel and Fetch buttons are kept
        // It is placed at the bottom of the window, and also made as wide as possible.
        final JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
        controlPanel.setBorder(compound);
        _cancelButton = new JButton("Cancel");
        _cancelButton.setToolTipText("Cancel this operation without fetching any library");
        controlPanel.add(_cancelButton);
        controlPanel.add(Box.createHorizontalStrut(5));
        _finishButton = new JButton("Fetch");
        _finishButton.setToolTipText("Fetch the science libraries for the selected instruments");
        controlPanel.add(_finishButton);
        controlPanel.add(Box.createHorizontalGlue());
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        _mainPanel.add(controlPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        final JPanel comp = new JPanel(new BorderLayout());
        comp.setBorder(BorderFactory.createEmptyBorder(15, 35, 15, 35));
        comp.add(_instrumentsPanel, BorderLayout.CENTER);

        _mainPanel.add(comp, gbc);
    }

    /**
     * Creates a (modal) dialog box in which the assistant is displayed.  The
     * dialog returned can be displayed with its <code>show()</code> method, after
     * first setting any desired properties.
     */
    JDialog createDialog(final Frame owner) {
        _wasConfirmed = false;
        final JDialog jd = new JDialog(owner, "Instruments Library Selection", true);
        jd.setContentPane(_mainPanel);
        jd.pack();

        // For some reason, the dialog is not being centered in the frame
        // automatically, so do it here.
        if (owner != null) {
            jd.setLocationRelativeTo(owner);
        } else {
            final Dimension dim = _mainPanel.getPreferredSize();
            final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            jd.setLocation(screen.width / 2 - dim.width / 2, screen.height / 2 - dim.height / 2);
        }

        jd.addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent we) {
                jd.dispose();
            }
        });
        _cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                   jd.dispose();
            }
        });
        _finishButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                _wasConfirmed = true;
                jd.dispose();
            }
        });
        return jd;
    }

    boolean wasConfirmed() {
        return _wasConfirmed;
    }

    /**
     * Returns the selected instruments from the panel as
     * a List of SPComponentType
     */
    Collection<SPComponentType> getSelectedItems() {
        if (_instrumentsPanel != null) {
            return _instrumentsPanel.getSelectedItems();
        }
        return Collections.emptyList();
    }



}
