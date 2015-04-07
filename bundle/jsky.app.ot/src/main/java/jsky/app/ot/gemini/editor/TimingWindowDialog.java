package jsky.app.ot.gemini.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow;

/**
 * Editor for a TimingWindow object. New it up and then call showEdit(),
 * @author rnorris
 */
@SuppressWarnings("serial")
public class TimingWindowDialog extends JDialog  {
	
	private static final long MS_PER_MINUTE = 1000 * 60;
	private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;

	private static String INSTRUCTIONS =
		"Specify a timing window. Starting time should be in the form YYYY-MM-DD hh:mm:ss " +
		"and will be interpreted in UTC. Window duration and repeat period are specified in " +
		"hours and minutes.";
	
    ///
    /// SET UP TEXT FIELDS
    ///

    private static final SimpleDateFormat UTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static {
    	UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

	JFormattedTextField window = new JFormattedTextField(UTC);
    JFormattedTextField period = new JFormattedTextField(HourMinuteFormat.HH_MM_SS);
    JFormattedTextField times = new JFormattedTextField();    
    JFormattedTextField duration = new JFormattedTextField(HourMinuteFormat.HH_MM);

	JFormattedTextField[] fields = { window, duration, times, period };
		
	{

		DocumentListener dl = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				updateEnabledState();
			}
			public void removeUpdate(DocumentEvent e) {
				updateEnabledState();
			}
			public void insertUpdate(DocumentEvent e) {
				updateEnabledState();
			}
		};

		window.setValue(new Date());
		duration.setValue(MS_PER_HOUR * 24);
		times.setValue(1000);
		period.setValue(MS_PER_HOUR * 48);
	

