package edu.gemini.pit.ui.util;

import org.jdesktop.swingx.calendar.DateSpan;
import org.jdesktop.swingx.calendar.JXMonthView;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;

@SuppressWarnings("serial")
public class CalendarPanel extends JPanel {

	private final JXMonthView monthView;
	
	private long startDate, endDate;
	
	public CalendarPanel(long _startDate, long _endDate) {
		
		this.startDate = _startDate;
		this.endDate = _endDate;

		monthView = new JXMonthView() {{

			// TODO: figure out how to get timezones working. If you use GMT it has all kinds
			// of bad problems, including not knowing how many days in a month, etc.
			// setTimeZone(TimeZone.getTimeZone("GMT"));

			// The crazy borders and insets were arrived at by experimentation.
			// The default geometry is really bad. This is better. Due to the use
			// of negative insets, modfications to this code may have surprising
			// results.

			Border b1 = BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY),
					BorderFactory.createEmptyBorder(11, 4, 0, 4));
			
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createEmptyBorder(0, 0, 1, 1), b1));
			
			setMonthStringInsets(new Insets(-3, 0, -3, 0));
							
			// Other options
			setPreferredCols(1);
            setFirstDisplayedDate(startDate);
			setSelectedDateSpan(new DateSpan(startDate,  endDate));
			setSelectionMode(JXMonthView.SINGLE_SELECTION); // TODO: change to allow multi-night
			addActionListener(new ActionListener() {				
				public void actionPerformed(ActionEvent e) {
					DateSpan span = getSelectedDateSpan();
					startDate = span.getStart();
					endDate = span.getEnd();
				}				
			});
			
		}};
		
		setLayout(new BorderLayout());
		add(monthView, BorderLayout.CENTER);
	
		add(new NavButton("<", -1), BorderLayout.WEST);
		add(new NavButton(">",  1), BorderLayout.EAST);

	}
	
	public long getStartDate() {
		return startDate;
	}
	
	public long getEndDate() {
		return endDate;
	}
	
	private class NavButton extends JButton {
		public NavButton(String text, final int delta) {
			super(text);
			setFocusable(false);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(monthView.getFirstDisplayedDate());
					cal.add(Calendar.MONTH, delta);
					monthView.setFirstDisplayedDate(cal.getTimeInMillis());
				}
			});
		}
	}
	
}
