package edu.gemini.util;

import java.io.Serializable;



public interface RemoteIterable<T extends Serializable> {

	RemoteIterator<T> iterator() ;
	
}
