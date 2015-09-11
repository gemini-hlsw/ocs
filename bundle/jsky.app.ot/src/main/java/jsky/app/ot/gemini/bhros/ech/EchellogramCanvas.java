package jsky.app.ot.gemini.bhros.ech;

import edu.gemini.spModel.gemini.bhros.BHROSParams;
import edu.gemini.spModel.gemini.bhros.BHROSParams.EntranceFibre;
import edu.gemini.spModel.gemini.bhros.ech.BRayLib;
import edu.gemini.spModel.gemini.bhros.ech.Echellogram;
import edu.gemini.spModel.gemini.bhros.ech.HROSHardwareConstants;
import edu.gemini.spModel.gemini.bhros.ech.WavelengthColor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JOptionPane;


/**
 * A canvas that draws an Echellogram. It supports the following interactions:
 * <ul>
 *  <li>Property change events for model properties (wavelength, centreX/Y), instrument properties
 *      (ech alt/az, goni angle), zoom scale, and wavelength/order near cursor.
 *  <li>Interactive display of wavelength and order under cursor.
 *  <li>Optional display of an array of reference lines.
 *  <li>Wavelength re-targetting on mouse click.
 * </ul>
 */
public class EchellogramCanvas extends JComponent implements HROSHardwareConstants, MouseMotionListener, MouseListener {

	private static final Logger LOGGER = Logger.getLogger(EchellogramCanvas.class.getName());
	
	public static final String PROP_WAVELENGTH = "wavelength";
	public static final String PROP_ENTRANCE_FIBRE = "entranceFibre";
	public static final String PROP_SCALE = "scale";
	public static final String PROP_ECH_AZ = "echAz";
	public static final String PROP_ECH_ALT = "echAlt";
	public static final String PROP_GONI_ANG = "goniAng";
	public static final String PROP_CENTRE_X_POS = "centreXpos";
	public static final String PROP_CENTRE_Y_POS = "centreXpos";
	public static final String PROP_ORDER_AND_WAVELENGTH_NEAR_CURSOR = "OrderAndWavelengthNearCursor";
	
	private static final Font FONT_ORDER_LABEL; //  = new Font("Lucida Sans Regular", Font.PLAIN, 6);		
	private static final Font FONT_FLAG; // = new Font("Lucida Sans Regular", Font.PLAIN, 5);		

	static {
		
		// Load the fonts.
		Font base = new Font("SansSerif", Font.PLAIN, 10); 
		try {
			InputStream is = EchellogramCanvas.class.getResourceAsStream("/resources/fonts/LucidaSansRegular.ttf");	
			base = Font.createFont(Font.TRUETYPE_FONT, is);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Could not load Echellogram font. Using " + base.getName(), e);			
		}
		FONT_ORDER_LABEL = base.deriveFont(6.0f);
		FONT_FLAG = base.deriveFont(5.0f);
		
	}
	
	private static final Color COLOR_CHIP_FILL = new Color(0xAA, 0xAA, 0xAA, 0xAA);
	private static final Color COLOR_CHIP_STROKE = Color.BLACK;
	private static final Color COLOR_BROKEN_PIXELS = Color.BLACK;
	private static final Color COLOR_ORDER_LABEL = Color.BLACK;
	private static final Color COLOR_CROSS = Color.BLACK;
	private static final Color COLOR_CROSS_DEFAULT = Color.GRAY;
	
	private static final Stroke STROKE_ORDER_LINE = new BasicStroke(1.5f);
	private static final Stroke STROKE_ORDER_LABEL = new BasicStroke(1.0f);
	private static final Stroke STROKE_CHIP = new BasicStroke(0.2f);
	private static final Stroke STROKE_BROKEN_PIXELS = new BasicStroke(0.025f);
	private static final Stroke STROKE_CROSS = new BasicStroke(0.3f);
	
	private static final int PADDING_TOP = 40; // px
	private static final int PADDING_LEFT = 30;
	private static final int PADDING_BOTTOM = 30; 
	private static final int PADDING_RIGHT = 10; 
	
	private static final Map RENDERING_HINTS = new HashMap();
	static {
		RENDERING_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		RENDERING_HINTS.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}
	
	private static final int DEFAULT_SCALE = 175; // percent
	
