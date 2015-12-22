package edu.gemini.ui.workspace.util;

import javax.swing.JList;
import javax.swing.JTree;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.looks.Options;

public class ElementFactory {

    public static JTree createTree() {
        JTree tree = new JTree();
        tree.setBorder(Borders.DLU2_BORDER);
        tree.putClientProperty(Options.TREE_LINE_STYLE_KEY, Options.TREE_LINE_STYLE_NONE_VALUE);
        return tree;
    }

    public static <A> JList<A> createList() {
        JList<A> list = new JList<>();
        list.setBorder(Borders.DLU2_BORDER);
        return list;
    }
}
