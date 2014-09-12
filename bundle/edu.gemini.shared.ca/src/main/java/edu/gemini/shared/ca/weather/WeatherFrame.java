package edu.gemini.shared.ca.weather;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import gov.aps.jca.CAException;

public class WeatherFrame extends JFrame {

	private final WeatherBean weather;
	
	WindMeter meter = new WindMeter();
	Hygrometer hygro = new Hygrometer();
	Thermometer thermo = new Thermometer();
	WeatherHeader header = new WeatherHeader();
	
	public WeatherFrame() throws CAException {

		super("Weather on Cerro Pachon");
		
		weather = new WeatherBean();
		setResizable(false);
		
		weather.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent pce) {
				String prop = pce.getPropertyName();
				header.update();
				if (prop.equals(WeatherBean.PROP_HUMIDITY)) {
					hygro.setHumidity((Double) pce.getNewValue());
				} else if (prop.equals(WeatherBean.PROP_TEMPERATURE)) {
					thermo.setTemperature((Double) pce.getNewValue());
				} else if (prop.equals(WeatherBean.PROP_WIND_DIRECTION)) {
					meter.setDirection((Double) pce.getNewValue());
				} else if (prop.equals(WeatherBean.PROP_WIND_SPEED)) {
					meter.setSpeed((Double) pce.getNewValue());
				}
			}
		});

		JPanel parent = new JPanel(new BorderLayout());
		parent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(parent);
		
		parent.add(header, BorderLayout.NORTH);
		parent.add(thermo, BorderLayout.WEST);
		parent.add(meter, BorderLayout.CENTER);
		parent.add(hygro, BorderLayout.SOUTH);
		
		pack();
		
	}
	

	class WeatherHeader extends JLabel {
		
		private final String UNAVAILABLE = "No weather information is available.";
		private final MessageFormat mf = new MessageFormat("{0,number,0.0}\ufffdC \ufffd Humidity {1,number,0.0} \ufffd Wind {3,number,0.0} kph at {4,number,0.0}\ufffd");

		public WeatherHeader() {
			setText(UNAVAILABLE);
			setFont(getFont().deriveFont(10.0f));
			setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
			setForeground(Color.BLACK);
			setHorizontalAlignment(SwingConstants.CENTER);
		}
		
		public void update() {
			if (weather.getTemperature() == null) {
				setText(UNAVAILABLE);
			} else {
				setText(mf.format(new Object[] {
					weather.getTemperature(),
					weather.getHumidity(),
					weather.getPressure(),
					weather.getWindSpeed(),
					weather.getWindDirection(),
				}));
			}
		}
		
		public void paint(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;		
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			super.paint(g);
		}

	}



}



