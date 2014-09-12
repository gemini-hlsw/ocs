package edu.gemini.util;

import java.io.Serializable;



public interface RemoteIterator<T extends Serializable> {

	T next() ;
	
	boolean hasNext() ;
	
	void remove() ;
	
}

