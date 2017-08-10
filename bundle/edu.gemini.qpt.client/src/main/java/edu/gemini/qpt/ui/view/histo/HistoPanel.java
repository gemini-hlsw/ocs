package edu.gemini.qpt.ui.view.histo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.ui.view.visualizer.VisualizerConstants;
import edu.gemini.skycalc.HHMMSS;

@SuppressWarnings("serial")
public class HistoPanel extends JPanel {


	private static final int STACKS = 90;
	private static final int DEGREES_PER_STACK = 360 / STACKS;
	private static final int STACKS_PER_LINE = STACKS / 10;

	private Variant variant;

	private static final Map<Object, Object> RENDERING_HINTS = new HashMap<Object, Object>();
	{
		RENDERING_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		RENDERING_HINTS.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
	}

	public HistoPanel() {
		setOpaque(true);
		setBackground(Color.WHITE);
	}

	public void setVariant(Variant variant) {
		this.variant = variant;
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (variant != null) {

			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHints(RENDERING_HINTS);

			// We're going to divide 360 deg into 36 blocks and
			// count up the number of blocked and unblocked obs
			// within each interval.
			int[] scheduled = new int[STACKS];
			int[] scheduled_e = new int[STACKS];
			int[] scheduled_w = new int[STACKS];
			int[] blocked = new int[STACKS];
			int[] unblocked = new int[STACKS];
			for (Obs obs: variant.getSchedule().getMiniModel().getAllObservations()) {
				int i = ((int) obs.getRa(variant.getSchedule().getMiddlePoint())) / DEGREES_PER_STACK;
				if (variant.getFlags(obs).contains(Variant.Flag.BLOCKED)) ++blocked[i]; else ++unblocked[i];
			}
			for (Alloc a: variant.getAllocs()) {
				int i = ((int) a.getObs().getRa(variant.getSchedule().getMiddlePoint())) / DEGREES_PER_STACK;
				int[] array = scheduled;
				if (a.getSeverity() != null) {
					switch (a.getSeverity()) {
					case Error: array = scheduled_e; break;
					case Warning: array = scheduled_w; break;
					}
				}
				++array[i];
			}

			// Ok, now we want to scale to 36 units high and the equivalent width
			double scale = getSize().getWidth() / STACKS;
			AffineTransform xf = AffineTransform.getTranslateInstance(0, getHeight());
			xf.scale(scale, -scale);

			// Tick marks
			g2d.setColor(Color.GRAY);
			g2d.setStroke(VisualizerConstants.DOTTED_STROKE_LIGHT);
			for (int i = STACKS_PER_LINE; i < STACKS;  i+= STACKS_PER_LINE) {
				int x = (int) (scale * i);
				g2d.drawLine(x, 0, x, 35);
				g2d.drawLine(x, 60, x, getHeight());
			}




			AffineTransform prev = g2d.getTransform();
			try {
				g2d.transform(xf);
				g2d.setStroke(new BasicStroke((float) (0.75 / scale)));
				for (int i = 0; i < STACKS; i++) {

					int y = 0;
					y = drawStack(g2d, scheduled[i], i, y, VisualizerConstants.ALLOC_COLOR.darker());
					y = drawStack(g2d, scheduled_e[i], i, y, VisualizerConstants.ALLOC_COLOR_ERR.darker());
					y = drawStack(g2d, scheduled_w[i], i, y, VisualizerConstants.ALLOC_COLOR_WARN.darker());
					y = drawStack(g2d, unblocked[i], i, y, Color.LIGHT_GRAY);
					y = drawStack(g2d, blocked[i], i, y, Color.WHITE);

				}
			} finally {
				g2d.setTransform(prev);
			}

			// Number Labels
			g2d.setColor(Color.BLACK);
			for (int i = STACKS_PER_LINE; i < STACKS;  i+= STACKS_PER_LINE) {
				Double x = scale * i;
				String label = HHMMSS.valStr(i * 10);
				label = label.substring(0, label.lastIndexOf(':'));
				GlyphVector gv = getFont().createGlyphVector(g2d.getFontRenderContext(), label);
				double width = gv.getOutline().getBounds().getWidth();
				g2d.drawGlyphVector(gv, (float) (x - width / 2.0), 50);
			}
		}
	}

	private int drawStack(Graphics2D g2d, int count, int x, int yoffset, Color color) {
		for (int i = 0; i < count; i++) {
			Rectangle2D rect = new Rectangle2D.Double(x, yoffset++, 1, 1);
			g2d.setColor(color);
			g2d.fill(rect);
			g2d.setColor(Color.GRAY);
			g2d.draw(rect);

		}
		return yoffset;
	}

}

