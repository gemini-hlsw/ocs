//
// $Id$
//

package edu.gemini.shared.gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.AffineTransform;


/**
 * BasicButtonUI subclass that shows the button text/icon vertically.
 */
public class RotatedButtonUI extends BasicButtonUI {

    public enum Orientation {
        topToBottom,
        bottomToTop,
    }

    protected Orientation orientation;

    private Rectangle paintIconR = new Rectangle();
    private Rectangle paintTextR = new Rectangle();
    private Rectangle paintViewR = new Rectangle();
    private Insets paintViewInsets = new Insets(0, 0, 0, 0);

    public RotatedButtonUI() {
        this(Orientation.topToBottom);
    }

    public RotatedButtonUI(Orientation orient) {
        this.orientation = (orient == null) ? Orientation.topToBottom : orient;
    }


    public Dimension getPreferredSize(JComponent c) {
        Dimension dim = super.getPreferredSize(c);
        return new Dimension( dim.height, dim.width );
    }


    public void paint(Graphics g, JComponent c) {
        AbstractButton button = (AbstractButton) c;
        String text = button.getText();
        Icon icon = (button.isEnabled()) ? button.getIcon() : button.getDisabledIcon();

        if ((icon == null) && (text == null)) {
            return;
        }

        FontMetrics fm = g.getFontMetrics();
        paintViewInsets = c.getInsets(paintViewInsets);

        paintViewR.x = paintViewInsets.left;
        paintViewR.y = paintViewInsets.top;

        // Use inverted height & width
        paintViewR.height = c.getWidth() - (paintViewInsets.left + paintViewInsets.right);
        paintViewR.width = c.getHeight() - (paintViewInsets.top + paintViewInsets.bottom);

        paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
        paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

        Graphics2D g2 = (Graphics2D) g;
        AffineTransform tr = g2.getTransform();
        if (orientation == Orientation.topToBottom) {
            g2.rotate( Math.PI / 2 );
            g2.translate( 0, - c.getWidth() );
            paintViewR.x = c.getHeight()/2 - (int)fm.getStringBounds(text, g).getWidth()/2;
            paintViewR.y = c.getWidth()/2 - (int)fm.getStringBounds(text, g).getHeight()/2;
        } else {
            g2.rotate( - Math.PI / 2 );
            g2.translate( - c.getHeight(), 0 );
            paintViewR.x = c.getHeight()/2 - (int)fm.getStringBounds(text, g).getWidth()/2;
            paintViewR.y = c.getWidth()/2 - (int)fm.getStringBounds(text, g).getHeight()/2;
        }

        if (icon != null) {
            icon.paintIcon(c, g, paintIconR.x, paintIconR.y);
        }

        if (text != null) {
            int textX = paintTextR.x;
            int textY = paintTextR.y + fm.getAscent();
            paintText(g, button, new Rectangle(paintViewR.x,paintViewR.y,textX,textY), text);
        }

        g2.setTransform( tr );
    }

}
