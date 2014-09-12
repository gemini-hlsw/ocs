package edu.gemini.shared.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * A layout manager for simple row/column layout needs.  This layout
 * manager is somewhat like Swing's BoxLayout, but addresses a slightly
 * differenet need.  In particular, the client can specify whether all the
 * components in the row or column should have the same width and whether
 * they should have the same height.  Note that unlike the GridLayoutManager,
 * the common width and height are based upon the component with the maximum
 * width or height, rather than arbitrarily based upon the space available to
 * the container.   Without this feature, common layout tasks like creating
 * a row of buttons that should all be the same width is hard to achieve.
 */
public class SquareLayout implements LayoutManager {

    /**
     * Use ROW_LAYOUT to create a horizontal row of components.
     */
    public static final int ROW_LAYOUT = 0;

    /**
     * Use COL_LAYOUT to create a vertical column of components.
     */
    public static final int COL_LAYOUT = 1;

    /**
     * Default gap to leave between components.
     */
    public static final int GAP_DEFAULT = 5;

    /**
     * This value is used in setting the horizontal alignment and indicates
     * that the components should be right justified.
     */
    public static final int RIGHT_ALIGN = 0;

    /**
     * This value is used in setting the horizontal alignment and indicates
     * that the components should be left justified.
     */
    public static final int LEFT_ALIGN = 1;

    /**
     * This value is used in setting the vertical alignment and indicates
     * that the components should be "top" justified.
     */
    public static final int TOP_ALIGN = 2;

    /**
     * This value is used in setting the vertical alignment and indicates
     * that the components should be "bottom" justified.
     */
    public static final int BOTTOM_ALIGN = 3;

    /**
     * This value is used in setting either the horizontal or vertical
     * alignment and indicates that the components should be centered
     * within thier container.  This is the default alignment for both
     * horizontal and vertical options.
     */
    public static final int CENTER_ALIGN = 4;

    //
    // Information calculated during the determination of the layout size.
    //
    private class LayoutInfo {

        int compW;		// common width of each component (may be -1)

        int compH;		// common height of each component (may be -1)

        int width;		// width of all components + spacing

        int height;		// height of all components + spacing
    }

    private int _dir;   // Which way to layout components, as a row or as a col

    private int _gap;   // Space to leave between the two components.

    private boolean _uniformW;  // Whether all components should have same width

    private boolean _uniformH;  // Whether all components should have same height

    private int _horizontalAlignment = CENTER_ALIGN;

    private int _verticalAlignment = CENTER_ALIGN;

    /**
     * Constructs with a ROW_LAYOUT, a 5 pixel gap between components, and
     * uniform width and height for all components.  Both horizontal and
     * vertical alignment are set to CENTER_ALIGNMENT.
     */
    public SquareLayout() {
        this(ROW_LAYOUT, GAP_DEFAULT, true, true);
    }

    /**
     * Constructs with all the available options except for alignment.  Both
     * horizontal and vertical alignment default to CENTER_ALIGN but may
     * be modified with {@link #setHorizontalAlignment setHorizontalAlignment}
     * and/or {@link #setVerticalAlignment setVerticalAlignment}.
     *
     * @param layoutDirection ROW_LAYOUT for horizontal layout, COL_LAYOUT for
     * vertical layout
     *
     * @param gap the space in pixels to leave between components
     *
     * @param uniformW true if the width of each component should be the same as
     * the maximum width of all the components
     *
     * @param uniformH true if the height of each component should be the same as
     * the maximum height of all the components
     *
     * @exception IllegalArgumentException if <code>layoutDirection</code> is
     * not ROW_LAYOUT or COL_LAYOUT, or if <code>gap</code> is less than 0
     */
    public SquareLayout(int layoutDirection, int gap, boolean uniformW, boolean uniformH) throws IllegalArgumentException {
        if ((layoutDirection != ROW_LAYOUT) && (layoutDirection != COL_LAYOUT)) {
            throw new IllegalArgumentException("bad layout direction: " + layoutDirection + ", should be ROW_LAYOUT or COL_LAYOUT.");
        }
        if (_gap < 0) {
            throw new IllegalArgumentException("gap must be non-negative, not: " + _gap);
        }
        _dir = layoutDirection;
        _gap = gap;
        _uniformW = uniformW;
        _uniformH = uniformH;
    }

    /**
     * Modify the horizontal alignment of components in the target container.
     * The alignment argument must be one of RIGHT_ALIGN, LEFT_ALIGN, or
     * CENTER_ALIGN (the default).
     */
    public void setHorizontalAlignment(int align) {
        switch (align) {
            case RIGHT_ALIGN:
            case LEFT_ALIGN:
            case CENTER_ALIGN:
                _horizontalAlignment = align;
                break;
            default:
                throw new IllegalArgumentException("bad horizontal alignment arg: " + align);
        }
    }

