package edu.gemini.qpt.ui.view.candidate;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.ui.gface.GComparator;
import edu.gemini.ui.gface.GTableViewer;
import edu.gemini.ui.gface.GViewer;

public class CandidateObsComparator extends MouseAdapter implements GComparator<Schedule, Obs>, PropertyChangeListener {

	private GViewer<Schedule, Obs> viewer;
	private JTable table;
	private CandidateObsAttribute attr = CandidateObsAttribute.Observation;
	private int direction = 1;
	private Variant variant;

	public int compare(Obs o1, Obs o2) {		
		
		// If we're sorting on Score, we have to do it here. The trivial comparators defined
		// in CandidateObsAttribute don't know how to track the current variant.
		if (attr == CandidateObsAttribute.Score) {
			if (variant == null) // race condition; gets reset quickly
				return 0;
			int ret = (int) Math.signum(variant.getScore(o1) - variant.getScore(o2));
			return direction * ((ret != 0) ? ret : -o1.compareTo(o2));
		}

		// If we don't have a variant, then we don't have a middle point.
        // This will happen when we've just deleted a variant, for example.
        // In this case, consider all Obs equal since we will have nothing to compare.
        if (variant == null)
            return 0;

		return direction * attr.getComparator(variant.getSchedule().getMiddlePoint()).compare(o1, o2);
	}


	public void propertyChange(PropertyChangeEvent evt) {
		
		// Track the current variant.
		if (Schedule.PROP_CURRENT_VARIANT.equals(evt.getPropertyName())) {
			variant = (Variant) evt.getNewValue();
		}
		
		// And refresh the viewer if we're sorting on the column that depends on the variant.
		if (attr == CandidateObsAttribute.Score)
			viewer.refresh();
		
	}

	public void modelChanged(GViewer<Schedule, Obs> viewer, Schedule oldModel, Schedule newModel) {

		// Hook up to the viewer's table to get clicks. This should probably be built into the
		// viewer logic, but actually it's not that bad to do it here.
		if (this.table == null) {
			this.viewer = viewer;
			table = ((GTableViewer<Schedule, Obs, CandidateObsAttribute>) viewer).getTable();			
			table.getTableHeader().addMouseListener(this);
		}

		// Ok, we need to track the current variant because we need to know the score. 
		// So we want to unhook the old listeners, if any, then hook the new.
		if (oldModel != null) oldModel.removePropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);		
		variant = newModel == null ? null : newModel.getCurrentVariant();				
		if (newModel != null) newModel.addPropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		JTableHeader header = table.getTableHeader();
		int col = header.columnAtPoint(e.getPoint());
		if (col >= 0) {
			String colName = table.getColumnName(col);
			CandidateObsAttribute newAttr = CandidateObsAttribute.valueOf(colName);
			if (attr == newAttr) direction *= -1;
			attr = newAttr;
			viewer.refresh();
		}		
	}

}