	private Echellogram ech;	
	private int scale = -1; // zoom scale in %
	private OrderAndWavelength orderAndWavelengthNearCursor;
	private double centreXpos = 0.0;
	private double centreYpos = BLUE_YCENTRE;
	private double minX, maxX;
	private double minY, maxY;
	private double echAz, echAlt, goniAng;
	private BHROSParams.EntranceFibre fibre;
	private ReferenceLine[] refLines;
	
	/**
	 * Constructs an echellogram canvas with the specified target wavelength and
	 * default centreX/Y positions at the centre of the blue-side CCD.
	 * @param wavelength
	 */
	public EchellogramCanvas(double wavelength, BHROSParams.EntranceFibre fibre) {
		this.fibre = fibre;
		setBackground(new Color(0xEE, 0xEE, 0xEE));
		addMouseMotionListener(this);
		addMouseListener(this);		
		setScale(DEFAULT_SCALE);
		setWavelength(wavelength);
	}

	private void buildEchellogram(double wavelength, double centreXpos, double centreYpos, BHROSParams.EntranceFibre fibre) {
		
		ech = new Echellogram(wavelength, centreXpos, centreYpos, fibre.getGoniometerOffset());

		// Ok. Figure out the max extent so we know how to size this baby.
		// We assume later on that (0,0) should be at the center of the canvas,
		// so let's make the simplifying assumption that the graph is roughly
		// symmetric. So we can get the extent by finding min and max X and Y values.
		minX = minY = maxX = maxY = 0;
		Echellogram.OrderData[] orders = ech.getOrderData();
		for (int i = 2; i <= orders.length - 3; i++) {
			for (int j = 0; j < orders[i].numPoints; j++) {
				minX = Math.min(minX, orders[i].xpos[j]);
				maxX = Math.max(maxX, orders[i].xpos[j]);
				minY = Math.min(minY, orders[i].ypos[j]);
				maxY = Math.max(maxY, orders[i].ypos[j]);
			}
		}
		
		if (orderAndWavelengthNearCursor != null) {
			// No way to figure out where the mouse is until the user moves
			// the mouse pointer. So we can't re-draw the flag. Just remove it.
			orderAndWavelengthNearCursor = null;
		}
		
		setScale(scale, true);
		setEchAlt(ech.getEchAlt());
		setEchAz(ech.getEchAz());
		setGoniAng(ech.getGoniAng());
		
	}

	
	public synchronized void paintComponent(Graphics g) {
			
		if (ech == null) 
			return;
		
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		
		// Set up 2D environment.
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHints(RENDERING_HINTS);
		
		// The plot data positions the origin in the center of the canvas,
		// so translate to move [0,0] to the center. The plot is also
		// upside-down and backward, so we need to rotate by radians and then
		// do a mirror-image scaling on the X axis.
		AffineTransform oldTransform = g2d.getTransform();
		g2d.transform(getTransform());

		// Now we are set to draw the parts, back to front.
		Echellogram.OrderData[] orders = ech.getOrderData();
		for (int i = 2; i <= orders.length - 3; i++) {
			drawOrderLine(g2d, orders[i]);
			if (i % 5 == 0)
				drawOrderLabels(g2d, orders[i]);
		}
		
		// Draw the CCD outline and center.
		drawChips(g2d);
		drawCentreMarks(g2d);

		// Draw reference lines
		if (refLines != null)
			drawReferenceLines(g2d, refLines, new Color(0xDD, 0xEE, 0xFF));
				
		// Highlight point for wavelength closest to the cursor.
		drawCursorHighlight(g2d);
		
		// Done. Clean up.
		g2d.setTransform(oldTransform);
		
	}

	private void drawReferenceLines(Graphics2D g2d, ReferenceLine[] elements, Color color) {
		for (int i = 0; i < elements.length; i++) {
			ReferenceLine line = elements[i];
			if (line.drawLabel) 
				drawFlag(g2d, line.wavelength * 1.0E-4, color, line.id);
			else
				drawDot(g2d, line.wavelength * 1.0E-4, color);
		}
	}

	private void drawDot(Graphics2D g2d, double wavelength, Color fill) {
		Point2D point = getCoordinatesForWavelength(wavelength);
		if (point != null) {
			drawTick(g2d, point, fill);
		} else {
			System.out.println(wavelength);
		}
	}
	
	private void drawFlag(Graphics2D g2d, double wavelength, Color fill, String text) {
		Point2D point = getCoordinatesForWavelength(wavelength);
		if (point != null) {
			drawFlag(g2d, point, fill, text);
		} else {
			System.out.println(wavelength);
		}
	}
	