		for (JFormattedTextField tf: fields) {
	    	tf.setBorder(BorderFactory.createCompoundBorder(tf.getBorder(), BorderFactory.createEmptyBorder(1, 3, 1, 3)));
			tf.getDocument().addDocumentListener(dl);
		}
		
	}

    ///
    /// SET UP TOGGLE BUTTONS
    ///
    
	JRadioButton durationFixed = new JRadioButton("for");
	JRadioButton durationForever = new JRadioButton("forever.");
	
	ButtonGroup durationGroup = new ButtonGroup();
	{
		durationGroup.add(durationFixed);
		durationGroup.add(durationForever);
	}
	
    JCheckBox repeatEnabled = new JCheckBox("The window repeats");

    JRadioButton repeatTypeForever = new JRadioButton();
    JRadioButton repeatTypeFixed = new JRadioButton();
	  
    
    ButtonGroup repeatTypeGroup = new ButtonGroup();
    {
    	repeatTypeGroup.add(repeatTypeForever);
    	repeatTypeGroup.add(repeatTypeFixed);
    }
    
	
	{

    	repeatTypeForever.setSelected(true);
    	durationFixed.setSelected(true);
		JToggleButton[] toggles = { repeatEnabled, repeatTypeForever, repeatTypeFixed, durationFixed, durationForever };

		ActionListener al = new ActionListener() {				
			public void actionPerformed(ActionEvent e) {
				updateEnabledState();
			}				
		};
		
		for (JToggleButton tb: toggles)
			tb.addActionListener(al);

	}

	
    ///
	/// SET UP LABELS
	///
    
    JLabel[] labels = new JLabel[4];

    
    ///
    /// SET UP BUTTONS
    ///
    
    boolean cancelled = true;
    
    JButton ok = new JButton("Ok") {{
		addActionListener(new ActionListener() {				
			public void actionPerformed(ActionEvent e) {
				cancelled = false;
				TimingWindowDialog.this.setVisible(false);
			}				
		});
	}};

	JButton cancel = new JButton("Cancel") {{
		addActionListener(new ActionListener() {				
			public void actionPerformed(ActionEvent e) {
				cancelled = true;
				TimingWindowDialog.this.setVisible(false);
			}				
		});
	}};
	
	
	///
	/// FINALLY, AN INSTANCE METHOD!
	///

	private void updateEnabledState() {

		// Repeat is enabled only if durationFixed is selected
		repeatEnabled.setEnabled(durationFixed.isSelected());
		
		// Most repeat options are enabled only if repeatEnabled is selected.
		boolean repeat = repeatEnabled.isEnabled() && repeatEnabled.isSelected();
		for (JLabel label: labels) label.setEnabled(repeat);
		repeatTypeForever.setEnabled(repeat);
		repeatTypeFixed.setEnabled(repeat);
		period.setEnabled(repeat);
		
		// With the exception of repeat count, which is enabled only if repeatEnabled
		// AND repeatTypeFixed are both selected.
		times.setEnabled(repeat && repeatTypeFixed.isSelected());
		
		// The Ok button is enabled if all the enabled text fields are correct.
		// This is a clever way of doing it, don't you think?
		boolean valid = true;
		for (JFormattedTextField tf: fields) {

			// Can't just call  tf.isEditValid() here because of the way
			// the events propogate; if we do it that way, we're 1 event behind.
			if (tf.isEnabled()) {
				try {
					tf.getFormatter().stringToValue(tf.getText());
				} catch (ParseException pe) {
					valid = false;
					break;
				}
			}

		}
		ok.setEnabled(valid);
		
	}
	
	public TimingWindowDialog(Frame owner) throws HeadlessException {
		super(owner, true);
		setTitle("Edit Timing Window");
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);




		
		setContentPane(new JPanel(new GridBagLayout()) {{	
			
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 1, 1, 1, Color.GRAY),
					BorderFactory.createEmptyBorder(20, 20, 10, 10)));
			
			add(new JTextArea(INSTRUCTIONS) {{
				setWrapStyleWord(true);
				setLineWrap(true);
				setBorder(
						BorderFactory.createCompoundBorder(
							BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
							BorderFactory.createEmptyBorder(0, 0, 10, 0)));
//				setBackground(new Color())
				setOpaque(false);
				Font f = getFont();
				setFont(f.deriveFont(f.getSize2D() - 2.0f));
				setEnabled(false);
				setDisabledTextColor(Color.BLACK);
				getPreferredSize();
				setPreferredSize(new Dimension(0, 50));
			}}, gbc(0, -1, 6, new Insets(0, 0, 10, 0)));

			add(new JLabel("Observing window begins at "), gbc(0, 0, 3));
			add(window, gbc(3, 0, 3, new Insets(0, 0, 5, 10)));
			add(new JLabel("and remains open ", SwingConstants.RIGHT), gbc(0, 1, 3));
			
			add(durationFixed, gbc(3, 1, 1));
			
			add(duration, gbc(4, 1, 1));
			add(new JLabel(" (hh:mm)."), gbc(5, 1, 1));
			
			add(durationForever, gbc(3, 2, 3));
			
			add(repeatEnabled, gbc(0, 3, 4, new Insets(10, 5, 0, 0)));
			add(repeatTypeForever, gbc(0, 4, 1, new Insets(0, 20, 0, 0)));
			add(labels[0] = new JLabel("forever"), gbc(1, 4, 2));
			add(repeatTypeFixed, gbc(0, 5, 1, new Insets(0, 20, 0, 0)));
			add(times, gbc(1, 5, 1));
			add(labels[1] = new JLabel(" times"), gbc(2, 5, 2));
			add(labels[2] = new JLabel("with a period of ", SwingConstants.RIGHT), gbc(1, 6, 2));
			add(period, gbc(3, 6, 1));
			add(labels[3] = new JLabel(" (hhh:mm:ss)."), gbc(4, 6, 1));
			add(new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)) {{
				setBorder(
					BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
						BorderFactory.createEmptyBorder(10, 0, 0, 0)));
				add(ok);
				add(cancel);
			}}, gbc(0, 7, 6, new Insets(20, 0, 0, 0)));
		}});


		updateEnabledState();
		
		
		pack();
