package edu.gemini.shared.ca.weather;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

public class Thermometer extends JPanel {

	private static final double BOTTOM = -30;
	private static final double TOP = 45;
	private static final double RANGE = TOP - BOTTOM;
	
	private static final double WIDTH = 50, HEIGHT = 200;
	
	private Double temperature;
	
	public Thermometer() {
		setOpaque(false);
		setPreferredSize(new Dimension((int) WIDTH, (int) HEIGHT + 20));
		setFont(getFont().deriveFont(8.0f));
	}

	public Double getTemperature() {
		return temperature;
	}	

	public void setTemperature(Double temperature) {
		if (this.temperature != temperature) {
			this.temperature = temperature;
			repaint();
		}
	}

	public void paint(Graphics g) {
		super.paint(g);
				
		Graphics2D g2d = (Graphics2D) g;		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

//		g2d.setColor(Color.YELLOW);
//		g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		
		double yscale = -HEIGHT / RANGE;
				
		
		AffineTransform t = AffineTransform.getTranslateInstance(WIDTH - 15, -TOP * yscale); 
		AffineTransform old = g2d.getTransform();
		g2d.transform(t);
				
		Arc2D bulb = new Arc2D.Double(-10, BOTTOM * yscale - 10, 20, 20, 0, 360, Arc2D.OPEN);
		
		RoundRectangle2D rect = new RoundRectangle2D.Double(-5, TOP * yscale, 10, (TOP - BOTTOM) * -yscale, 10, 10);
		
		g2d.setColor(Color.BLACK);
		g2d.draw(rect);
		g2d.draw(bulb);

		g2d.setColor(Color.WHITE);
		g2d.fill(rect);
		g2d.fill(bulb);

		if (temperature != null) {

			// Now draw mercury
			g2d.setStroke(new BasicStroke(0));
			Arc2D bulb2 = new Arc2D.Double(-7, BOTTOM * yscale - 7, 14, 14, 0, 360, Arc2D.OPEN);
			Rectangle2D rect2 = new Rectangle2D.Double(-2.5, temperature.doubleValue() * yscale, 5.5, (temperature.doubleValue() - BOTTOM) * -yscale);
			g2d.setColor(new Color(255, 150, 150));
			g2d.fill(rect2);
			g2d.fill(bulb2);

		}
			
		// draw tick marks
		g2d.setStroke(new BasicStroke(0.7f));
		for (double i = BOTTOM + 5; i < TOP; i+= 5) {
			g2d.setColor(Color.LIGHT_GRAY);
			g2d.draw(new Line2D.Double(-4.5, i * yscale, 4.5, i* yscale));
			g2d.setColor(Color.BLACK);

			FontRenderContext frc = g2d.getFontRenderContext();
			GlyphVector gv = g2d.getFont().createGlyphVector(frc, Integer.toString((int) i));

		
			g2d.drawGlyphVector(gv, - 12 - (float) gv.getOutline().getBounds2D().getWidth(), -1 + (float) (i * yscale) + getFont().getSize2D() / 2);
		}
		
		g2d.setTransform(old);
		
	}
	
}
