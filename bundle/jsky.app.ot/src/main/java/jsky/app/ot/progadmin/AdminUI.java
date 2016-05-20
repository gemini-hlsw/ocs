package jsky.app.ot.progadmin;

import javax.swing.*;
import java.awt.*;

/**
 * User interface for the admin dialog.
 */
final class AdminUI extends JPanel {
    private ProgramAttrUI programAttrUI;
    private TimeAcctUI timeAcctUI;
    private GsaUI gsaUI;

    AdminUI() {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.gridx   = 0;
        gbc.weightx = 1.0;
        gbc.insets  = new Insets(10, 10, 0, 10);

        PanelWrapper wrap;

        programAttrUI = new ProgramAttrUI();
        wrap = new PanelWrapper("Program Attributes", programAttrUI);
        gbc.gridy   = 0;
        add(wrap, gbc);

        timeAcctUI = new TimeAcctUI();
        wrap = new PanelWrapper("Time Accounting", timeAcctUI);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.gridy   = 1;
        add(wrap, gbc);

        gsaUI = new GsaUI();
        wrap = new PanelWrapper("Archive Attributes", gsaUI);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.gridy   = 2;
        add(wrap, gbc);

        gbc.gridy   = 3;
        gbc.insets  = new Insets(0,0,0,0);
        add(Box.createHorizontalStrut(520), gbc);
    }

    ProgramAttrUI getProgramAttrUI() {
        return programAttrUI;
    }

    TimeAcctUI getTimeAcctUI() {
        return timeAcctUI;
    }


    public GsaUI getGsaUI() {
        return gsaUI;
    }
}
