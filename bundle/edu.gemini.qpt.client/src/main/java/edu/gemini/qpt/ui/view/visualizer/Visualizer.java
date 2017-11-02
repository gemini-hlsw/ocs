package edu.gemini.qpt.ui.view.visualizer;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import edu.gemini.lch.services.model.Observation;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.MoonCalc;
import edu.gemini.spModel.core.Site;
import jsky.coords.WorldCoords;
import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Block;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.shared.sp.Conds;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.core.util.LocalSunriseSunset;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.qpt.core.util.TimingWindowSolver;
import edu.gemini.qpt.ui.util.BooleanViewPreference;
import edu.gemini.qpt.ui.util.ColorWheel;
import edu.gemini.qpt.ui.util.ElevationPreference;
import edu.gemini.qpt.ui.util.Graphics2DAttributes;
import edu.gemini.qpt.ui.util.MoonLabel;
import edu.gemini.qpt.ui.util.TimePreference;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.skycalc.TwilightBoundedNight;

/**
 * Concrete schedule visualizer class.
 * <p>
 * The implementation contains only drawing routines and associated caching logic; all other
 * state management and event handling is done by the superclass.
 */
@SuppressWarnings("serial")
public final class Visualizer extends VisualizerBase implements VisualizerConstants {


	private final Font LABEL_FONT; // not static because we derive it at runtime.
	{
		LABEL_FONT = getFont(); // .deriveFont(getFont().getSize2D() - 2.0f);
	}

	// Constructing the various curved shapes is pretty math-intensive, so we do a little
	// bit of caching. The moon curve, boundary bars, and open/closed elevation curves are
	// cached. The sidereal objects are defined entirely by their WorldCoords (ra/dec) and
	// the start/end times that are part of the CachedShape struct. See below.
	private final Object cacheLock = new Object(); // common sync object
	private CachedShape cachedMoonCurve, cachedSunCurve;
	private final Map<String, CachedShape> openElevationCurveCache = new HashMap<>();
	private final Map<String, CachedShape> fullElevationCurveCache = new HashMap<>();
	private final Map<String, CachedShape> closedElevationCurveCache = new HashMap<>();
	private final Map<String, CachedShape> skyBackgroundCurveCache = new HashMap<>();

	// Should the visualizer pay attention to QC-only warnings?
	private final boolean qcOnly;

	// Should the timezone be fixed?
	private final TimePreference timePreference;

	public Visualizer() {
		this(true, null);
	}

