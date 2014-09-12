package edu.gemini.ui.gface;

import javax.swing.JLabel;


public interface GSubElementDecorator<M, E, S> extends GDecorator<M, E> {

	void decorate(JLabel label, E element, S subElement, Object value);
	
}
