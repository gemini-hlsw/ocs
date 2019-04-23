// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ObserveTimeLine.java 38416 2011-11-07 14:19:47Z swalker $
//
package jsky.app.ot.editor.seq;

import edu.gemini.shared.util.EventSupport;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import jsky.science.Time;
import jsky.timeline.DefaultTimeLineModel;
import jsky.timeline.IllegalNodePositionException;
import jsky.timeline.TimeLine;
import jsky.timeline.TimeLineNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;


/**
 * A utility class used to manage a TimeLine object displaying
 * the sequence of iterations for the current observation.
 *
 * @author Allan Brighton (then hacked by Shane)
 */
final class ObserveTimeLine extends TimeLine implements VetoableChangeListener {

    public static final class ObserveTimeLineEvent extends EventObject {
        public final ObserveTimeLineNode node;

        public ObserveTimeLineEvent(ObserveTimeLine source, ObserveTimeLineNode node) {
            super(source);
            this.node = node;
        }
    }

    public interface ObserveTimeLineListener extends EventListener {
        // Called via reflection (EventSupport)... don't delete.
        void showSetup(ObserveTimeLineEvent event);
        void showStep(ObserveTimeLineEvent event);
    }

    private final EventSupport support = new EventSupport(ObserveTimeLineListener.class, ObserveTimeLineEvent.class);
    private final Map<String, ObserveTimeLineNode> nodeMap = new HashMap<String, ObserveTimeLineNode>();

    private ObserveTimeLineNode selected = null;


    /**
     * Initialize with the TimeLine widget and the observation node.
     */
    public ObserveTimeLine() {
        setMode("");  // disable selections
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        removeMouseListener(_mouseListener);
        removeMouseMotionListener(_mouseDragListener);
        removeKeyListener(_keyListener);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                ObserveTimeLineNode node = findNode(e.getPoint());
                selectNode(node);
            }
        });
    }

    void addObserveTimeLineListener(ObserveTimeLineListener listener) { support.addListener(listener); }

    private void addNode(ObserveTimeLineNode node) {
        try {
            addTimeLineNodeNoEvents(node);
        } catch (IllegalNodePositionException ex) {
            ex.printStackTrace();
        }
    }

    /** Update the timeline to display the sequence for the current observation */
    public void update(PlannedTime plannedTime) {
        nodeMap.clear();

        double totalTime = plannedTime.totalTime() / 1000.0;
        int segments = plannedTime.steps.size() + 1;
        setModel(new DefaultTimeLineModel(new Time(0.), new Time(totalTime), segments));


        // add a setup time node
        ObserveTimeLineNode setup = new ObserveTimeLineNode(0, plannedTime, -1);
        addNode(setup);
        setup.addVetoableChangeListener(this);

        // add nodes to node list for the sequence

        double startTime = plannedTime.setup.time.toDuration().toMillis() / 1000.0;
        for (int step=0; step<plannedTime.steps.size(); ++step) {
            ObserveTimeLineNode n = new ObserveTimeLineNode(startTime, plannedTime, step);
            addNode(n);
            n.addVetoableChangeListener(this);
            startTime += plannedTime.steps.get(step).totalTime() / 1000.0;

            String lab = plannedTime.sequence.getItemValue(step, Keys.DATALABEL_KEY).toString();
            nodeMap.put(lab, n);
        }

        support.fireEvent(new ObserveTimeLineEvent(this, setup), "showSetup");
    }

    public void selectDatasetNode(String label) {
        selectNode(nodeMap.get(label));
    }

    public ObserveTimeLineNode getSelectedNode() { return selected; }

    private void selectNode(ObserveTimeLineNode n) {
        if (selected == n) return;
        if (selected != null) selected.setSelectionMode(TimeLineNode.UNSELECTED);

        String method;
        if ((n == null) || (n.step < 0)) {
            method = "showSetup";
        } else {
            n.setSelectionMode(TimeLineNode.NODE_SELECTED);
            method = "showStep";
        }
        selected = n;
        support.fireEvent(new ObserveTimeLineEvent(this, n), method);

        repaint();
    }

    private ObserveTimeLineNode findNode(Point pt) {
        for (TimeLineNode n : _nodes) {
            if (n.containsPoint(pt)) return (ObserveTimeLineNode) n;
        }
        return null;
    }


    @Override public Point getToolTipLocation(MouseEvent event) {
        Point p = event.getPoint();
        return new Point(p.x + 10, p.y + 10);
    }

    @Override protected void paintCenterLine(Graphics2D g) {
        // override to ignore this, don't want it
    }

    @Override protected void paintStartLabel(Graphics2D g) {
        // override to ignore this, don't want it
    }

    @Override protected void paintEndLabel(Graphics2D g) {
        // override to ignore this, don't want it
    }

    @Override protected void handleMouseClicked(MouseEvent evt) {
        // do nothing
    }

    @Override protected void handleMousePressed(MouseEvent evt) {
        // do nothing
    }

    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        // do nothing
    }
}