	public Visualizer(boolean qcOnly, TimePreference timePreference) {
		this.qcOnly = qcOnly;
		this.timePreference = timePreference;

		setOpaque(true);
		setBackground(DAY_COLOR);

		// When the control is resized, clear all the shape caches because they're invalid.
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				clearShapeCaches();
				repaint();
			}

		});

		TimePreference.BOX.addPropertyChangeListener(evt -> repaint());
	}

	private void clearShapeCaches() {
		synchronized (cacheLock) {
			cachedMoonCurve = null;
			cachedSunCurve = null;
			openElevationCurveCache.clear();
			fullElevationCurveCache.clear();
			closedElevationCurveCache.clear();
			skyBackgroundCurveCache.clear();
		}
	}


	@Override
	public void setModel(final Variant newModel) {
		super.setModel(newModel);
		clearShapeCaches();
		repaint();
	}

	@Override
	public void paint(final Graphics g) {

		// Preview only if the pref is set. Ugh.
        final Obs preview = (BooleanViewPreference.SHOW_IN_VISUALIZER.get() && selection.isEmpty() && dragObject.isEmpty())
                        ? this.preview
                        : null;

		// Normally ignoring repaint only applies to system-generated events, but we want this
		// behavior even with explicit calls to repaint(). This is mostly in response to QPT-185
		// which requires this behavior to avoid flickering when starting an alloc drag.
		if (getIgnoreRepaint())
			return;

		// Call the superclass paint() ... it sets up all of our transforms, so nothing
		// will work unless we do this
		super.paint(g);

		// Only paint if there is a model and current variant. Otherwise it's just a blank
		// background. As always, we draw back-to-front.
		if (model != null) {
			// Some collections
			final SortedSet<Block> blocks = model.getSchedule().getBlocks();
			final SortedSet<Alloc> allocs = model.getAllocs();

			// Set up for pretty rendering.
			final Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHints(RENDERING_HINTS);

			// Background stuff: night, blocks, sun, moon, elevation lines
			paintNight(g2d);
			blocks.forEach(b -> paintBlock(g2d, b));
 			paintSun(g2d);
			paintMoon(g2d);
			paintElevationLines(g2d);

			// Elevation lines (unselected)
			g2d.setColor(Color.GRAY);
			g2d.setStroke(DOTTED_STROKE_LIGHT);
			allocs.stream().filter(a -> !selection.contains(a)).forEach(a ->
			    g2d.draw(getElevationCurve(a.getObs()::getCoords, minTime, maxTime, false, a.getObs().getObsId(), fullElevationCurveCache))
            );

			// Moon Phase
			{
				final long when = (maxTime + minTime) / 2;
				final ImprovedSkyCalc calc = new ImprovedSkyCalc(model.getSchedule().getSite());
				calc.calculate(new WorldCoords(0, 0), new Date(when), true);

				final int p = MoonCalc.approximatePeriod(System.currentTimeMillis());
				final long full = MoonCalc.getMoonTime(p, MoonCalc.Phase.FULL);
				final MoonLabel label = new MoonLabel(16, calc.getLunarIlluminatedFraction(), System.currentTimeMillis() < full);
				label.setSize(label.getPreferredSize());

				final AffineTransform t = g2d.getTransform();
				g2d.translate(getWidth() - label.getSize().width - 20, 20);
				label.paint(g);
				g2d.setTransform(t);
			}

			// Area under curve segment (all allocs and drag)
			allocs.forEach(a -> {
			    final Obs obs = a.getObs();
				final Color c = getColor(a);
				g2d.setColor(c);

				// Paint cals in a striped pattern
				if (obs.getObsClass().isCalibration()) {
					// TODO: cache this up
					final BufferedImage bi = new BufferedImage(4, 4,  BufferedImage.TYPE_4BYTE_ABGR);

					final int rgb = c.getRGB();
					bi.setRGB(2, 0, rgb);
					bi.setRGB(3, 0, rgb);
					bi.setRGB(1, 1, rgb);
					bi.setRGB(2, 1, rgb);
					bi.setRGB(0, 2, rgb);
					bi.setRGB(1, 2, rgb);
					bi.setRGB(0, 3, rgb);
					bi.setRGB(3, 3, rgb);

					final TexturePaint tp = new TexturePaint(bi, new Rectangle2D.Double(0, 0, bi.getWidth(), bi.getHeight()));
					g2d.setPaint(tp);
				}

				g2d.fill(getElevationCurve(obs::getCoords, a.getStart(), a.getEnd(), true, obs.getObsId(), closedElevationCurveCache));
			});

			if (dragObject != null) {
				g2d.setColor(DRAG_COLOR);
				dragObject.forEach(a -> {
				    final Obs obs = a.getObs();
                    g2d.fill(getElevationCurve(obs::getCoords, a.getStart() + dragDelta, a.getEnd() + dragDelta, true, obs.getObsId(), closedElevationCurveCache));
                });
			}

			// Group bars
			paintGroupBars(g2d);

			// Curve segment outline (all allocs)
			g2d.setColor(Color.GRAY);
			g2d.setStroke(SOLID_STROKE_LIGHT);
			allocs.forEach(a -> {
			    final Obs obs = a.getObs();
                g2d.draw(getElevationCurve(obs::getCoords, a.getStart(), a.getEnd(), false, obs.getObsId(), openElevationCurveCache));
            });

			// Boundary bars (unselected allocs)
            allocs.stream().filter(a -> !selection.contains(a)).forEach(a -> {
                final Line2D left = new Line2D.Double(a.getStart(), 0, a.getStart(), MAX_DEG);
                final Line2D right = new Line2D.Double(a.getEnd(), 0, a.getEnd(), MAX_DEG);
                g2d.draw(timeAlt2XY.createTransformedShape(left));
                g2d.draw(timeAlt2XY.createTransformedShape(right));
            });

			// SB curve (only if dragging or selected or preview)
			if (dragObject.size() == 1 || selection.size() == 1 || preview != null) {
				paintSkyBrightnessLines(g2d);
				final String obsId;
				final Function <Long, WorldCoords> coords;

				if (!dragObject.isEmpty()) {
					obsId = dragObject.first().getObs().getObsId();
					coords = dragObject.first().getObs()::getCoords;
				} else if (!selection.isEmpty()) {
					obsId = selection.first().getObs().getObsId();
					coords = selection.first().getObs()::getCoords;
				} else {
					obsId = preview.getObsId();
					coords = preview::getCoords;
				}

				g2d.setColor(SB_COLOR);
				g2d.setStroke(SB_STROKE);
				g2d.draw(getSkyBackgroundCurve(coords, minTime, maxTime, obsId));
			}

			// Setup lines (selection allocs)
            g2d.setColor(Color.BLACK);
            g2d.setStroke(DOTTED_STROKE_LIGHT);
            allocs.stream().filter(a -> selection.contains(a) && a.getSetupType() != Alloc.SetupType.NONE).forEach(a -> {
                final long setupEnd = a.getStart() + a.getSetupTime();
                final ImprovedSkyCalc calc = new ImprovedSkyCalc(model.getSchedule().getSite());
                calc.calculate(a.getObs().getCoords(setupEnd), new Date(setupEnd), false);
                final Line2D setup = new Line2D.Double(setupEnd, 0, setupEnd, calc.getAltitude());
                g2d.draw(timeAlt2XY.createTransformedShape(setup));
            });

			// Timing blocks
            final int twrgb = new Color(0x00, 0x00, 0x80, 0xAA).getRGB();
			if (dragObject != null || selection.size() == 1) {
                StreamSupport.stream(dragObject.spliterator(), false)
                        .map(Alloc::getObs)
                        .filter(o -> !(o.getTimingWindows().isEmpty() && LttsServicesClient.getInstance().getObservation(o) == null))
                        .forEach(o ->
                                new TimingWindowSolver(o, false).solve(minTime, maxTime).forEach(i -> {
                                    // TODO: cache this up
                                    final BufferedImage bi = new BufferedImage(4, 4,  BufferedImage.TYPE_4BYTE_ABGR);
                                    bi.setRGB(0, 0, twrgb);
                                    bi.setRGB(2, 0, twrgb);
                                    bi.setRGB(3, 1, twrgb);
                                    bi.setRGB(0, 2, twrgb);
                                    bi.setRGB(2, 2, twrgb);
                                    bi.setRGB(1, 3, twrgb);

                                    final TexturePaint tp = new TexturePaint(bi, new Rectangle2D.Double(0, 0, bi.getWidth(), bi.getHeight()));
                                    g2d.setPaint(tp);

                                    final Shape rect = timeAlt2XY.createTransformedShape(new Rectangle2D.Double(i.getStart(), MIN_DEG, i.getLength(), MAX_DEG - MIN_DEG));
                                    g2d.fill(rect);
                                })
                        );
			}

			// Elevation lines and boundary bars (selected and drag)
			g2d.setColor(Color.DARK_GRAY);
			g2d.setStroke(SOLID_STROKE);
			selection.forEach(a -> {
			    final Obs obs = a.getObs();
                g2d.draw(getElevationCurve(obs::getCoords, minTime, maxTime, false, obs.getObsId(), fullElevationCurveCache));

                final Line2D left = new Line2D.Double(a.getStart(), 0, a.getStart(), MAX_DEG);
                final Line2D right = new Line2D.Double(a.getEnd(), 0, a.getEnd(), MAX_DEG);
                g2d.draw(timeAlt2XY.createTransformedShape(left));
                g2d.draw(timeAlt2XY.createTransformedShape(right));
            });

			if (preview != null) {
				g2d.draw(getElevationCurve(preview::getCoords, minTime, maxTime, false, preview.getObsId(), fullElevationCurveCache));
			}

			if (dragObject != null) {
			    dragObject.forEach(a -> {
			        final Obs obs = a .getObs();
                    final long dragStartTime = a.getStart() + dragDelta;
                    final long dragEndTime = a.getEnd() + dragDelta;
                    final Line2D left = new Line2D.Double(dragStartTime, 0, dragStartTime, MAX_DEG);
                    final Line2D right = new Line2D.Double(dragEndTime, 0, dragEndTime, MAX_DEG);
                    g2d.draw(timeAlt2XY.createTransformedShape(left));
                    g2d.draw(timeAlt2XY.createTransformedShape(right));
                    g2d.draw(getElevationCurve(obs::getCoords, minTime, maxTime, false, obs.getObsId(), fullElevationCurveCache));
                });
			}

			allocs.forEach(a -> {
				final double end = a.getEnd();
				final double start = a.getStart();
				final Graphics2DAttributes g2da = new Graphics2DAttributes(g2d);
				final Point2D pos = new Point2D.Double((end + start) / 2f , 10f);
				timeAlt2XY.transform(pos, pos);
				final float nudge = LABEL_FONT.getSize() / 2f;
				g2d.setFont(LABEL_FONT);
				g2d.setColor(Color.BLACK);
				g2d.transform(AffineTransform.getRotateInstance(- Math.PI * 0.5, pos.getX(), pos.getY()));
				g2d.drawString(a.getObs().toString(), (float) pos.getX(), (float) pos.getY() + nudge);
				g2da.restore();
			});

			paintTimeTicks(g2d);

            // LCH-117 shutter windows:
            // - For each planned observation show those shuttering windows which occur between the currently scheduled
            //   start and end times of the observation (use e.g. an light red/orange color)
            // - For the currently selected observation show all shuttering windows (use e.g. red color to differentiate
            //   it from shuttering windows of all other planned observations).
            // LCH-186: When moving an observation only show windows for this one observation.
            if (dragObject != null && !dragObject.isEmpty()) {
                // Treat drag object the same as a selected object
                dragObject.forEach(a -> paintShutteringWindows(g2d, a, true));
            } else {
                dragObject.forEach(a -> paintShutteringWindows(g2d, a, selection.contains(a)));
            }
        }
	}


	private void paintElevationLines(final Graphics2D g2d) {
		final Graphics2DAttributes g2da = new Graphics2DAttributes(g2d);
		g2d.setColor(COLOR_ELEVATION_LINE);

		switch (ElevationPreference.BOX.get()) {
		case AIRMASS:
			// Draw airmass lines every so often from 0 - 90 deg. The one at 30 is special
			// because it represents airmass 2.0 (more or less), so we use a different style.
			for (long elevation = MIN_DEG; elevation < MAX_DEG; elevation += 10) {
				// Create and draw the line.
				final Shape line = alt2Y.createTransformedShape(new Line2D.Double(0, elevation, getSize().getWidth(), elevation));
				g2d.setStroke(elevation == 30 ? SOLID_STROKE_LIGHT : DOTTED_STROKE_LIGHT);
				g2d.draw(line);

				// And the label, off to the left. Don't need one at zero.
				if (elevation != 0) {
					g2d.setFont(LABEL_FONT);
					g2d.setColor(LABEL_COLOR);

                    // Cheat here, so we show 2.00 instead of 1.99.
					final double airmass = ImprovedSkyCalc.getAirmass(elevation);
					final double displayedAirmass = Math.abs(2.00 - airmass) < 0.01 ? 2.00 : airmass;
					final String label = String.format(" %1.2f", displayedAirmass);
					g2d.drawString(label, 0, line.getBounds().y + LABEL_FONT.getSize2D() / 2);
				}
			}
			break;

		case ELEVATION:
			// Draw elevation lines every so often from 0 - 90 deg. The one at 30 is special
			// because it represents airmass 2.0 (more or less), so we use a different style.
			for (long elevation = MIN_DEG; elevation < MAX_DEG; elevation += 10) {
				// Create and draw the line.
				final Shape line = alt2Y.createTransformedShape(new Line2D.Double(0, elevation, getSize().getWidth(), elevation));
				g2d.setStroke(elevation == 30 ? SOLID_STROKE_LIGHT : DOTTED_STROKE_LIGHT);
				g2d.draw(line);

				// And the label, off to the left. Don't need one at zero.
				if (elevation != 0) {
					g2d.setFont(LABEL_FONT);
					g2d.setColor(LABEL_COLOR);
					g2d.drawString(" " + elevation + "\u00B0", 0, line.getBounds().y + LABEL_FONT.getSize2D() / 2);
				}
			}
			break;
		}

		g2da.restore();
	}


	private void paintSkyBrightnessLines(final Graphics2D g2d) {
		final Graphics2DAttributes g2da = new Graphics2DAttributes(g2d);
		g2d.setStroke(DOTTED_STROKE);
        byte[] percentages = { 20, 50, 80 };
        for (final byte pct: percentages) {
            final double sb = Conds.getBrightestMagnitude(pct);

			// Create and draw the line.
			final Shape line = sb2Y.createTransformedShape(new Line2D.Double(0, sb, getSize().getWidth(), sb));
			g2d.setColor(SB_COLOR);
			g2d.draw(line);

			// And the label, off to the right. Don't need one at zero.
			g2d.setFont(LABEL_FONT);
			g2d.setColor(LABEL_COLOR);
			g2d.drawString(pct + "%", line.getBounds().width - g2d.getFontMetrics().getWidths()['0'] * 4, line.getBounds().y + LABEL_FONT.getSize2D() / 2);

		}
		g2da.restore();
	}

	private void renderCurve(final Graphics2D g2d, final Consumer<Shape> renderFunc, final Option<Stroke> stroke, final Option<Color> color, final Shape curve) {
        final Graphics2DAttributes g2da = new Graphics2DAttributes(g2d);
        stroke.forEach(g2d::setStroke);
        color.forEach(g2d::setColor);
        renderFunc.accept(curve);
        g2da.restore();
    }

    private void paintCurve(final Graphics2D g2d, final Stroke stroke, final Color color, final Shape curve) {
	    renderCurve(g2d, g2d::draw, ImOption.apply(stroke), ImOption.apply(color), curve);
    }

    private void fillCurve(final Graphics2D g2d, final Color color, final Shape curve) {
        renderCurve(g2d, g2d::fill, ImOption.empty(), ImOption.apply(color), curve);
    }

	private void paintMoon(final Graphics2D g2d) {
	    paintCurve(g2d, MOON_STROKE, MOON_COLOR, getMoonCurve(minTime, maxTime));
	}


	private void paintSun(final Graphics2D g2d) {
        paintCurve(g2d, SUN_STROKE, SUN_COLOR, getSunCurve(minTime, maxTime));
	}

	private void paintNight(final Graphics2D g2d) {
		final Site site = model.getSchedule().getSite();
		final TwilightBoundType tbt = LocalSunriseSunset.forSite(site);
		final TwilightBoundedNight night = new TwilightBoundedNight(tbt, model.getSchedule().getStart(), site);

		// Night is just a colored rectangle.
		final Shape rect = time2X.createTransformedShape(new Rectangle2D.Double(night.getStartTime(), 0, night.getTotalTime(), getSize().getHeight()));
        fillCurve(g2d, NIGHT_COLOR, rect);
	}

	private void paintBlock(final Graphics2D g2d, final Block b) {
		// Blocks are just colored rectangles.
		final Shape rect = time2X.createTransformedShape(new Rectangle2D.Double(b.getStart(), 0, b.getLength(), getSize().getHeight()));
		fillCurve(g2d, BLOCK_COLOR, rect);
	}

    private void paintShutteringWindows(final Graphics2D g2d, final Alloc a, final boolean selected) {
        final Obs obs = a.getObs();
        if (!obs.getLGS()) return; // only display for LGC observations
        final Observation observation = LttsServicesClient.getInstance().getObservation(obs);
        if (observation == null) return;

   		final Graphics2DAttributes g2da = new Graphics2DAttributes(g2d);
   		observation.getTargetsSortedByType().forEach(observationTarget -> {
            if (observationTarget.isScience()) {
                g2d.setPaint(selected ? SELECTED_SHUTTERING_WINDOW_COLOR : SHUTTERING_WINDOW_COLOR);
            } else {
                g2d.setPaint(selected ? SELECTED_WFS_SHUTTERING_WINDOW_COLOR : SHUTTERING_WFS_WINDOW_COLOR);
            }

            observationTarget.getLaserTarget().getShutteringWindows().stream()
                    .filter(shutteringWindow -> selected || a.overlaps(new Interval(shutteringWindow.getStart().getTime(), shutteringWindow.getEnd().getTime()), Interval.Overlap.EITHER))
                    .forEach(shutteringWindow -> {
                        final long start = shutteringWindow.getStart().getTime(), end = shutteringWindow.getEnd().getTime();
                        final Shape rect = time2X.createTransformedShape(new Rectangle2D.Double(start, 0, Math.max(end - start, MIN_SHUTTER_WINDOW_MS), getSize().getHeight()));
                        g2d.fill(rect);
                    });
        });

   		g2da.restore();
    }

	private void paintTimeTicks(final Graphics2D g2d) {
		final Graphics2DAttributes g2da = new Graphics2DAttributes(g2d);

		// Ok, we want to paint a tick at each hour between twilights. Start with
		// the min time and then push it forward to the next hour, then one hour more.
		final Calendar cal = Calendar.getInstance();
		final TimePreference tp = timePreference != null ? timePreference : TimePreference.BOX.get();

		switch (tp) {
		case LOCAL:
			cal.setTimeZone(model.getSchedule().getSite().timezone());
			break;

		case SIDEREAL:
		case UNIVERSAL:
			cal.setTimeZone(TimeZone.getTimeZone("UTC"));
			break;
		}


		final long minTime = this.minTime + PADDING;
		final ImprovedSkyCalc calc = new ImprovedSkyCalc(model.getSchedule().getSite());

		final long nudge;
		switch (tp) {
		case LOCAL:
		case UNIVERSAL:
			nudge = TimeUtils.MS_PER_HOUR - minTime % TimeUtils.MS_PER_HOUR;
			break;

		case SIDEREAL:
			long siderealMinTime = calc.getLst(new Date(minTime)).getTime();
			nudge = TimeUtils.MS_PER_HOUR - siderealMinTime % TimeUtils.MS_PER_HOUR;
			break;

		default:
			throw new Error("Impossible.");
		}


		final long start = (minTime + nudge); // + TimeUtils.MS_PER_HOUR;
		for (long time = start; time < maxTime - PADDING; time += TimeUtils.MS_PER_HOUR) {
			// The tick mark: tick y values in elevation degrees
			final Shape tick = timeAlt2XY.createTransformedShape(new Line2D.Double(time, MIN_DEG, time, MIN_DEG + 10));
			g2d.setColor(Color.GRAY);
			g2d.draw(tick);

			final long normalOrSiderealTime;
			switch (tp) {
			case LOCAL:
			case UNIVERSAL:
				normalOrSiderealTime = time;
				break;

			case SIDEREAL:
				normalOrSiderealTime = calc.getLst(new Date(time)).getTime();
				break;

			default:
				throw new Error("Impossible.");
			}


			// And the label
			cal.setTimeInMillis(normalOrSiderealTime);
			final String hour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
			g2d.setFont(LABEL_FONT);
			g2d.setColor(LABEL_COLOR);
			g2d.drawString(hour, tick.getBounds().x - 5 /* HACK: do this correctly */, getHeight() - 7);

			// If we're at the very beginning, draw the timezone
			if (time == start) {
				final String tz;
				switch (tp) {
				case LOCAL:
					final TimeZone timeZone = model.getSchedule().getSite().timezone();
					final boolean daylight = timeZone.inDaylightTime(new Date(minTime));
					tz = timeZone.getDisplayName(daylight, TimeZone.SHORT);
					break;
				case SIDEREAL:  tz = "LST"; break;
				case UNIVERSAL: tz = "UTC"; break;
				default:
					throw new Error("Impossible.");
				}
				g2d.drawString(tz, tick.getBounds().x - 35, getHeight() - 7);
			}
		}

		g2da.restore();
	}

	private void paintGroupBars(final Graphics2D g2d) {
		final Graphics2DAttributes g2da = new Graphics2DAttributes(g2d);
		g2d.setStroke(SOLID_STROKE_LIGHT);

		model.getAllocs().forEach(a -> {
		    final int i = a.getGroupIndex();
		    if (i != -1) {
                final Shape rect = time2X.createTransformedShape(new Rectangle2D.Double(a.getStart(), 0, a.getLength(), 7));
                g2d.setColor(ColorWheel.get(i));
                g2d.fill(rect);

                final Shape line = time2X.createTransformedShape(new Line2D.Double(a.getStart(), 7, a.getEnd(), 7));
                g2d.setColor(Color.GRAY);
                g2d.draw(line);
            }
        });

		g2da.restore();
	}

	/**
	 * Returns the elevation curve for the given object between start and end times,
	 * optionally as a closed shape corresponding to the area under the curve. These shapes
	 * are cached.
	 */
	private Shape getElevationCurve(final Function<Long, WorldCoords> coords, final long start, final long end,
                                    final boolean close, final String obsId, final Map<String, CachedShape> cache) {
		synchronized (cacheLock) {

			// Return the cached shape, if any.
			final CachedShape cs = cache.get(obsId);
			if (cs != null && cs.matches(start, end))
				return cs.shape;

			// Otherwise build the shape.
			final ImprovedSkyCalc calc = new ImprovedSkyCalc(model.getSchedule().getSite());
			final GeneralPath path = new GeneralPath();
			if (close) path.moveTo(start, 0);
			for  (long t = start; t < end; t += INTEGRATION_STEP) {
				calc.calculate(coords.apply(t), new Date(t), false);
				if (t == start && !close) {
					path.moveTo(t, (float) calc.getAltitude());
				} else {
					path.lineTo(t, (float) calc.getAltitude());
				}
			}
			calc.calculate(coords.apply(end), new Date(end), false);
			path.lineTo(end, (float) calc.getAltitude());
			if (close) {
				path.lineTo(end, 0);
				path.closePath();
			}

			// Cache and return it.
  			final CachedShape csNew = new CachedShape(timeAlt2XY.createTransformedShape(path), start, end);
			cache.put(obsId, csNew);
			return csNew.shape;
		}
	}


	private Shape getSkyBackgroundCurve(final Function<Long, WorldCoords> coords, final long start, final long end, final String obsId) {
		synchronized (cacheLock) {

			// Return the cached shape, if any.
			final Map<String, CachedShape> cache = skyBackgroundCurveCache;
			final CachedShape cs = cache.get(obsId);
			if (cs != null && cs.matches(start, end))
				return cs.shape;

			// Otherwise build the shape.
			final ImprovedSkyCalc calc = new ImprovedSkyCalc(model.getSchedule().getSite());
			final GeneralPath path = new GeneralPath();
			boolean hop = true;
			for (long t = start; t < end; t += INTEGRATION_STEP) {
				calc.calculate(coords.apply(t), new Date(t), true);
				if (calc.getAltitude() > 0) {
					if (hop) {
						path.moveTo(t, calc.getTotalSkyBrightness().floatValue());
						hop = false;
					} else {
						path.lineTo(t, calc.getTotalSkyBrightness().floatValue());
					}
				} else {
					hop = true;
				}
			}
			calc.calculate(coords.apply(end), new Date(end), true);
			if (!hop)
				path.lineTo(end, calc.getTotalSkyBrightness().floatValue());

			// Cache and return it.
			final CachedShape csNew = new CachedShape(timeSB2XY.createTransformedShape(path), start, end);
			cache.put(obsId, csNew);
			return csNew.shape;

		}
	}

	private Shape getMoonCurve(final long start, final long end) {
		synchronized (cacheLock) {

			// Try to use the cached version if we can.
			if (cachedMoonCurve != null && cachedMoonCurve.matches(start, end))
				return cachedMoonCurve.shape;

			// Cache lookup failed, oh well. Calculate the moon's path.
			final ImprovedSkyCalc calc = new ImprovedSkyCalc(model.getSchedule().getSite());
			final GeneralPath path = new GeneralPath();
			final WorldCoords coords = new WorldCoords(0, 0);

			for (long t = start; t < end; t += TimeUtils.MS_PER_MINUTE / 2) {
				calc.calculate(coords, new Date(t), true);
				float alt = (float) calc.getLunarElevation();
				if (t == start) {
					path.moveTo(t, alt);
				} else {
					path.lineTo(t, alt);
				}
			}
			calc.calculate(new WorldCoords(), new Date(end), true);
			path.lineTo(end, (float) calc.getLunarElevation());

			// Done. Cache our result and return it.
			cachedMoonCurve = new CachedShape(timeAlt2XY.createTransformedShape(path), start, end);
			return cachedMoonCurve.shape;
		}
	}

	private Shape getSunCurve(final long start, final long end) {
		synchronized (cacheLock) {

			// Try to use the cached version if we can.
			if (cachedSunCurve != null && cachedSunCurve.matches(start, end))
				return cachedSunCurve.shape;

			// Cache lookup failed, oh well. Calculate the sun path.
			final ImprovedSkyCalc calc = new ImprovedSkyCalc(model.getSchedule().getSite());
			final GeneralPath path = new GeneralPath();
			for (long t = start; t < end; t += TimeUtils.MS_PER_MINUTE / 2) {
				calc.calculate(new WorldCoords(), new Date(t), true);
				float alt = (float) calc.getSunAltitude();
				if (t == start) {
					path.moveTo(t, alt);
				} else {
					path.lineTo(t, alt);
				}
			}
			calc.calculate(new WorldCoords(), new Date(end), true);
			path.lineTo(end, (float) calc.getSunAltitude());

			// Done. Cache our result and return it.
			cachedSunCurve = new CachedShape(timeAlt2XY.createTransformedShape(path), start, end);
			return cachedSunCurve.shape;
		}
	}

	private Color getColor(final Alloc a) {
		Color c = ALLOC_COLOR;
		final Severity s = a.getSeverity(qcOnly);
		if (s != null) {
			switch (s) {
			    case Error:
			        c = ALLOC_COLOR_ERR;
			        break;
			    case Warning:
			        c = ALLOC_COLOR_WARN;
			        break;
			}
		}

		if (selection.contains(a)) {
			// TODO: cache
			c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 224);
		}
		return c;
	}

	/**
	 * Fine-grained struct used for caching. Just a shape plus the start/end times that it
	 * represents.
	 */
	private static class CachedShape {

		final Shape shape;
		final long start;
		final long end;

		CachedShape(final Shape shape, final long start, final long end) {
			this.shape = shape;
			this.start = start;
			this.end = end;
		}

		boolean matches(long start, long end) {
			return start == this.start && end == this.end;
		}
	}
}
