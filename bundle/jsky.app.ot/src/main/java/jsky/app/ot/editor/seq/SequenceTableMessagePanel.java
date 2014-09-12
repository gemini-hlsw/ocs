package jsky.app.ot.editor.seq;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.gemini.seqcomp.smartgcal.SmartgcalMappingErrorMessage;
import jsky.app.ot.util.OtColor;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * A message panel for smart calibration mapping errors.
 */
class SequenceTableMessagePanel extends JPanel {
    private JTextArea textArea;

    SequenceTableMessagePanel() {
        super(new java.awt.BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 6));

        textArea = new JTextArea() {{
            setWrapStyleWord(true);
            setLineWrap(true);
            setOpaque(false);
            Font f = getFont();
            setFont(f.deriveFont(f.getSize2D() - 1.0f));
            setEnabled(false);
            setDisabledTextColor(Color.BLACK);
            getPreferredSize();
            setPreferredSize(new Dimension(0, 30));
        }};

        setBackground(OtColor.LIGHT_SALMON);
        add(textArea, BorderLayout.CENTER);
    }

    public void setSequence(ConfigSequence seq, SPNodeKey nodeKey) {
        String msg = SmartgcalMappingErrorMessage.get(seq, nodeKey);
        if (msg == null) {
            textArea.setText("");
            setVisible(false);
        } else {
            textArea.setText(msg);
            setVisible(true);
        }
    }
}