	// Draw a vertical tick line. This is for calibration wavelengths that are numerous
	// and very close together, so they need to be small and easy to draw.
	private void drawTick(Graphics2D g2d, Point2D where, Color fill) {		
		float x = (float) where.getX();
		float y = (float) where.getY();
		float w = 0.2f; // mm
		float h = 1.0f;
		
		g2d.setColor(fill);
		
		Shape dot = new Rectangle2D.Float(x - w / 2, y - h / 2, w, h);
		g2d.fill(dot);
	}
	
	private void drawFlag(Graphics2D g2d, Point2D where, Color fill, String text) {
		
		g2d.setFont(FONT_FLAG);
		
		float x = (float) where.getX();
		float y = (float) where.getY();
		
		Shape label = getTextShape(g2d, text);	
		AffineTransform labelTransform = new AffineTransform();
		labelTransform.translate(x - (1 + label.getBounds2D().getWidth()) / 2.0, y + 7);
		labelTransform.scale(1, -1);
		label = labelTransform.createTransformedShape(label);

		Rectangle2D bounds = label.getBounds2D();
		float bx = (float) bounds.getX() - 1;
		float by = (float) bounds.getY() - 1;
		float bh = (float) bounds.getHeight() + 2;
		float bw = (float) bounds.getWidth() + 2;
	
		GeneralPath gp = new GeneralPath();

		gp.moveTo(x, y);
		gp.lineTo(x - 1.5F, by);
		
		gp.lineTo(bx, by);
		gp.lineTo(bx, by + bh);
		gp.lineTo(bx + bw, by + bh);
		gp.lineTo(bx + bw, by);

		gp.lineTo(x + 1.5F, by);

		gp.closePath();
		
		g2d.setColor(fill);
		g2d.fill(gp);

		g2d.setStroke(new BasicStroke(0.25f));
		
		g2d.setColor(Color.BLACK);
		g2d.draw(gp);
		
		
		g2d.fill(label);
	}
	
	private void drawCursorHighlight(Graphics2D g2d) {
		if (orderAndWavelengthNearCursor != null) {
			String text = 
				"Order " + orderAndWavelengthNearCursor.order + ", " +
				EchellogramDisplayUnits.formatMicrons(orderAndWavelengthNearCursor.wavelength);
			drawFlag(g2d, orderAndWavelengthNearCursor.wavelength, Color.YELLOW, text);
		}
	}

	/**
	 * Returns the current transform, which can be used to map instrument coordinates 
	 * (in mm, with (0, 0) centered between the two CCDs) to screen coordinates.
	 */
	public AffineTransform getTransform() {
		AffineTransform newTransform = new AffineTransform();
		newTransform.translate(PADDING_LEFT, PADDING_TOP); // padding
		newTransform.rotate(Math.PI);
		newTransform.scale(-scale / 100.0, scale / 100.0);
		newTransform.translate(-minX, -maxY);
		return newTransform;
	}
	
	private void drawOrderLine(Graphics2D g2d, Echellogram.OrderData order) {

		// Construct a path connecting all the data points.
		GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, order.numPoints);
		path.moveTo((float) order.xpos[0], (float) order.ypos[0]);
		for (int i = 1; i < order.numPoints; i++)
			path.lineTo((float) order.xpos[i], (float) order.ypos[i]);

