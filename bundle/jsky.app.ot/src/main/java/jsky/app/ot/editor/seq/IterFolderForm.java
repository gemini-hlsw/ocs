package jsky.app.ot.editor.seq;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import jsky.app.ot.ui.util.FlatButtonUtil;
import jsky.util.gui.*;

import static jsky.app.ot.util.OtColor.VERY_LIGHT_GREY;

public class IterFolderForm extends JPanel {
	public IterFolderForm() {
		initComponents();
	}

	private void initComponents() {
        JLabel titleLabel = new JLabel();
		title = new TextBoxWidget();
		tabbedPane = new JTabbedPane();
		printButton = new JButton();
		exportButton = new JButton();
		table = new PrintableJTable() {{
            setAutoResizeMode(AUTO_RESIZE_OFF);
            setBackground(VERY_LIGHT_GREY);
            getTableHeader().setReorderingAllowed(false);
            setRowSelectionAllowed(true);
            setColumnSelectionAllowed(false);
            setFocusable(false);
            setShowHorizontalLines(false);
            setShowVerticalLines(false);
            setIntercellSpacing(new Dimension(0, 0));
        }};
		timeLineScrollPane = new JScrollPane();
		timeline = new ObserveTimeLine();

        totalTimeLabel = new JLabel("0.0") {{ setForeground(Color.BLACK); }};

        zoomInButton  = FlatButtonUtil.create("ZoomIn24.gif");
        zoomInButton.setToolTipText("Zoom In");

		zoomOutButton = FlatButtonUtil.create("ZoomOut24.gif");
        zoomOutButton.setToolTipText("Zoom Out");

		CellConstraints cc = new CellConstraints();

		//======== this ========
		setLayout(new FormLayout(
			new ColumnSpec[] {
				new ColumnSpec(ColumnSpec.RIGHT, Sizes.DEFAULT, FormSpec.NO_GROW),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
			},
			new RowSpec[] {
				new RowSpec(Sizes.DLUY5),
				FormFactory.LINE_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.LINE_GAP_ROWSPEC,
				new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
			}));

		//---- titleLabel ----
		titleLabel.setText("Title");
		titleLabel.setLabelFor(title);
		add(titleLabel, cc.xy(1, 3));
		add(title, cc.xy(3, 3));

		//======== tabbedPane ========
		{

            JPanel pan = new JPanel();
			//======== panel1 ========
			{
				pan.setBorder(new EmptyBorder(10, 10, 5, 10));
                pan.setLayout(new GridBagLayout());

                pan.add(timeLinePanel(), new GridBagConstraints() {{
                    gridy   = 0;
                    weightx = 1.0;
                    fill    = BOTH;
                }});


                //======== scrollPane1 ========
                JScrollPane scrollPane1 = new JScrollPane();
				{
					//---- table ----
					table.setBackground(Color.lightGray);
					scrollPane1.setViewportView(table);
                    scrollPane1.setViewportBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				}
				pan.add(scrollPane1, new GridBagConstraints() {{
                    gridy   = 1;
                    weightx = 1.0;
                    weighty = 1.0;
                    fill    = BOTH;
                }});

				//======== buttonPanel ========
                JPanel buttonPanel = new JPanel();
				{
					buttonPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
					buttonPanel.setLayout(new FormLayout(
                            new ColumnSpec[]{
                                    FormFactory.DEFAULT_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.DEFAULT_COLSPEC
                            },
                            RowSpec.decodeSpecs("default")));

					//---- printButton ----
					printButton.setText("Print ...");
					buttonPanel.add(printButton, cc.xy(1, 1));

					//---- exportButton ----
					exportButton.setText("Export to XML ...");
					buttonPanel.add(exportButton, cc.xy(3, 1));
				}
				pan.add(buttonPanel, new GridBagConstraints() {{
                    gridy   = 2;
                    anchor  = EAST;
                }});
			}
			tabbedPane.addTab("Timeline", pan);
//			tabbedPane.addTab("Timeline", timeLinePanel());

		}
		add(tabbedPane, cc.xywh(1, 5, 3, 1));
	}

    private JPanel timeLinePanel() {
        JPanel pan = new JPanel(new GridBagLayout());

        timeLineScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        timeLineScrollPane.setViewportView(timeline);

        pan.add(timeLineScrollPane, new GridBagConstraints() {{
            gridy     = 0;
            gridwidth = 4;
            weightx   = 1.0;
            fill      = BOTH;
        }});

        // don't allow it to squish this row too much
        JPanel strut = new JPanel() {{
            setMinimumSize(new Dimension(0, 70));
            setPreferredSize(new Dimension(0, 70));
        }};
        pan.add(strut, new GridBagConstraints() {{
            gridy      = 0;
            gridx      = 4;
        }});

        pan.add(zoomInButton, new GridBagConstraints() {{
            gridx   = 0;
            gridy   = 1;
        }});

        pan.add(zoomOutButton, new GridBagConstraints() {{
            gridx   = 1;
            gridy   = 1;
        }});

        pan.add(new JLabel("Total", JLabel.RIGHT), new GridBagConstraints() {{
            gridx   = 2;
            gridy   = 1;
            weightx = 1.0;
            fill    = HORIZONTAL;
            anchor  = EAST;
        }});

        pan.add(totalTimeLabel, new GridBagConstraints() {{
            gridx   = 3;
            gridy   = 1;
            insets  = new Insets(0, 10, 0, 0);
        }});

//        pan.add(new JLabel("min"), new GridBagConstraints() {{
//            gridx   = 4;
//            gridy   = 1;
//            insets  = new Insets(0, 5, 0, 0);
//        }});
        return pan;
    }

    TextBoxWidget title;
	JTabbedPane tabbedPane;
    JButton printButton;
	JButton exportButton;
    PrintableJTable table;
    JScrollPane timeLineScrollPane;
	ObserveTimeLine timeline;
	JButton zoomInButton;
	JButton zoomOutButton;
    JLabel totalTimeLabel;

//    ObserveTimeLineDetail detail;
}