//		validate();
		
	}

	private static GridBagConstraints gbc(int x, int y, int xs) {
		return gbc(x, y, xs, new Insets(0, 0, 0, 0));
	}
	
	private static GridBagConstraints gbc(int x, int y, int xs, Insets insets) {
		return new GridBagConstraints(x, y + 1, xs, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0);
	}

	/**
	 * Edit the passed TimingWindow object, returning a new one with the
	 * edit results, or null if the edit was cancelled. This method blocks
	 * until the user closes the dialog.
	 * @param initialValue
	 * @return a new TimingWindow, or null
	 */
	public TimingWindow showEdit(TimingWindow tw) {
		setValue(tw);
		updateEnabledState();
		cancelled = true;
		setLocationRelativeTo(getOwner());
		setVisible(true);
		return cancelled ? null : getValue();
	}
	
	private void setValue(TimingWindow initialValue) {
		
		window.setValue(new Date(initialValue.getStart()));
		
		if (initialValue.getDuration() == TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) {
			durationForever.setSelected(true);
		} else {
			duration.setValue(initialValue.getDuration());
		}
		
		switch (initialValue.getRepeat()) {

		case TimingWindow.REPEAT_FOREVER:
			repeatEnabled.setSelected(true);
			repeatTypeForever.setSelected(true);
			period.setValue(initialValue.getPeriod());
			break;
			
		case TimingWindow.REPEAT_NEVER:
			repeatEnabled.setSelected(false);
			break;

		default:
			repeatEnabled.setSelected(true);
			repeatTypeFixed.setSelected(true);
			times.setValue(initialValue.getRepeat());
			period.setValue(initialValue.getPeriod());
			break;
			
		}		

	}
	
	private TimingWindow getValue() {
		long start = ((Date) window.getValue()).getTime();
		
		if (durationForever.isSelected()) {
			return new TimingWindow(start, TimingWindow.WINDOW_REMAINS_OPEN_FOREVER, TimingWindow.REPEAT_NEVER, 0);
		}

		long duration = (Long) this.duration.getValue();

		if (repeatEnabled.isSelected()) {
			long period = (Long) this.period.getValue();
			if (repeatTypeFixed.isSelected()) {
				int repeat = ((Number) times.getValue()).intValue();
				return new TimingWindow(start, duration, repeat, period);
			} else {
				return new TimingWindow(start, duration, TimingWindow.REPEAT_FOREVER, period);
			}
		} else {
			return new TimingWindow(start, duration, TimingWindow.REPEAT_NEVER, 0);
		}
	}
	
	public static void main(String[] args) throws UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
		new TimingWindowDialog(null).showEdit(new TimingWindow());
		System.exit(0);
	}
	
}


@SuppressWarnings("serial") 
class HourMinuteFormat extends Format {

	private static final long MS_PER_MINUTE = 1000 * 60;
	private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;
	private static final Pattern HH_MM_PAT = Pattern.compile("^(\\d+):(\\d{2})$");
	private static final Pattern HH_MM_SS_PAT = Pattern.compile("^(\\d+):(\\d{2}):(\\d{2})$");

	public static final HourMinuteFormat HH_MM    = new HourMinuteFormat(false);
	public static final HourMinuteFormat HH_MM_SS = new HourMinuteFormat(true);

	private final boolean showSeconds;

	private HourMinuteFormat(final boolean showSeconds) {
		this.showSeconds = showSeconds;
	}
	
	@Override
	public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {

		final long ms = (Long) obj;
		final long hh = ms / MS_PER_HOUR;
		final long mm = (ms % MS_PER_HOUR) / MS_PER_MINUTE;

		final String s;
		if (showSeconds) {
			final long ss = (ms % MS_PER_MINUTE) / 1000;
			s = String.format("%d:%02d:%02d", hh, mm, ss);
		} else {
			s = String.format("%d:%02d", hh, mm);
		}

		return toAppendTo.append(s);
		
	}

	@Override
	public Object parseObject(final String source, final ParsePosition pos) {

		final Pattern p = showSeconds ? HH_MM_SS_PAT : HH_MM_PAT;
		final Matcher m = p.matcher(source);
		if (!m.matches()) {			
			pos.setIndex(0);
			pos.setErrorIndex(0);
			return null;
		}
		
		final long hh = Long.parseLong(m.group(1));
		final long mm = Long.parseLong(m.group(2));
		final long ss = showSeconds ? Long.parseLong(m.group(3)) : 0;
		pos.setIndex(m.end());
		return hh * MS_PER_HOUR + mm * MS_PER_MINUTE + ss * 1000;
		
	}
	
}