    /**
     * Modify the vertical alignment of components in the target container.
     * The alignment argument must be one of TOP_ALIGN, BOTTOM_ALIGN, or
     * CENTER_ALIGN (the default).
     */
    public void setVerticalAlignment(int align) {
        switch (align) {
            case TOP_ALIGN:
            case BOTTOM_ALIGN:
            case CENTER_ALIGN:
                _verticalAlignment = align;
                break;
            default:
                throw new IllegalArgumentException("bad horizontal alignment arg: " + align);
        }
    }

    /**
     *  No op -- this layout manager isn't tied to a particular container.
     */
    public void addLayoutComponent(String s, Component comp) {
    }

    /**
     *  No op -- this layout manager isn't tied to a particular container.
     */
    public void removeLayoutComponent(Component comp) {
    }

    //
    // Calculate the preferred or minimum size of the target, and also the
    // common width and height that each component should have if _uniformW
    // and/or _uniformH is true respectively.
    //
    private LayoutInfo _getLayoutSize(Container target, boolean preferredSize) {
        int maxW = 0;
        int maxH = 0;
        int totW = 0;
        int totH = 0;
        int ncomps = target.getComponentCount();
        for (int i = 0; i < ncomps; ++i) {
            Component comp = target.getComponent(i);
            Dimension d;
            if (preferredSize) {
                d = comp.getPreferredSize();
            } else {
                d = comp.getMinimumSize();
            }
            if (d.width > maxW)
                maxW = d.width;
            if (d.height > maxH)
                maxH = d.height;
            totW += d.width;
            totH += d.height;
        }

        // Get the w and h if all the components were crammed together
        if (_dir == ROW_LAYOUT) {
            if (_uniformW) {
                totW = maxW * ncomps;
            }
            totH = maxH;
        } else {
            if (_uniformH) {
                totH = maxH * ncomps;
            }
            totW = maxW;
        }

        // Add in the h and v gaps
        if (_dir == ROW_LAYOUT) {
            totW += _gap * Math.max(ncomps - 1, 0);
        } else {
            totH += _gap * Math.max(ncomps - 1, 0);
        }

        // Add in the container's insets
        Insets ins = target.getInsets();
        totW += ins.left + ins.right;
        totH += ins.top + ins.bottom;
        LayoutInfo li = new LayoutInfo();
        li.compW = _uniformW ? maxW : -1;
        li.compH = _uniformH ? maxH : -1;
        li.width = totW;
        li.height = totH;
        return li;
    }

    public Dimension preferredLayoutSize(Container target) {
        LayoutInfo layoutInfo = _getLayoutSize(target, true);
        return new Dimension(layoutInfo.width, layoutInfo.height);
    }

    public Dimension minimumLayoutSize(Container target) {
        LayoutInfo layoutInfo = _getLayoutSize(target, false);
        return new Dimension(layoutInfo.width, layoutInfo.height);
    }

    public void layoutContainer(Container target) {
        LayoutInfo layoutInfo = _getLayoutSize(target, true);
        int ncomps = target.getComponentCount();
        Insets targetIns = target.getInsets();
        int childrenW = layoutInfo.width - targetIns.right - targetIns.left;
        int childrenH = layoutInfo.height - targetIns.top - targetIns.bottom;
        Dimension targetSize = target.getSize();
        int x, y;
        switch (_horizontalAlignment) {
            case LEFT_ALIGN:
                x = targetIns.left;
                break;
            case RIGHT_ALIGN:
                x = targetSize.width - targetIns.right - childrenW;
                break;
            default:
                x = (targetSize.width - childrenW) / 2;
                break;
        }
        switch (_verticalAlignment) {
            case TOP_ALIGN:
                y = targetIns.top;
                break;
            case BOTTOM_ALIGN:
                y = targetSize.height - targetIns.bottom - childrenH;
                break;
            default:
                y = (targetSize.height - childrenH) / 2;
                break;
        }
        for (int i = 0; i < ncomps; ++i) {
            Component comp = target.getComponent(i);
            Dimension d = comp.getPreferredSize();
            int w = (layoutInfo.compW > 0) ? layoutInfo.compW : d.width;
            int h = (layoutInfo.compH > 0) ? layoutInfo.compH : d.height;
            comp.setBounds(x, y, w, h);
            if (_dir == ROW_LAYOUT) {
                x += w + _gap;
            } else {
                y += h + _gap;
            }
        }
    }

}

