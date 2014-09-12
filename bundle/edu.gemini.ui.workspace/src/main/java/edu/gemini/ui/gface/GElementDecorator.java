package edu.gemini.ui.gface;

import javax.swing.JLabel;


public interface GElementDecorator<M, E> extends GDecorator<M, E> {

	void decorate(JLabel label, E element);
	
}
