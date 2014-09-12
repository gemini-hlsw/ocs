package edu.gemini.shared.ca.weather;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class Hygrometer extends JPanel {

	private Double humidity;

	private double HEIGHT = 30;
	
	public Hygrometer() {
		setPreferredSize(new Dimension(200, (int) HEIGHT));	
		setFont(getFont().deriveFont(8.0f));
	}
	
	public Double getHumidity() {
		return humidity;
	}
	
	public void setHumidity(Double humidity) {
		this.humidity = humidity;
		repaint();
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		
		Graphics2D g2d = (Graphics2D) g;		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		double xscale = getWidth() / 100.0;
		
		Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, 100 * xscale - 1, 10);
		
		g2d.setStroke(new BasicStroke(0.7f));
		
		g2d.setColor(Color.WHITE);
		g2d.fill(rect);
		
		g2d.setColor(Color.DARK_GRAY);
		g2d.draw(rect);

		if (humidity != null) {
			Rectangle2D.Double hrect = new Rectangle2D.Double(2, 3, humidity.doubleValue() * xscale - 1, 5);
			g2d.setColor(new Color(150, 150, 255));
			g2d.fill(hrect);
		}


		for (int i = 10; i < 100; i+= 10) {
			g2d.setColor(Color.LIGHT_GRAY);
			g2d.draw(new Line2D.Double(i * xscale, 1, i * xscale, 9));
			
			g2d.setColor(Color.BLACK);

			FontRenderContext frc = g2d.getFontRenderContext();
			GlyphVector gv = g2d.getFont().createGlyphVector(frc, Integer.toString(i));
		
			g2d.drawGlyphVector(gv, i * (float) xscale - (float) gv.getOutline().getBounds2D().getWidth() / 2, 10 + 4 + getFont().getSize2D());

			
		}
		
	}

}