		// And draw it.
		g2d.setStroke(STROKE_ORDER_LINE);
		g2d.setColor(WavelengthColor.getColorFromWaveLength((int) (1000.0 * order.fsrLimitHigh)));
		g2d.draw(path);
		
	}
	
	private void drawOrderLabels(Graphics2D g2d, Echellogram.OrderData order) {

		// Set up.
		g2d.setStroke(STROKE_ORDER_LABEL);
		g2d.setFont(FONT_ORDER_LABEL);
		
		// First point in the order line.
		double x0 = order.xpos[0];
		double y0 = order.ypos[0];

		// Last point in the line.
		double xN = order.xpos[order.xpos.length - 1];
		double yN = order.ypos[order.ypos.length - 1];
		
		// Text needs to be rotated a little bit so that it follows the slope of
		// the order line. It looks much better this way. See javadoc for
		// AffineTransform.getRotateInstance() for info on the matrix constructor.
		double dx = xN - x0;
		double dy = yN - y0;
		double r = Math.sqrt(dx * dx + dy * dy);
		double sin = dy / r;
		double cos = dx / r;
		double[] matrix = { cos, sin, -sin, cos, 0, 0 }; 
		AffineTransform transform = new AffineTransform(matrix);

		// Also need the slope for offsetting later on.
		double slope = dy / dx;
		
		// We also need to do a mirror-image flip on the Y axis because we're in
		// a strange coordinate system.
		transform.scale(1, -1);
		
		// Width of a space is used for padding.
		double spacer = g2d.getFontMetrics().charWidth(' ');
		
		// The order number hangs off the left side.
		{ 
			// Get the text shape and do the initial transform.
			Shape shape = getTextShape(g2d, Integer.toString(order.orderNumber));
			shape = transform.createTransformedShape(shape);

			// Move the text to the left based on width + spacer, then up or
			// down based on the line height and slope of the line.
			AffineTransform tt = AffineTransform.getTranslateInstance(x0, y0);
			double lineHeight = shape.getBounds2D().getHeight();
			double width = shape.getBounds2D().getWidth() + spacer;
			tt.translate(-width, -(width * slope + lineHeight / 2.0));			
			shape = tt.createTransformedShape(shape);

			// Done.
			drawShape(g2d, shape, getBackground(), COLOR_ORDER_LABEL);
		}

		// The order high end wavelength hangs off the right side.
		{ 
			// Get the text shape and do the initial transform.
			Shape shape = getTextShape(g2d, EchellogramDisplayUnits.formatMicrons(order.fsrLimitHigh));
			shape = transform.createTransformedShape(shape);

			// Move the text to the right spacer, then up or down based on the
			// line height and slope of the line.
			AffineTransform tt = AffineTransform.getTranslateInstance(xN, yN);
			double lineHeight = shape.getBounds2D().getHeight();
			double width = spacer;
			tt.translate(width, width * slope - lineHeight / 2.0);
			shape = tt.createTransformedShape(shape);

			// Done.
			drawShape(g2d, shape, getBackground(), COLOR_ORDER_LABEL);
		}
		
	}
	
	private void drawChips(Graphics2D g2d) {

		g2d.setStroke(STROKE_CHIP);
		g2d.setColor(COLOR_CHIP_FILL);

		// jira:OT-427
		// red chip is busted.
//		Rectangle2D red = new Rectangle2D.Double(- CHIP_XSIZE / 2.0, CHIP_GAP / 2.0, CHIP_XSIZE, RED_CHIP_YSIZE);
		Rectangle2D blu = new Rectangle2D.Double(- CHIP_XSIZE / 2.0, - (CHIP_GAP / 2.0 + BLUE_CHIP_YSIZE), CHIP_XSIZE, BLUE_CHIP_YSIZE);
		
//		g2d.fill(red);
		g2d.fill(blu);
		
	}

	private void drawCentreMarks(Graphics2D g2d) {

		// Highlight the broken pixels
		g2d.setStroke(STROKE_BROKEN_PIXELS);
		g2d.setColor(COLOR_BROKEN_PIXELS);
		for (int i = 0; i < BLUE_CHIP_BROKEN_ROWS.length; i++) {
			double y = PIXELMM * (CHIP_YPIX - BLUE_CHIP_BROKEN_ROWS[i]); 
			Rectangle2D row = new Rectangle2D.Double(- CHIP_XSIZE / 2.0, - (CHIP_GAP / 2.0 + y), CHIP_XSIZE, PIXELMM);
			g2d.draw(row);
		}
		for (int i = 0; i < BLUE_CHIP_BROKEN_COLS.length; i++) {
			double x = PIXELMM * (CHIP_XPIX - BLUE_CHIP_BROKEN_COLS[i]); 
			Rectangle2D row = new Rectangle2D.Double(x - CHIP_XSIZE / 2.0, - (CHIP_GAP / 2.0 + BLUE_CHIP_YSIZE), PIXELMM, BLUE_CHIP_YSIZE);
			g2d.draw(row);
		}

		// Chip outline.
		g2d.setStroke(STROKE_CHIP);
		g2d.setColor(COLOR_CHIP_STROKE);

		// jira:OT-427
		// red chip is busted.
//		Rectangle2D red = new Rectangle2D.Double(- CHIP_XSIZE / 2.0, CHIP_GAP / 2.0, CHIP_XSIZE, RED_CHIP_YSIZE);
		Rectangle2D blu = new Rectangle2D.Double(- CHIP_XSIZE / 2.0, - (CHIP_GAP / 2.0 + BLUE_CHIP_YSIZE), CHIP_XSIZE, BLUE_CHIP_YSIZE);
		
//		g2d.draw(red);
		g2d.draw(blu);


		
		// now draw the default X
		{
			double centreXpos = 0.0, centreYpos = BLUE_YCENTRE;
			g2d.setStroke(STROKE_CROSS);
			g2d.setColor(COLOR_CROSS_DEFAULT);		
			GeneralPath cross = new GeneralPath();
			cross.moveTo((float) centreXpos - 2, (float) centreYpos - 2);
			cross.lineTo((float) centreXpos + 2, (float) centreYpos + 2);
			cross.moveTo((float) centreXpos - 2, (float) centreYpos + 2);
			cross.lineTo((float) centreXpos + 2, (float) centreYpos - 2);
			g2d.draw(cross);
		}
		
		// now draw the centre X
		{
			g2d.setStroke(STROKE_CROSS);
			g2d.setColor(COLOR_CROSS);		
			GeneralPath cross = new GeneralPath();
			cross.moveTo((float) centreXpos - 2, (float) centreYpos - 2);
			cross.lineTo((float) centreXpos + 2, (float) centreYpos + 2);
			cross.moveTo((float) centreXpos - 2, (float) centreYpos + 2);
			cross.lineTo((float) centreXpos + 2, (float) centreYpos - 2);
			g2d.draw(cross);
		}
	}
	
	
	/**
	 * Return the given text as a Shape, based on the current Graphics2D context.
	 */
	private Shape getTextShape(Graphics2D g2d, String text) {
		FontRenderContext frc = g2d.getFontRenderContext();
		GlyphVector gv = g2d.getFont().createGlyphVector(frc, text);
		return gv.getOutline();
	}
	
	/**
	 * Draw a shape on the given Graphics2D, using different colors for draw 
	 * and fill. Useful for drawing text with an outline.
	 */
	private void drawShape(Graphics2D g2d, Shape shape, Color draw, Color fill) {
		g2d.setColor(draw);
		g2d.draw(shape);
		g2d.setColor(fill);
		g2d.fill(shape);
	}
	
	public int getScale() {
		return scale;
	}
	
	/**
	 * Sets the current scale in percent, where 100 means 1mm == 1px.
	 * @param scale
	 */
	public void setScale(int scale) {
		setScale(scale, false);
	}

	private void setScale(int scale, boolean forceRepaint) {
		if (forceRepaint || scale != this.scale) {
			int prev = this.scale;
			this.scale = scale;
			int w = PADDING_LEFT + PADDING_RIGHT + (int) ((-minX + maxX) * scale / 100);
			int h = PADDING_TOP + PADDING_BOTTOM + (int) ((-minY + maxY) * scale / 100);			
			setPreferredSize(new Dimension(w, h));			
			revalidate();
			repaint();
			firePropertyChange(PROP_SCALE, new Integer(prev), new Integer(scale));
		}
	}
	
	/**
	 * Return the echellogram coordinates in mm for the given pixel.
	 * This simply runs the point through the inverse of the current transform.
	 */
	public Point2D getModelCoordinate(int clientX, int clientY) {
		try {
			Point2D coords = new Point2D.Double(clientX, clientY);
			coords = getTransform().inverseTransform(coords, coords); // this really smokes
			return coords;
		} catch (NoninvertibleTransformException e) {
			throw new RuntimeException(e); // won't happen.
		}
	}

	/**
	 * Returns the closest order and wavelength for the given echellogram coordinate position.
	 * Get the echellogram coordinate for a given pixel location via getModelCoordinate().
	 * <p>
	 * TODO: Remove C-ness when correctness is known.
	 */
	public OrderAndWavelength getOrderAndWavelength(Point2D coords) {

		double xcurs = coords.getX();
		double ycurs = coords.getY();
		
		Echellogram.OrderData[] echellePlotData = ech.getOrderData();
		
		double dist;
		double mindist = Double.MAX_VALUE;
		double wavel;
		int index = -1;
		int order = 0;
		int points = 0;
		
	    for (int j = 1; j < echellePlotData.length; j++) {
			if (echellePlotData[j].numPoints > 1) {

				/* Where we find X plot values that span the cursor's X-position, interpolate Y value */
				for (int i = 0; i < echellePlotData[j].numPoints - 1; i++) {
					
					if (echellePlotData[j].xpos[i] <= xcurs && echellePlotData[j].xpos[i+1] >= xcurs) {
						
						double yval = BRayLib.interp(	
								echellePlotData[j].xpos[i], 
								echellePlotData[j].ypos[i],
								echellePlotData[j].xpos[i+1], 
								echellePlotData[j].ypos[i+1],
								xcurs);
						
						dist = Math.abs(ycurs - yval);  /* Get Y position difference */
					
					} else  { /* Out of range - just use echellogram plot X,Y positions */
						
						dist = Math.sqrt( 
								Math.pow(echellePlotData[j].ypos[i] - ycurs, 2) +
								Math.pow(echellePlotData[j].xpos[i] - xcurs, 2) );
					
					}
					
					if (dist < mindist) {
						index = j;
						points = echellePlotData[j].numPoints;
						order = echellePlotData[j].orderNumber;
						mindist = dist;
					}
					
				}
			}
	    }
	    
		if (index == -1) {
			return null;
		}
		
	    double FSRlow   = echellePlotData[index].fsrLimitLow;   
	    double FSRhi    = echellePlotData[index].fsrLimitHigh;   
	    double plotXlow = echellePlotData[index].xpos[0];
	    double plotXhi  = echellePlotData[index].xpos[points-1];
	    if (xcurs <= plotXlow) {
			wavel = FSRlow;
	    } else if (xcurs >= plotXhi) {
			wavel = FSRhi;
	    } else {
			wavel = BRayLib.interp(plotXlow, FSRlow, plotXhi, FSRhi, xcurs);
	    }

		return new OrderAndWavelength(order, wavel);

	}
	
	/**
	 * Return the echellogram coordinates for the given wavelength, or null if the
	 * wavelength is out of range.
	 * <p>
	 * TODO: Remove C-ness when correctness is known. I think there is a problem here because the
	 * father you get from the central wavelength, the farther the points stray from the spectral
	 * lines. Unclear where the problem is. This also happens in the C version.
	 */
	public Point2D getCoordinatesForWavelength(double wavelength) {
		Echellogram.OrderData[] echellePlotData = ech.getOrderData();
		for (int index = 2; index < echellePlotData.length; index++) {
			if (echellePlotData[index].fsrLimitLow <= wavelength && echellePlotData[index].fsrLimitHigh >= wavelength) {
				int points   = echellePlotData[index].numPoints;
				double FSRlow   = echellePlotData[index].fsrLimitLow; /* Get wavelengths and X plot positions */
				double FSRhi    = echellePlotData[index].fsrLimitHigh;  /* at the ends of this order */
				double plotXlow = echellePlotData[index].xpos[0];
				double plotXhi  = echellePlotData[index].xpos[points-1];
				double xpos = BRayLib.interp(FSRlow, plotXlow, FSRhi, plotXhi, wavelength);
				double plotYlow = echellePlotData[index].ypos[0];  /* Also Y positions */
				double plotYhi  = echellePlotData[index].ypos[points-1];
				double ypos = BRayLib.interp(plotXlow, plotYlow, plotXhi, plotYhi, xpos);  
				return new Point2D.Double(xpos, ypos);
			}	
		}
		return null;		
	}
	
	public static class OrderAndWavelength {
		
		public final int order;
		public final double wavelength;
		
		public OrderAndWavelength(int order, double wavelength) {
			this.order = order;
			this.wavelength = wavelength;
		}
		
		public String toString() {
			return "OrderAndWavelength[" + order + ", " + wavelength + "]";
		}
		
	}

	public OrderAndWavelength getOrderAndWavelengthNearCursor() {
		return orderAndWavelengthNearCursor;
	}
	
	private void setOrderAndWavelengthNearCursor(OrderAndWavelength orderAndWavelengthNearCursor) {
		Object prev = this.orderAndWavelengthNearCursor;
		this.orderAndWavelengthNearCursor = orderAndWavelengthNearCursor;
		firePropertyChange(PROP_ORDER_AND_WAVELENGTH_NEAR_CURSOR, prev, this.orderAndWavelengthNearCursor);
		repaint();
	}

	public void mouseDragged(MouseEvent me) {
		// nop
	}

	public void mouseMoved(MouseEvent me) {
		calculateOrderAndWavelengthNearCursor(me.getX(), me.getY());
	}
		
	private void calculateOrderAndWavelengthNearCursor(int x, int y) {
		Point2D coords = getModelCoordinate(x, y);
		OrderAndWavelength ow = getOrderAndWavelength(coords);
		setOrderAndWavelengthNearCursor(ow);
	}

	public void setReferenceLines(ReferenceLine[] lines) {
		refLines = lines;
		repaint();
	}
	
	public void mouseClicked(MouseEvent arg0) {
		try {
			if (orderAndWavelengthNearCursor != null) 
				setWavelength(orderAndWavelengthNearCursor.wavelength);
		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
			JOptionPane.showMessageDialog(this, 
					"Tracking wavelength out of range. Try re-centering the CCD.", 
					"Error",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public void mousePressed(MouseEvent arg0) {
	}

	public void mouseReleased(MouseEvent arg0) {
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
		orderAndWavelengthNearCursor = null;
		repaint();
	}

	public double getCentreXpos() {
		return centreXpos;
	}
	
	public void setCentreXpos(double centreXpos) {
		try {
			Double prev = new Double(this.centreXpos);
			buildEchellogram(getWavelength(), centreXpos, centreYpos, fibre);
			this.centreXpos = centreXpos;
			firePropertyChange(PROP_CENTRE_X_POS, prev, new Double(centreXpos));
			repaint();
		} catch (IllegalArgumentException iae) {
			LOGGER.log(Level.WARNING, "Could not set centerXpos to " + centreXpos, iae);
		}
	}

	public double getCentreYpos() {
		return centreYpos;
	}
	
	public void setCentreYpos(double centreYpos) {
		try {
			Double prev = new Double(this.centreYpos);
			buildEchellogram(getWavelength(), centreXpos, centreYpos, fibre);
			this.centreYpos = centreYpos;
			firePropertyChange(PROP_CENTRE_Y_POS, prev, new Double(centreYpos));
			repaint();
		} catch (IllegalArgumentException iae) {
			LOGGER.log(Level.WARNING, "Could not set centerYpos to " + centreYpos, iae);
		}
	}

	public double getEchAlt() {
		return echAlt;
	}
	
	public int getMinOrder() {
		return ech.getMinOrder();
	}
	
	public int getMaxOrder() {
		return ech.getMaxOrder();
	}

	public double getMinWavelength() {
		return ech.getMinWavelength();
	}
	
	public double getMaxWavelength() {
		return ech.getMaxWavelength();
	}
	
	public Set getGapOrders() {
		return ech.getGapOrders();
	}

	private void setEchAlt(double echAlt) {
		Object prev = new Double(this.echAlt);
		this.echAlt = echAlt;
		firePropertyChange(PROP_ECH_ALT, prev, new Double(echAlt));
	}
	
	public double getEchAz() {
		return echAz;
	}
	
	private void setEchAz(double echAz) {
		Object prev = new Double(this.echAz);
		this.echAz = echAz;
		firePropertyChange(PROP_ECH_AZ, prev, new Double(echAz));
	}
	
	public double getGoniAng() {
		return goniAng;
	}
	
	private void setGoniAng(double goniAng) {
		Object prev = new Double(this.goniAng);
		this.goniAng = goniAng;
		firePropertyChange(PROP_GONI_ANG, prev, new Double(goniAng));
	}

	/**
	 * Returns the spectral order for the current wavelength and CCD position.
	 */
	public int getOrder() {
		return ech.getOrder();
	}
	
	public synchronized void setWavelength(double wavelength) {
		// Do this first. If it fails, our state won't be changed at all.
		Object prev = new Double(getWavelength());
		buildEchellogram(wavelength, centreXpos, centreYpos, fibre);
		firePropertyChange(PROP_WAVELENGTH, prev, new Double(getWavelength()));		
	}
	
	public double getWavelength() {
		return ech == null ? 0 : ech.getWavelength();
	}

	public BHROSParams.EntranceFibre getEntranceFibre() {
		return fibre;
	}

	public void setEntranceFibre(EntranceFibre fibre) {
		try {
			Object prev = this.fibre;
			buildEchellogram(getWavelength(), centreXpos, centreYpos, fibre);
			this.fibre = fibre;
			firePropertyChange(PROP_ENTRANCE_FIBRE, prev, fibre);
			repaint();
		} catch (IllegalArgumentException iae) {
			LOGGER.log(Level.WARNING, "Could not set entrance fibre to " + fibre, iae);
		}

	}	

	
}
