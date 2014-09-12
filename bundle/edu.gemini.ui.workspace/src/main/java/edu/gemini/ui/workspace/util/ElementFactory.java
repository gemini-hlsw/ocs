package edu.gemini.ui.workspace.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;

import org.jdesktop.swingx.JXTreeTable;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.looks.Options;

public class ElementFactory {

	public static JTree createTree() {
		JTree tree = new JTree();
		tree.setBorder(Borders.DLU2_BORDER);
		tree.putClientProperty(Options.TREE_LINE_STYLE_KEY, Options.TREE_LINE_STYLE_NONE_VALUE);
//		Font f = tree.getFont();
//		tree.setFont(f.deriveFont(f.getSize() - 2.0f));
		return tree;
	}
	
	public static JList createList() {
		JList list = new JList();
		list.setBorder(Borders.DLU2_BORDER);
//		Font f = list.getFont();
//		list.setFont(f.deriveFont(f.getSize() - 2.0f));
		return list;
	}
	
	public static JTable createTable() {
		final JTable table = new JTable();
//		table.setBorder(Borders.DLU2_BORDER);
//		Font f = table.getFont();
//		table.setFont(f.deriveFont(f.getSize() - 2.0f));
//		table.setShowHorizontalLines(false);
//		table.setShowVerticalLines(false);
		table.setEnabled(false);
//		table.getTableHeader().setFont(f.deriveFont(f.getSize() - 2.0f));

		table.addFocusListener(new FocusListener() {
		
			private final Color foc = table.getSelectionBackground();
			private final Color unf = new Color(0xEE, 0xEE, 0xEE);
			
			public void focusLost(FocusEvent fe) {
				table.setSelectionBackground(unf);
			}
		
			public void focusGained(FocusEvent fe) {
				table.setSelectionBackground(foc);
			}
		
		});
		
		return table;
	}

	public static JXTreeTable createTreeTable() {
		
		final JXTreeTable table = new JXTreeTable();
//		table.setBorder(Borders.DLU2_BORDER);
		final Font f = table.getFont().deriveFont(table.getFont().getSize() - 2.0f);
		table.setFont(f);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(true);
		table.setEnabled(false);
		table.getTableHeader().setFont(f);
				
		table.addFocusListener(new FocusListener() {
		
			private final Color foc = table.getSelectionBackground();
			private final Color unf = new Color(0xEE, 0xEE, 0xEE);
			
			public void focusLost(FocusEvent fe) {
				table.setSelectionBackground(unf);
			}
		
			public void focusGained(FocusEvent fe) {
				table.setSelectionBackground(foc);
			}
		
		});
		
		return table;

	}

	public static JLabel createLabel(String string) {
		final JLabel label = new JLabel(string);
		final Font f = label.getFont().deriveFont(label.getFont().getSize() - 2.0f);
		label.setFont(f);
		return label;
	}
	
	
}
