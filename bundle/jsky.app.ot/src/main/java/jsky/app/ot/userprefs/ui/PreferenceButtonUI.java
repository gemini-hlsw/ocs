//
// $
//

package jsky.app.ot.userprefs.ui;

import javax.swing.*;
import javax.swing.plaf.metal.MetalToggleButtonUI;
import java.awt.*;

/**
 * UI for toolbar radio buttons that remain pressed after selecting them.
 * For example, this can be used in situations where a tab pane might be
 * appropriate but ugly.  A preferences dialog, for example, could use this to
 * display buttons for the various preferences panels that it offers.
 */
public class PreferenceButtonUI extends MetalToggleButtonUI {
    private Color selectedBorderColor = PreferenceButtonPanelBuilder.SELECTED_BORDER_COLOR;
    private Color selectedColor       = PreferenceButtonPanelBuilder.SELECTED_COLOR;
    private Color backgroundColor     = PreferenceButtonPanelBuilder.BACKGROUND_COLOR;

    /**
     * Returns the color used to highlight the border of the selected button.
     * Two slim arcs will be drawn, one on the left and one on the right of the
     * button using this color.  This creates an offset effect.
     */
    public Color getSelectedBorderColor() {
        return selectedBorderColor;
    }

    /**
     * @see #getSelectedBorderColor()
     */
    public void setSelectedBorderColor(Color selectedBorderColor) {
        this.selectedBorderColor = selectedBorderColor;
    }

    /**
     * Returns the color used to highlight the selected button.  This color
     * will be shown along the horizontal middle of the button, blended from
     * the top and bottom where the
     * {@link #getBackgroundColor() background color} is shown
     */
    public Color getSelectedColor() {
        return selectedColor;
    }

    /**
     * @see #getSelectedColor()
     */
    public void setSelectedColor(Color selectedColor) {
        this.selectedColor = selectedColor;
    }

    /**
     * Returns the normal background color for unselected buttons or the
     * toolbar panel in which this button finds itself.
     *
     * @see #getSelectedColor()
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @see #getBackgroundColor()
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (!b.isContentAreaFilled()) return;

        Graphics2D g2 = (Graphics2D) g;

        int width      = b.getWidth();
        int height     = b.getHeight();
        int halfHeight = height/2;
        int extra      = (height%2 == 1) ? 1 : 0;

        // Create two rectangles that divide the area of the button in half,
        // top and bottom.
        Shape top    = new Rectangle(0, 0, width, halfHeight);
        Shape bottom = new Rectangle(0, halfHeight, width, halfHeight+extra);

        // Paint the top half blending from the background color at the top
        // down to the selected color at the bottom (which is the middle of the
        // button as a whole)
        Paint origPaint = g2.getPaint();
        GradientPaint gp = new GradientPaint(0, 0, backgroundColor, 0, halfHeight, selectedColor);
        g2.setPaint(gp);
        g2.fill(top);

        // Paint the bottom half blending from the selected color at the top
        // (middle of the button) down to the background color at the bottom.
        gp = new GradientPaint(0, halfHeight, selectedColor, 0, height, backgroundColor);
        g2.setPaint(gp);
        g2.fill(bottom);

        // Restore original paint setting.
        g2.setPaint(origPaint);

        // Now draw two filled arcs at the left and right borders to set off
        // this button as the seleced one.
        RenderingHints hints = g2.getRenderingHints();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        Color origColor = g2.getColor();
        g2.setColor(selectedBorderColor);
        g2.fillArc(-2, 0, 4, height, 90, -180);
        g2.fillArc(width-2, 0, 4, height, 90, 180);

        // Restore original settings.
        g2.setColor(origColor);
        g2.setRenderingHints(hints);
    }
}
