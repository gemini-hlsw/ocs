package edu.gemini.qpt.ui.view.visualizer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

import edu.gemini.qpt.shared.util.TimeUtils;

/**
 * Just a place to hang the constants.
 */
@SuppressWarnings("serial")
public interface VisualizerConstants {

    Color ALLOC_COLOR = new Color(64, 128, 64, 128);
    Color ALLOC_COLOR_WARN = new Color(224, 196, 32, 128);
    Color ALLOC_COLOR_ERR = new Color(224, 64, 64, 128);


    Color DRAG_COLOR = new Color(0, 0, 0, 128);
    Color NIGHT_COLOR = new Color(0xDD, 0xDD, 0xDD);
    Color BLOCK_COLOR = new Color(0xCC, 0xCC, 0xCC);
    Color SELECTED_SHUTTERING_WINDOW_COLOR = new Color(255, 0, 0, 64);
    Color SHUTTERING_WINDOW_COLOR = new Color(255, 128, 0, 64);

    // Used to display a striped area for non-science target shutter windows
    Color SHUTTERING_WINDOW_GRADIENT_COLOR = new Color(255, 0, 0, 16);
    Paint SELECTED_WFS_SHUTTERING_WINDOW_COLOR = new GradientPaint(0, 2.5F, SELECTED_SHUTTERING_WINDOW_COLOR, 0, 5, SHUTTERING_WINDOW_GRADIENT_COLOR, true);
    Paint SHUTTERING_WFS_WINDOW_COLOR = new GradientPaint(0, 2.5F, SHUTTERING_WINDOW_COLOR, 0, 5, SHUTTERING_WINDOW_GRADIENT_COLOR, true);

    long  MIN_SHUTTER_WINDOW_MS = TimeUtils.MS_PER_MINUTE;
    Color DAY_COLOR = Color.WHITE;
    Color COLOR_ELEVATION_LINE = Color.BLACK;
    Color MOON_COLOR = new Color(240, 240, 240, 128);
  
    Color SUN_COLOR = new Color(212, 212, 96, 128);
    Color LABEL_COLOR = Color.BLACK;
    Color SB_COLOR = new Color(0xD0EDFF);

    Color TIMING_WINDOW_COLOR = new Color(0x00, 0x00, 0x80, 0xAA);

    Stroke MOON_STROKE = new BasicStroke(5.0f);
    Stroke SUN_STROKE = new BasicStroke(5.0f);
    Stroke SOLID_STROKE = new BasicStroke(1.0f);
    Stroke SOLID_STROKE_LIGHT = new BasicStroke(0.5f);

    Stroke SB_STROKE = new BasicStroke(2.0f);

    Stroke DOTTED_STROKE = new BasicStroke(
            1.0f, // width
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            1.0f,
            new float[]{3, 2},
            0);

    Stroke DOTTED_STROKE_LIGHT = new BasicStroke(
            0.25f, // width
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            1.0f,
            new float[]{3, 2},
            0);

    long INTEGRATION_STEP = 60 * TimeUtils.MS_PER_SECOND;

    Map<Object, Object> RENDERING_HINTS = new HashMap<Object, Object>() {{
        put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }};
}
