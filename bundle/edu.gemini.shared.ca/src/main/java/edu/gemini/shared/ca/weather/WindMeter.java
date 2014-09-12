package edu.gemini.shared.ca.weather;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

import javax.swing.JPanel;

public class WindMeter extends JPanel {

	private Double direction;
	private Double speed;

	private static final int WIND_MAX = 20;
	
	{
		setOpaque(false);
		setPreferredSize(new Dimension(220, 200));
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
				
		Graphics2D g2d = (Graphics2D) g;		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		AffineTransform t = AffineTransform.getTranslateInstance(getSize().getWidth() / 2, getSize().getHeight() / 2);

		if (direction != null)
			t.rotate(direction.doubleValue() * (2 * Math.PI / 360));
		
		t.scale(5, 5);

		AffineTransform old = g2d.getTransform();
		g2d.transform(t);
		
		g2d.setStroke(new BasicStroke(0.1f));		

		for (int i = WIND_MAX; i > 0; i -= 5) {		
			g2d.setColor(Color.WHITE);			
			g2d.fill(new Arc2D.Double(-i, -i, i * 2, i * 2, 0, 360, Arc2D.OPEN));
			g2d.setColor(Color.BLACK);			
			g2d.draw(new Arc2D.Double(-i, -i, i * 2, i * 2, 0, 360, Arc2D.OPEN));
		}

		if (speed != null) {
			g2d.setColor(Color.LIGHT_GRAY);
			g2d.draw(new Line2D.Double(0, 0.5 + speed.doubleValue(), 0, WIND_MAX));
		}
		
		g2d.setStroke(new BasicStroke(0.3f));		
		g2d.setColor(new Color(0x33, 0x66, 0x99));
		
		if (speed != null) {
			GeneralPath gp = new GeneralPath();
			gp.moveTo(0, 0);
			gp.lineTo(0, (float) speed.doubleValue());
			gp.lineTo(-0.5f, (float) speed.doubleValue() - 1);
			gp.lineTo(+0.5f, (float) speed.doubleValue() - 1);
			gp.lineTo(0, (float) speed.doubleValue());
			g2d.draw(gp);
			g2d.fill(gp);
		}
		
		g2d.setTransform(old);
						
	}

	public Double getDirection() {
		return direction;
	}

	public void setDirection(Double direction) {
		this.direction = direction;
		repaint();
	}

	public Double getSpeed() {
		return speed;
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
		repaint();
	}
	
}
