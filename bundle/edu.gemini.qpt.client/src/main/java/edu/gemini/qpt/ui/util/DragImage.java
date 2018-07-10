package edu.gemini.qpt.ui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.icon.EmptyIcon;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.ui.gface.GSelection;

public class DragImage {

    public static Image forSelection(Variant v, GSelection<?> selection) {

        final JPanel panel = new JPanel(new GridLayout(selection.size(), 1));
        panel.setOpaque(false);
        for (Object o: selection) {
        
            JLabel label = new JLabel(o.toString());
            label.setForeground(Color.RED);
            
            Obs obs;
            if (o instanceof Obs) {
                obs = (Obs) o;
            } else if (o instanceof Alloc) {
                obs = ((Alloc) o).getObs();
            } else {
                obs = null;
            }
            
            if (obs != null) {
                label.setIcon(CandidateDecorator.getIcon(v.getFlags(obs), obs));
                label.setForeground(CandidateDecorator.getColor(v.getFlags(obs)));
            } else {
                label.setIcon(new EmptyIcon(16, 16));
            }

            label.setSize(label.getPreferredSize());
            
            
            panel.add(label);

        }

        panel.setSize(panel.getPreferredSize());
        panel.doLayout();
            
        Image image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        
        final Graphics g = image.getGraphics();
        panel.printAll(g);
        return image;
        
    }

    public static Point getCenterOffset(Image dragImage) {
        return new Point(-dragImage.getWidth(null) / 2, -dragImage.getHeight(null) / 2);
    }
        
}
