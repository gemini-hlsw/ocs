package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.gui.MultilineLabel;
import edu.gemini.shared.util.FileUtil;
import jsky.app.ot.ui.util.FlatButtonUtil;
import jsky.app.ot.util.OtColor;
import jsky.app.ot.util.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

/**
 * Contains widgets and logic for displaying the pre-2010B magnitude
 * information.
 */
final class OriginalMagnitudeEditor extends AbstractTelescopePosEditor {
    private final JPanel pan;
    private final MultilineLabel label;

    OriginalMagnitudeEditor() {
        pan = new JPanel(new GridBagLayout()) {{
            setBackground(OtColor.BANANA);
        }};

        JButton clear = FlatButtonUtil.createSmallRemoveButton();
        clear.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String msg = "Permanently delete pre-2010B OT release magnitude information?";
                String title = "Confirm Delete";
                Object[] opts = {"Cancel", "Delete"};

                int res = JOptionPane.showOptionDialog(pan, msg, title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[1]);
                if (res == 1) {
                    //noinspection deprecation
                    getTarget().setBrightness("");
                }
            }
        });
        pan.add(clear, new GridBagConstraints() {{
            gridx=0; gridy=0; anchor=WEST; insets=new Insets(0, 0, 0, 5);
        }});

        label = new MultilineLabel() {{
            setForeground(Color.black);
            setEnabled(false);
        }};
        pan.add(label, new GridBagConstraints() {{
            gridx=1; gridy=0; anchor=WEST; fill=HORIZONTAL; weightx=1.0;
        }});

        JButton help = FlatButtonUtil.createHelpButton();
        help.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHelp();
            }
        });

        pan.add(help, new GridBagConstraints() {{
            gridx=2; gridy=0; anchor=EAST;
        }});

    }

    private void showHelp() {
        String title = "Pre-2010B Magnitude Information.";

        URL help = Resources.getResource("messages/OldMagnitudeHelp.html");
//        URL addUrl = Resources.getResource(Resources.IMAGES_SUBPATH + "/eclipse/add_small.gif");
        URL rmUrl = Resources.getResource(Resources.IMAGES_SUBPATH + "/eclipse/remove_small.gif");

        String text;
        try {
            text = FileUtil.getURL(help);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

//        text = text.replace("%ADD%", addUrl.toString());
        text = text.replace("%REMOVE%", rmUrl.toString());

        JTextPane pane = new JTextPane() {{
            setContentType("text/html");
            setEditable(false);
            setOpaque(false);
            setFocusable(false);
        }};
        pane.setText(text);

        JOptionPane.showMessageDialog(pan, pane, title,
                JOptionPane.INFORMATION_MESSAGE);

    }

    @Override
    protected void reinit() {
        @SuppressWarnings({"deprecation"}) String oldMagString = getTarget().getBrightness();
        if ((oldMagString == null) || "".equals(oldMagString)) {
            pan.setVisible(false);
            return;
        }

        pan.setVisible(true);
        label.setText(oldMagString);
    }

    @Override
    public Component getComponent() {
        return pan;
    }
}