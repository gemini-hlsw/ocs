package jsky.app.ot.gemini.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import jsky.html.*;
/*
 * Created by JFormDesigner on Wed Nov 02 16:13:46 CET 2005
 */



/**
 * @author User #1
 */
public class Phase1Form extends JPanel {
	public Phase1Form() {
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		tabbedPane = new JTabbedPane();
		sciencePanel = new JPanel();
		jScrollPane4 = new JScrollPane();
		sciencePane = new JTextArea();
		techPanel = new JPanel();
		jScrollPane3 = new JScrollPane();
		techPane = new JTextArea();
		commentsPanel = new JPanel();
		jScrollPane5 = new JScrollPane();
		commentsPane = new JTextArea();
		targetsPanel = new JPanel();
		jScrollPane1 = new JScrollPane();
		targetsTable = new JTable();
		summaryPanel = new JPanel();
		summaryPane = new HTMLViewer();
		navigatorPanel = new JPanel();
		CellConstraints cc = new CellConstraints();

		//======== this ========
		setLayout(new BorderLayout());

		//======== tabbedPane ========
		{
			
			//======== sciencePanel ========
			{
				sciencePanel.setLayout(new GridBagLayout());
				
				//======== jScrollPane4 ========
				{
					jScrollPane4.setBorder(LineBorder.createBlackLineBorder());
					
					//---- sciencePane ----
					sciencePane.setBackground(Color.lightGray);
					sciencePane.setEditable(false);
					sciencePane.setMargin(new Insets(10, 10, 10, 10));
					sciencePane.setLineWrap(true);
					sciencePane.setWrapStyleWord(true);
					jScrollPane4.setViewportView(sciencePane);
				}
				sciencePanel.add(jScrollPane4, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(5, 5, 5, 5), 0, 0));
			}
			tabbedPane.addTab("Science", sciencePanel);
			
			
			//======== techPanel ========
			{
				techPanel.setLayout(new GridBagLayout());
				
				//======== jScrollPane3 ========
				{
					jScrollPane3.setBorder(LineBorder.createBlackLineBorder());
					
					//---- techPane ----
					techPane.setBackground(Color.lightGray);
					techPane.setEditable(false);
					techPane.setMargin(new Insets(10, 10, 10, 10));
					techPane.setLineWrap(true);
					techPane.setWrapStyleWord(true);
					jScrollPane3.setViewportView(techPane);
				}
				techPanel.add(jScrollPane3, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(5, 5, 5, 5), 0, 0));
			}
			tabbedPane.addTab("Technical", techPanel);
			
			
			//======== commentsPanel ========
			{
				commentsPanel.setLayout(new GridBagLayout());
				
				//======== jScrollPane5 ========
				{
					jScrollPane5.setBorder(LineBorder.createBlackLineBorder());
					
					//---- commentsPane ----
					commentsPane.setBackground(Color.lightGray);
					commentsPane.setLineWrap(true);
					commentsPane.setWrapStyleWord(true);
					jScrollPane5.setViewportView(commentsPane);
				}
				commentsPanel.add(jScrollPane5, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(5, 5, 5, 5), 0, 0));
			}
			tabbedPane.addTab("TAC Comments", commentsPanel);
			
			
			//======== targetsPanel ========
			{
				targetsPanel.setLayout(new GridBagLayout());
				
				//======== jScrollPane1 ========
				{
					jScrollPane1.setBorder(LineBorder.createBlackLineBorder());
					
					//---- targetsTable ----
					targetsTable.setBackground(Color.lightGray);
					targetsTable.setEnabled(false);
					jScrollPane1.setViewportView(targetsTable);
				}
				targetsPanel.add(jScrollPane1, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(5, 5, 5, 5), 0, 0));
			}
			tabbedPane.addTab("Targets", targetsPanel);
			
			
			//======== summaryPanel ========
			{
				summaryPanel.setLayout(new FormLayout(
					ColumnSpec.decodeSpecs("default:grow"),
					new RowSpec[] {
						FormFactory.LINE_GAP_ROWSPEC,
						FormFactory.LINE_GAP_ROWSPEC,
						new RowSpec(RowSpec.CENTER, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
						FormFactory.LINE_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC
					}));
				summaryPanel.add(summaryPane, cc.xywh(1, 3, 1, 1, CellConstraints.FILL, CellConstraints.FILL));
				
				//======== navigatorPanel ========
				{
					navigatorPanel.setLayout(new BorderLayout());
				}
				summaryPanel.add(navigatorPanel, cc.xy(1, 5));
			}
			tabbedPane.addTab("Summary", summaryPanel);
			
		}
		add(tabbedPane, BorderLayout.CENTER);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	JTabbedPane tabbedPane;
	JPanel sciencePanel;
	private JScrollPane jScrollPane4;
	JTextArea sciencePane;
	JPanel techPanel;
	private JScrollPane jScrollPane3;
	JTextArea techPane;
	JPanel commentsPanel;
	private JScrollPane jScrollPane5;
	JTextArea commentsPane;
	JPanel targetsPanel;
	private JScrollPane jScrollPane1;
	JTable targetsTable;
	JPanel summaryPanel;
	HTMLViewer summaryPane;
	JPanel navigatorPanel;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
